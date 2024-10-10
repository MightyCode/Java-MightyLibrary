package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.shader.ShaderLoader;
import MightyLibrary.mightylib.graphics.text.FontLoader;
import MightyLibrary.mightylib.resources.animation.AnimationDataLoader;
import MightyLibrary.mightylib.resources.data.JSONLoader;
import MightyLibrary.mightylib.resources.texture.IconLoader;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.resources.texture.TextureData;
import MightyLibrary.mightylib.resources.texture.TextureDataLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resources {
    public static class ResourceEntry {
        public ResourceLoader Loader;
        public DataType Data;
        public ActionEnum Action;
        public String info;

        public enum ActionEnum {
            PRELOAD,
            LOAD,
            UNLOAD,
            RELOAD
        }

        public ResourceEntry (ResourceLoader loader, DataType data, ActionEnum action) {
            Loader = loader;
            Data = data;
            Action = action;
        }

        public ResourceEntry setInfo(String info){
            this.info = info;
            return this;
        }
    }

    public static String FOLDER = "resources/";

    public static abstract class LoadingMethod{}
    public static class AllResourcesMethod extends LoadingMethod{}
    public static class BatchResourcesMethod extends LoadingMethod{
        private final String firstBatch;
        public BatchResourcesMethod(String firstBatch){
            this.firstBatch = firstBatch;
        }
    }

    private static Resources singletonInstance = null;

    public static Resources createInstance(LoadingMethod loadingMethod, int maxNumberOfWorkers){
        if (singletonInstance == null) {
            singletonInstance = new Resources(loadingMethod, maxNumberOfWorkers);
        }

        return singletonInstance;
    }

    public static Resources getInstance(){
        if (singletonInstance == null){
            singletonInstance = new Resources(new AllResourcesMethod(), 1);
        }

        return singletonInstance;
    }

    public final List<ResourceLoader> Loaders;

    private final HashMap<Class<? extends DataType>, HashMap<String, DataType>> resources;
    private final LoadingMethod loadingMethod;

    private final ArrayList<ResourceWorker> workers;
    private final int maxNumberOfWorkers;

    private final ArrayList<ResourceEntry> resourcesToProcess;
    private final Lock queueLock;

    private final Queue<ResourceEntry> resourcesToProcessMainContext;

    private boolean initialized;
    private boolean firstLoadDone;

    private Resources(LoadingMethod loadingMethod, int maxNumberOfWorkers) {
        this.loadingMethod = loadingMethod;
        resources = new HashMap<>();
        Loaders = new ArrayList<>();

        workers = new ArrayList<>();
        this.maxNumberOfWorkers = maxNumberOfWorkers;

        resourcesToProcess = new ArrayList<>();
        queueLock = new ReentrantLock();

        resourcesToProcessMainContext = new ArrayDeque<>();

        // Mandatory loaders
        Loaders.add(new ShaderLoader());
        Loaders.add(new IconLoader());
        Loaders.add(new TextureDataLoader());
        Loaders.add(new AnimationDataLoader());
        Loaders.add(new FontLoader());
        JSONLoader jsonLoader = new JSONLoader();
        Loaders.add(jsonLoader);

        Loaders.add(new BatchLoader(jsonLoader));

        initialized = false;
        firstLoadDone = false;
    }

    public void init() {
        if (initialized)
            return;

        // Init all loaders
        for (ResourceLoader loader : Loaders){
            HashMap<String, DataType> map = new HashMap<>();
            loader.create(map);
            resources.put(loader.getType(), map);
        }

        initialized = true;
    }


    public void load() {
        if (firstLoadDone)
            return;

        // Load the texture error
        DataType dataType = resources.get(TextureData.class).get("error");
        dataType.addReference("mandatory");
        addResourceToProcess(new ResourceEntry(getLoader(TextureData.class), dataType, ResourceEntry.ActionEnum.PRELOAD));

        if (loadingMethod instanceof BatchResourcesMethod){
            System.out.println("--Load Resources");
            // Load the batch resources first
            for (ResourceLoader loader : Loaders){
                if (loader.getType() == BatchResources.class){
                    loadAllOfType(loader.getType());
                    break;
                }
            }

            loadBatch(((BatchResourcesMethod) loadingMethod).firstBatch);
        } else {
            System.out.println("--Load Resources");
            for (ResourceLoader loader : Loaders){
                loadAllOfType(loader.getType());
            }
        }

        process(-1);
        while(!finishedCurrentLoading()){}

        finishLoading();
    }


    private void finishLoading() {
        Texture.CreateErrorTexture();
        firstLoadDone = true;
    }

    public void process(int allowedMilliseconds) {
        if (!initialized)
            throw new RuntimeException("Resources not initialized");

        if (!remainingResourceToProcess()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < allowedMilliseconds || allowedMilliseconds == -1) {
            ResourceEntry entry = getNextResourceToProcessMainContext();
            if (entry == null)
                return;

            mainContextHandleResource(entry);

            System.out.println("Main thread doing " + entry.Data.getDataName());
        }
    }

    private void mainContextHandleResource(ResourceEntry entry){
        switch (entry.Action) {
            case PRELOAD:
                entry.Loader.preload(entry.Data);
                notifyPreLoadedResources(entry.Data);
                break;
            case LOAD:
                entry.Data.load();
                break;
            case UNLOAD:
                entry.Data.unload();
                break;
            case RELOAD:
                entry.Loader.reload(entry.Data);
                break;
        }
    }

    public boolean finishedCurrentLoading() {
        if (remainingResourceToProcess()) {
            return false;
        }

        // iterate in inverse order to avoid ConcurrentModificationException
        for (int i = workers.size() - 1; i >= 0; i--) {
            ResourceWorker worker = workers.get(i);
            if (worker.isAlive())
                return false;

            worker.interrupt();

            try {
                System.out.println("Join worker " + worker.getThreadNumber());
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            workers.remove(worker);
        }

        workers.clear();

        return true;
    }

    boolean remainingResourceToProcess() {
        return remainingResourceToProcess(true) && remainingResourceToProcess(false);
    }

    boolean remainingResourceToProcess(boolean isMainContext) {
        if (isMainContext)
            return !resourcesToProcessMainContext.isEmpty();

        queueLock.lock();
        try {
            return !resourcesToProcess.isEmpty();
        }
        finally {
            queueLock.unlock();
        }
    }

    ResourceEntry getNextResourceToProcessMainContext() {
        ResourceEntry first = resourcesToProcessMainContext.poll();
        ResourceEntry current = first;
        boolean start = true;

        while ((start || first != current) && current != null) {
            if (!current.Data.isPreLoaded())
                return current;

            if (current.Data.canBeLoad() && !current.Data.isLoaded()) {
                return current;
            }

            start = false;

            resourcesToProcessMainContext.add(current);
            current = resourcesToProcessMainContext.poll();
        }

        return null;
    }

    void printQueue(){
        System.out.println("----------------------------------");
        System.out.println("Queue size: " + resourcesToProcess.size());
        for (ResourceEntry entry : resourcesToProcess){
            System.out.println("(" + entry.Data.getClass().getSimpleName() + ") " + entry.Data.getDataName());
        }
        System.out.println("----------------------------------");
    }

    ResourceEntry getNextResourceToProcess() {
        queueLock.lock();

        try {
            int indexCurrent = 0;
            if (resourcesToProcess.isEmpty())
                return null;

            ResourceEntry current = resourcesToProcess.get(0);
            boolean start = true;

            while ((start || indexCurrent != 0) && current != null) {
                if (!current.Data.isPreLoaded()) {
                    resourcesToProcess.remove(indexCurrent);
                    return current;
                }

                if (current.Data.canBeLoad() && !current.Data.isLoaded()) {
                    //printQueue();
                    resourcesToProcess.remove(indexCurrent);
                    return current;
                }

                start = false;
                indexCurrent = (indexCurrent + 1) % resourcesToProcess.size();
                current = resourcesToProcess.get(indexCurrent);
            }
        }
        finally {
            queueLock.unlock();
        }

        return null;
    }

    private void addResourceToProcess(ResourceEntry resourceEntry) {
        // Todo : not a good place to do that
        if (resourceEntry.Data.getTypeSetUp() == DataType.TYPE_SET_UP.IMMEDIATELY_BY_MAIN_CONTEXT) {
            System.out.println("Immediate processing in main context of (" + resourceEntry.Data.getClass().getSimpleName()  + ") :" + resourceEntry.Data.getDataName()
                + ", action : " + resourceEntry.Action);
            mainContextHandleResource(resourceEntry);

            return;
        }

        if (resourceEntry.Data.getTypeSetUp() == DataType.TYPE_SET_UP.MAIN_CONTEXT) {
            resourcesToProcessMainContext.add(resourceEntry);
            System.out.println("Added to main context (" + resourceEntry.Data.getClass().getSimpleName() + ") : " + resourceEntry.Data.getDataName() +
                    ", action : " + resourceEntry.Action);
        } else {
            queueLock.lock();
            try {
                resourcesToProcess.add(resourceEntry);
                System.out.println("Added to worker (" + resourceEntry.Data.getClass().getSimpleName() + ") : " + resourceEntry.Data.getDataName() +
                        ", action : " + resourceEntry.Action);
                if (workers.size() < maxNumberOfWorkers && resourcesToProcess.size() > workers.size()) {
                    ResourceWorker worker = new ResourceWorker(this, workers.size() + 1);
                    workers.add(worker);
                    worker.start();
                }
            }
            finally {
                queueLock.unlock();
            }
        }
    }

    public static Class<? extends DataType> getClassFromName(String name) {
        for (ResourceLoader resourceLoader : singletonInstance.Loaders) {
            if (resourceLoader.getResourceNameType().equals(name))
                return resourceLoader.getType();
        }

        return DataType.class;
    }

    public <T extends DataType> T getResource(Class<T> type, String name) {
        if (!resources.containsKey(type)) {
            throw new RuntimeException("Can't find resource of type: " + type.getName());
        }

        return type.cast(resources.get(type).get(name));
    }

    private ResourceLoader getLoader (Class<? extends DataType> typeOfResource){
        for (ResourceLoader loader : Loaders){
            if (typeOfResource.equals(loader.getType()))
                return loader;
        }

        return null;
    }

    public boolean isExistingResource(Class<? extends DataType> type, String name) {
        if (!resources.containsKey(type))
            return false;

        return resources.get(type).containsKey(name);
    }

    public void loadBatch(String batchName) {
        if (!(loadingMethod instanceof BatchResourcesMethod)) {
            System.err.println("Can't load batch: " + batchName + " when loading method is not BatchResources");
            return;
        }

        System.out.println("--Load batch: " + batchName);

        BatchResources batchResources = getResource(BatchResources.class, batchName);
        JSONArray content = batchResources.getObject().getJSONArray("batchs");
        for (int j = 0; j < content.length(); ++j) {
            JSONObject resource = content.getJSONObject(j);

            String resourceTypeName = resource.getString("type");

            Class<? extends DataType> type = getClassFromName(resourceTypeName);
            ResourceLoader loader = getLoader(type);

            JSONArray resourceFiles = resource.getJSONArray("files");
            JSONArray regularExpressions = resource.getJSONArray("regex");

            for (int i = 0; i < resourceFiles.length(); ++i) {
                String resourceName = resourceFiles.getString(i);

                DataType dataType = this.resources.get(type).get(resourceName);

                if (dataType == null)
                    throw new RuntimeException("Can't find resource: " + resourceName + " of type: " + type.getName());

                if (dataType.notReferenced())
                    addResourceToProcess(new ResourceEntry(loader, dataType, ResourceEntry.ActionEnum.PRELOAD));

                dataType.addReference(batchName);
            }

            for (int i = 0; i < regularExpressions.length(); ++i) {
                String regularExpression = regularExpressions.getString(i);
                try {
                    Pattern pattern = Pattern.compile(regularExpression);

                    for (String resourceName : this.resources.get(type).keySet()) {
                        Matcher matcher = pattern.matcher(resourceName);
                        if (matcher.matches()) {
                            DataType dataType = this.resources.get(type).get(resourceName);

                            if (dataType.notReferenced())
                                addResourceToProcess(new ResourceEntry(loader, dataType, ResourceEntry.ActionEnum.PRELOAD));

                            dataType.addReference(batchName);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    public void createAndInit(Class<? extends DataType> resourceType, String resourceName, String resourcePath){
        for (ResourceLoader resourceLoader : singletonInstance.Loaders) {
            if (resourceType == resourceLoader.getType()) {
                resourceLoader.createAndSetUp(resourceType, resources.get(resourceType), resourceName, resourcePath);
                break;
            }
        }
    }

    private void loadAllOfType(Class<? extends DataType> typeOfResource){
        ResourceLoader loader = getLoader(typeOfResource);

        for (DataType dataType : resources.get(typeOfResource).values()){
            if (dataType.notReferenced())
                addResourceToProcess(new ResourceEntry(loader, dataType, ResourceEntry.ActionEnum.PRELOAD));
        }
    }

    public void reloadAllOfType() {
        for (Class<? extends DataType> c : resources.keySet()) {
            reloadAllOfType(c);
        }
    }

    public void reloadAllOfType(Class<? extends DataType> typeOfResource) {
        for (DataType dataType : resources.get(typeOfResource).values()) {
            addResourceToProcess(new ResourceEntry(getLoader(typeOfResource), dataType, ResourceEntry.ActionEnum.RELOAD));
        }
    }


    public void unload(){
        System.out.println("--Unload Resources");
        for (Class<? extends DataType> c : resources.keySet()){
            unloadAllOfType(c);
        }
    }

    public void unloadAllOfType(Class<? extends DataType> typeOfResource){
        for (DataType dataType : resources.get(typeOfResource).values())
            addResourceToProcess(new ResourceEntry(getLoader(typeOfResource), dataType, ResourceEntry.ActionEnum.UNLOAD));
    }

    public void unloadBatch(String batchName) {
        if (!(loadingMethod instanceof BatchResourcesMethod)) {
            System.err.println("Can't unload batch: " + batchName + " when loading method is not BatchResources");
        }

        System.out.println("--Unload batch: " + batchName);;
        BatchResources batchResources = getResource(BatchResources.class, batchName);
        JSONArray content = batchResources.getObject().getJSONArray("batchs");
        for (int j = 0; j < content.length(); ++j) {
            JSONObject resource = content.getJSONObject(j);

            String resourceTypeName = resource.getString("type");
            Class<? extends DataType> type = getClassFromName(resourceTypeName);

            JSONArray resourceFiles = resource.getJSONArray("files");
            JSONArray regularExpressions = resource.getJSONArray("regex");

            for (int i = 0; i < resourceFiles.length(); ++i) {
                String resourceName = resourceFiles.getString(i);

                DataType dataType = this.resources.get(type).get(resourceName);
                dataType.removeReference(batchName);

                if (dataType.notReferenced())
                    addResourceToProcess(new ResourceEntry(getLoader(type), dataType, ResourceEntry.ActionEnum.UNLOAD));
            }

            for (int i = 0; i < regularExpressions.length(); ++i) {
                String regularExpression = regularExpressions.getString(i);
                try {
                    Pattern pattern = Pattern.compile(regularExpression);

                    for (String resourceName : this.resources.get(type).keySet()) {
                        Matcher matcher = pattern.matcher(resourceName);
                        if (matcher.matches()) {
                            DataType dataType = this.resources.get(type).get(resourceName);
                            dataType.removeReference(batchName);

                            if (dataType.notReferenced())
                                addResourceToProcess(new ResourceEntry(getLoader(type), dataType, ResourceEntry.ActionEnum.UNLOAD));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Can't compile regular expression: " + regularExpression);
                }
            }
        }
    }

    public void addCategoryAndLoader(Class<? extends DataType> category, ResourceLoader loader) {
        if (resources.containsKey(category))
            return;

        resources.put(category, new HashMap<>());
        Loaders.add(loader);
    }

    public void addResourceAndSetUp(Class<? extends DataType> type, DataType dataType) {
        resources.get(type).put(dataType.getDataName(), dataType);
        dataType.addReference("manually");
        addResourceToProcess(new ResourceEntry(getLoader(type), dataType, ResourceEntry.ActionEnum.PRELOAD));
    }

    public void addPreLoadedResource(DataType dataType) {
        resources.get(dataType.getClass()).put(dataType.getDataName(), dataType);
        dataType.addReference("manually");
        addResourceToProcess(new ResourceEntry(null, dataType, ResourceEntry.ActionEnum.LOAD));
    }

    void notifyPreLoadedResources(DataType dataType) {
        addResourceToProcess(new ResourceEntry(null, dataType, ResourceEntry.ActionEnum.LOAD));
    }

    public void unloadResource(Class<? extends DataType> type, String name) {
        addResourceToProcess(new ResourceEntry(null, resources.get(type).get(name), ResourceEntry.ActionEnum.UNLOAD));
    }

    public void deleteResource(Class<? extends DataType> type, String name) {
        DataType dataType = resources.get(type).get(name);
        deleteResource(dataType);
    }

    public void deleteResource(DataType dataType) {
        if (dataType == null)
            return;

        if (!dataType.isManuallyCreated())
            throw new RuntimeException("Can't delete resource: " + dataType.getDataName() + " because it's not manually created");

        if (dataType.isLoaded())
            addResourceToProcess(new ResourceEntry(null, dataType, ResourceEntry.ActionEnum.UNLOAD).setInfo("Delete"));
        else {
            queueLock.lock();
            try {
                resources.get(dataType.getClass()).remove(dataType.getDataName());
            }
            finally {
                queueLock.unlock();
            }
        }
    }
}
