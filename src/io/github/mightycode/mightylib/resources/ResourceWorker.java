package io.github.mightycode.mightylib.resources;

import static io.github.mightycode.mightylib.resources.Resources.ResourceEntry.ActionEnum.*;

// Task run by the engine Workers: processes the resource queue until it is empty
public class ResourceWorker implements Runnable {
    private final Resources resource;

    private int numberErrorUnloading;
    private int numberErrorLoading;

    public ResourceWorker(Resources resource) {
        this.resource = resource;
    }

    public int getNumberErrorUnloading() {
        return numberErrorUnloading;
    }

    public int getNumberErrorLoading() {
        return numberErrorLoading;
    }

    /**
     * Query from resources a remaining resource to load
     */
    @Override
    public void run() {
        numberErrorLoading = 0;
        numberErrorUnloading = 0;

        try {
            processQueue();
        } catch (Throwable e) {
            System.err.println("--Resource worker failed:");
            e.printStackTrace();
        } finally {
            resource.workerEnded();
        }
    }

    private void processQueue() {
        while (true) {
            Resources.ResourceEntry entry = resource.getNextResourceToProcess();
            if (entry == null) {
                // Stops only when the queue is empty, checked with the queue lock so no entry added meanwhile is forgotten
                if (resource.canWorkerStop())
                    return;

                // Remaining entries wait for their dependencies, often loaded by the main thread
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }

            synchronized (entry) {
                if (entry.Action == PRELOAD) {
                    numberErrorLoading += preloadData(entry.Data, entry.Loader);
                } else if (entry.Action == LOAD) {
                    numberErrorLoading += loadData(entry.Data);
                } else if (entry.Action == UNLOAD) {
                    numberErrorUnloading += unloadData(entry);
                } else if (entry.Action == RELOAD) {
                    numberErrorLoading += reloadData(entry);
                }
            }
        }
    }

    int preloadData(DataType dataType, ResourceLoader loader) {
        if (dataType.isPreLoaded()) {
            //System.out.println("Thread " + threadNumber + " already preloaded " + dataType.getClass().getSimpleName() + " : " + dataType.getDataName());
            return 1;
        }


        //System.out.println("Thread " + threadNumber + " preloading " + dataType.getClass().getSimpleName() + " : " + dataType.getDataName());

        loader.preload(dataType);

        if (!dataType.isPreLoaded()) {
            //System.out.println("Thread " + threadNumber + " error preloading " + dataType.getClass().getSimpleName() + " : " + dataType.getDataName());
            return 1;
        }

        resource.notifyPreLoadedResources(dataType);

        return 0;
    }

    int loadData(DataType dataType) {
        if (dataType.isLoaded()) {
            //System.out.println("Thread " + threadNumber + " already loaded " + dataType.getClass().getSimpleName() + " : " + dataType.getDataName());
            return 1;
        }

       // System.out.println("Thread " + threadNumber + " loading " + dataType.getClass().getSimpleName() + " : " + dataType.getDataName());

        dataType.load();

        if (!dataType.isLoaded()) {
            //System.out.println("Thread " + threadNumber + " error loading " + dataType.getClass().getSimpleName() + " : " + dataType.getDataName());
            return 1;
        }

        return 0;
    }

    synchronized int unloadData(Resources.ResourceEntry entry) {
        DataType dataType = entry.Data;
        if (!dataType.isLoaded())
            return 1;

        //System.out.println("Thread " + threadNumber + " unloading " + dataType.getClass().getSimpleName() + " : " + dataType.getDataName());

        dataType.unload();

        if (dataType.isLoaded())
            return 1;

        if ("Delete".equals(entry.info))
            resource.deleteResource(dataType);

        return 0;
    }

    synchronized int reloadData(Resources.ResourceEntry entry) {
        if (!entry.Data.isLoaded())
            return 1;

       // System.out.println("Thread " + threadNumber + " reloading " + entry.Data.getClass().getSimpleName() + " : " + entry.Data.getDataName());
        entry.Loader.reload(entry.Data);

        if (!entry.Data.isPreLoaded())
            return 1;

        resource.notifyPreLoadedResources(entry.Data);

        return 0;
    }
}
