package MightyLibrary.mightylib.resources;

import static MightyLibrary.mightylib.resources.Resources.ResourceEntry.ActionEnum.*;

public class ResourceWorker extends Thread {
    private final int threadNumber;
    public int getThreadNumber() {
        return threadNumber;
    }

    private final Resources resource;

    private int numberErrorUnloading;
    private int numberErrorLoading;

    private boolean isWorking = true;
    public boolean isWorking() {
        return isWorking;
    }

    public ResourceWorker(Resources resource, int threadNumber) {
        this.threadNumber = threadNumber;
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

        Resources.ResourceEntry entry = resource.getNextResourceToProcess();

        while (resource.remainingResourceToProcess(false) || entry != null) {
            if (entry != null) {
                //System.out.println("Thread " + threadNumber + " PROCESS (" + entry.Data.getClass().getSimpleName() + ") : " + entry.Data.getDataName() + " " + entry.Data.isLoaded() + " " + entry.Action);

                synchronized (entry) {
                    if (entry.Action == PRELOAD) {
                        numberErrorLoading += preloadData(entry.Data, entry.Loader);
                    } else if (entry.Action == LOAD) {
                        numberErrorLoading += loadData(entry.Data);
                    } else if (entry.Action == UNLOAD) {
                        numberErrorUnloading += unloadData(entry.Data);
                    } else if (entry.Action == RELOAD) {
                        numberErrorLoading += reloadData(entry);
                    }

                    //System.out.println("Thread " + threadNumber + " PROCESSED (" + entry.Data.getClass().getSimpleName() + ") : " + entry.Data.getDataName() + " " + entry.Data.isLoaded());

                   /*if (numberErrorLoading > 0) {
                        System.out.println("Thread " + threadNumber + " error loading/unloading/reloading " + entry.Data.getClass().getSimpleName() + " : " + entry.Data.getDataName());
                    }*/
                }

                entry = resource.getNextResourceToProcess();
            }
        }

        //System.out.println("End of thread " + threadNumber);
        isWorking = false;
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

    synchronized int unloadData(DataType dataType) {
        if (!dataType.isLoaded())
            return 1;

        //System.out.println("Thread " + threadNumber + " unloading " + dataType.getClass().getSimpleName() + " : " + dataType.getDataName());

        dataType.unload();

        if (!dataType.isLoaded())
            return 1;

        return 0;
    }

    synchronized int reloadData(Resources.ResourceEntry entry) {
        if (!entry.Data.isLoaded())
            return 1;

       // System.out.println("Thread " + threadNumber + " reloading " + entry.Data.getClass().getSimpleName() + " : " + entry.Data.getDataName());
        entry.Loader.preload(entry.Data);

        if (!entry.Data.isLoaded())
            return 1;

        if (entry.info.equals("Delete"))
            resource.deleteResource(entry.Data.getClass(), entry.Data.getDataName());

        return 0;
    }
}
