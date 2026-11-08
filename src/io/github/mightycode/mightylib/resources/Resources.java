package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.shader.ShaderLoader;
import io.github.mightycode.mightylib.graphics.text.FontLoader;
import io.github.mightycode.mightylib.resources.animation.AnimationDataLoader;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import io.github.mightycode.mightylib.resources.texture.IconLoader;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TextureDataLoader;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and loading pipeline of the data read from the disk.
 *
 * Single owner: references and states of the data are changed by the main thread only. Calls from other threads
 * (owned resources registered by a worker) are posted to the main thread. Workers only run the preload (initWithFile)
 * and, for THREAD_CONTEXT data, the load (internLoad), on a data that the main thread does not touch meanwhile.
 *
 * Reconciliation: for each data, update compares what is wanted (referenced or not) with its state and starts the
 * next step. A data with a running step is never changed: when the step ends, update runs again, so a data unloaded
 * then loaded again while it was read ends in the wanted state without any special case.
 */
public class Resources {
    public static String FOLDER = "resources/";

    // Reference added by loadAllOfType (AllResourcesMethod, batch descriptions)
    private static final String ALL_REFERENCE = "all";
    // Prefix of the references a data adds to its dependencies
    static final String DEPENDENCY_REFERENCE_PREFIX = "dependency:";
    // Name of the fallback of each type (ResourceLoader.createFallback), and the reference keeping it loaded
    public static final String FALLBACK_NAME = "error";

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

    // Tests only: replace the singleton by a fresh instance using only the given loaders (no GL loader)
    static Resources createForTests(LoadingMethod loadingMethod, int maxNumberOfWorkers, List<ResourceLoader> loaders) {
        singletonInstance = new Resources(loadingMethod, maxNumberOfWorkers, loaders);
        return singletonInstance;
    }

    public final List<ResourceLoader> Loaders;

    // Read from any thread, written by the main thread (and by addOwnedResource, atomically)
    private final Map<Class<? extends DataType>, Map<String, DataType>> resources;
    private final LoadingMethod loadingMethod;
    // Hot reload on file change: created off, started explicitly (code, command watch, system property)
    private final ResourceWatcher watcher = new ResourceWatcher(this);
    private final ResourceDescriber describer = new ResourceDescriber(this);
    private final BatchManager batches = new BatchManager(this);
    private final FallbackRegistry fallbacks = new FallbackRegistry(this);
    private final ReloadManager reloads = new ReloadManager(this);
    private final DependencyGraph dependencies = new DependencyGraph(this);

    private final ResourceScheduler scheduler;
    private boolean shuttingDown;
    // A failure throws instead of using the fallback (EngineConfig.setStrictResources)
    private boolean strict;

    private boolean initialized;
    private boolean firstLoadDone;

    private Resources(LoadingMethod loadingMethod, int maxNumberOfWorkers) {
        this(loadingMethod, maxNumberOfWorkers, null);
    }

    // Without given loaders, the mandatory ones are used
    private Resources(LoadingMethod loadingMethod, int maxNumberOfWorkers, List<ResourceLoader> loaders) {
        this.loadingMethod = loadingMethod;
        resources = new ConcurrentHashMap<>();
        Loaders = new ArrayList<>();

        scheduler = new ResourceScheduler(this, maxNumberOfWorkers);

        if (loaders != null) {
            Loaders.addAll(loaders);
        } else {
            // Mandatory loaders
            Loaders.add(new ShaderLoader());
            Loaders.add(new IconLoader());
            Loaders.add(new TextureDataLoader());
            Loaders.add(new AnimationDataLoader());
            Loaders.add(new FontLoader());
            JSONLoader jsonLoader = new JSONLoader();
            Loaders.add(jsonLoader);

            Loaders.add(new BatchLoader(jsonLoader));
        }

        initialized = false;
        firstLoadDone = false;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    // Reloads reading their new version at the same time (both versions stay in memory); the others wait
    public void setMaxReloadCopies(int maxReloadCopies) {
        reloads.setMaxCopies(maxReloadCopies);
    }

    public void init() {
        if (initialized)
            return;

        // Init all loaders
        for (ResourceLoader loader : Loaders){
            Map<String, DataType> map = new ConcurrentHashMap<>();
            loader.create(map);
            resources.put(loader.getType(), map);
            fallbacks.register(loader, map);
        }

        initialized = true;
    }

    // ---- Fallbacks (see FallbackRegistry) ----

    void loadFallbacks() {
        fallbacks.loadAll();
    }

    public <T extends DataType> T getFallback(Class<T> type) {
        return fallbacks.get(type);
    }

    boolean hasFallback(DataType data) {
        return fallbacks.has(data);
    }

    public boolean isFallback(DataType data) {
        return fallbacks.is(data);
    }

    public <T extends DataType> T usable(Class<T> type, T data) {
        return fallbacks.usable(type, data);
    }

    // getResource, with the fallback of the type for an unknown name (reported once per name)
    public <T extends DataType> T getResourceOrFallback(Class<T> type, String name) {
        T data = getResource(type, name);
        if (data != null)
            return data;

        return fallbacks.forUnknown(type, name, strict);
    }


    public void load() {
        if (firstLoadDone)
            return;

        // Fallbacks first: they replace the data still loading
        loadFallbacks();

        System.out.println("--Load Resources");
        if (loadingMethod instanceof BatchResourcesMethod){
            // The batch descriptions first: they are loaded immediately
            loadAllOfType(BatchResources.class);
            loadBatch(((BatchResourcesMethod) loadingMethod).firstBatch);
        } else {
            for (ResourceLoader loader : Loaders){
                loadAllOfType(loader.getType());
            }
        }

        // Main context steps may depend on worker results, so keep processing until everything is done
        while (true) {
            process(-1);
            if (finishedCurrentLoading())
                break;

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        finishLoading();
    }


    private void finishLoading() {
        Texture.CreateErrorTexture();
        firstLoadDone = true;
    }

    /**
     * Main thread: apply the results of the workers, update the data and run the main context steps
     * until the time is spent (at least one step), or until nothing is left with -1.
     */
    public void process(int allowedMilliseconds) {
        if (!initialized)
            throw new RuntimeException("Resources not initialized");

        scheduler.process(allowedMilliseconds);
    }

    // Main thread: true when no data is being loaded or unloaded
    public boolean finishedCurrentLoading() {
        return scheduler.isIdle() && dependencies.waitingCount() == 0;
    }

    // Main thread: see ResourceDescriber
    public JSONObject describe() {
        return describer.describe();
    }

    public JSONObject describe(ResourceSelection selection) {
        return describer.describe(selection);
    }

    JSONObject describePipeline() {
        JSONObject pipeline = new JSONObject();
        scheduler.describe(pipeline);
        pipeline.put("waitingDependencies", dependencies.waitingCount());
        pipeline.put("finished", finishedCurrentLoading());
        return pipeline;
    }

    Map<Class<? extends DataType>, Map<String, DataType>> registry() {
        return resources;
    }

    // ---- Reconciliation, main thread only ----

    void runOnMainThread(Runnable task) {
        scheduler.runOnMainThread(task);
    }

    BatchManager batches() {
        return batches;
    }

    DependencyGraph dependencies() {
        return dependencies;
    }

    void enqueueUpdate(DataType data) {
        scheduler.enqueueUpdate(data);
    }

    void requestUpdate(DataType data) {
        // Immediate data are set up in the call that wants them (batch descriptions are read right after)
        if (data.getTypeSetUp() == DataType.TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT)
            update(data);
        else
            scheduler.enqueueUpdate(data);
    }

    boolean isWanted(DataType data) {
        // A reload copy is wanted while it is the current copy of a wanted data
        if (data.reloadOf != null)
            return data.reloadOf.reloadCopy == data && isWanted(data.reloadOf);

        return !shuttingDown && !data.deleteWanted && !data.notReferenced();
    }

    void update(DataType data) {
        if (data.jobRunning)
            return;

        if (!isWanted(data)) {
            dependencies.stopWaiting(data);
            dependencies.release(data);
            data.failed = false;
            data.reloadWanted = false;
            data.discard();

            // The new version being read is not needed either
            DataType copy = data.reloadCopy;
            if (copy != null) {
                data.reloadCopy = null;
                update(copy);
                reloads.copyEnded();
            }
            reloads.forget(data);

            if (data.deleteWanted) {
                Map<String, DataType> map = resources.get(data.getClass());
                if (map != null)
                    map.remove(data.getDataName(), data);
            }
            return;
        }

        if (data.reloadWanted) {
            // Too many copies alive: waits for a place (still loaded and usable)
            if (data.isLoaded() && data.reloadOf == null && reloads.mustWait(data))
                return;

            data.reloadWanted = false;

            // Loaded: the new version is read apart, the data stays usable meanwhile
            if (data.isLoaded() && data.reloadOf == null && reloads.startCopy(data))
                return;

            data.failed = false;
            dependencies.stopWaiting(data);
            dependencies.release(data);
            data.discard();
            reloads.wakeDeferred();
        }

        if (data.failed || data.isLoaded())
            return;

        if (!data.isPreLoaded()) {
            startStep(data, true);
            return;
        }

        dependencies.acquire(data);

        for (DataType dependency : data.getDependencies()) {
            // A failed dependency with a fallback is replaced by it (Resources.usable): the data can be loaded
            if (dependency.failed && !hasFallback(dependency)) {
                fail(data, "its dependency " + dependency.getClass().getSimpleName() + " "
                        + dependency.getDataName() + " failed");
                return;
            }
        }

        if (!dependencies.allReady(data)) {
            String cycle = DependencyGraph.findCycle(data);
            if (cycle != null) {
                fail(data, "dependency cycle (" + cycle + ")");
                return;
            }

            dependencies.waitFor(data);
        } else {
            dependencies.stopWaiting(data);
            startStep(data, false);
        }
    }

    /**
     * The data cannot be loaded: back to the initial state (its dependencies released) until it is no longer wanted
     * or reloaded; the data waiting for it fail too, instead of waiting forever.
     */
    private void fail(DataType data, String reason) {
        String message = data.getClass().getSimpleName() + " " + data.getDataName() + " failed: " + reason;
        // A fallback must never fail: nothing could replace it
        if (data.reloadOf != null && !isWanted(data)) {
            reloads.fail(data, message);
            return;
        }

        if (strict || isFallback(data))
            throw new IllegalStateException((strict ? "Strict resources: " : "Fallback failed: ") + message);

        if (data.reloadOf != null) {
            reloads.fail(data, message);
            return;
        }

        boolean replaced = hasFallback(data);
        System.err.println("--" + message + (replaced ? ", its fallback is used" : ""));

        data.failed = true;
        dependencies.stopWaiting(data);
        dependencies.release(data);
        data.discard();

        List<DataType> waiters = new ArrayList<>(data.waiters);
        data.waiters.clear();
        for (DataType waiter : waiters) {
            if (!waiter.waitingDependencies || waiter.failed)
                continue;

            // Replaced by the fallback: the waiters go on with it
            if (replaced)
                scheduler.enqueueUpdate(waiter);
            else
                fail(waiter, "its dependency " + data.getClass().getSimpleName() + " " + data.getDataName() + " failed");
        }
    }

    // Preload (read the file) or load (compute) the data, where its type wants it
    private void startStep(DataType data, boolean preload) {
        ResourceLoader loader = null;
        if (preload) {
            loader = findLoader(data);
            if (loader == null) {
                fail(data, "no loader");
                return;
            }
        }

        data.jobRunning = true;
        StepResult result = new StepResult();
        ResourceLoader stepLoader = loader;

        Runnable work = () -> {
            try {
                if (preload)
                    stepLoader.read(data);
                else
                    result.success = data.runLoad();
            } catch (Throwable e) {
                result.error = e;
            }
        };

        Runnable end = () -> endStep(data, preload, result);

        scheduler.submit(data, work, end);
    }

    private static class StepResult {
        boolean success = true;
        Throwable error;
    }

    private void endStep(DataType data, boolean preload, StepResult result) {
        data.jobRunning = false;

        if (result.error instanceof ResourceException) {
            // Expected (the data is wrong, not the code): one line, no stack trace
            fail(data, result.error.getMessage());
        } else if (result.error != null) {
            result.error.printStackTrace();
            fail(data, (preload ? "preload" : "load") + " threw " + result.error);
        } else if (preload) {
            data.setPreloaded();
        } else if (result.success) {
            if (data.reloadOf != null && isWanted(data)) {
                reloads.commit(data);
            } else {
                data.loadDone(true);
                dependencies.wakeWaiters(data);
                reloads.contentChanged(data);
            }
        } else {
            fail(data, "load returned false");
        }

        // Wanted state may have changed meanwhile: unloaded if no longer referenced, next step otherwise
        requestUpdate(data);
    }

    // Loader of the type of the data, or of one of its parent types
    private ResourceLoader findLoader(DataType data) {
        ResourceLoader loader = getLoader(data.getClass());
        if (loader != null)
            return loader;

        for (ResourceLoader candidate : Loaders) {
            if (candidate.getType().isInstance(data))
                return candidate;
        }

        return null;
    }

    void addReferenceAndUpdate(DataType data, String reference) {
        data.addReference(reference);
        requestUpdate(data);
    }

    void removeReferenceAndUpdate(DataType data, String reference) {
        data.removeReference(reference);
        requestUpdate(data);
    }

    // ---- Registry ----

    public static Class<? extends DataType> getClassFromName(String name) {
        for (ResourceLoader resourceLoader : singletonInstance.Loaders) {
            if (resourceLoader.getResourceNameType().equals(name))
                return resourceLoader.getType();
        }

        return null;
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

    // ---- Batches (see BatchManager) ----

    public void loadBatch(String batchName) {
        if (!(loadingMethod instanceof BatchResourcesMethod)) {
            System.err.println("Can't load batch: " + batchName + " when loading method is not BatchResources");
            return;
        }

        batches.load(batchName);
    }

    public void unloadBatch(String batchName) {
        if (!(loadingMethod instanceof BatchResourcesMethod)) {
            System.err.println("Can't unload batch: " + batchName + " when loading method is not BatchResources");
            return;
        }

        batches.unload(batchName);
    }

    // ---- Whole types ----

    void loadAllOfType(Class<? extends DataType> typeOfResource){
        runOnMainThread(() -> {
            for (DataType dataType : resources.get(typeOfResource).values()) {
                if (!dataType.references.contains(ALL_REFERENCE))
                    addReferenceAndUpdate(dataType, ALL_REFERENCE);
            }
        });
    }

    public void reloadAllOfType() {
        for (Class<? extends DataType> c : resources.keySet()) {
            reloadAllOfType(c);
        }
    }

    /**
     * Read again the loaded (or failed) data matching the selection, see reload(DataType).
     *
     * @return the number of data concerned (0 is not an error)
     */
    public int reload(ResourceSelection selection) {
        List<DataType> selected = new ArrayList<>();
        forEachRegistered((type, data) -> {
            if ((data.isLoaded() || data.hasFailed() || data.jobRunning) && isReloadable(data)
                    && selection.matches(type, data))
                selected.add(data);
        });

        runOnMainThread(() -> {
            for (DataType data : selected)
                reloads.request(data);
        });
        return selected.size();
    }

    // Read again from the disk the loaded data of this type
    public void reloadAllOfType(Class<? extends DataType> typeOfResource) {
        runOnMainThread(() -> {
            for (DataType dataType : resources.get(typeOfResource).values())
                reloads.request(dataType);
        });
    }

    /**
     * Read again one data from its source. A loaded data stays usable with its previous version until the new one
     * is loaded (or kept if it fails); then the data using it are read again. A failed data is read again in place.
     */
    public void reload(DataType dataType) {
        runOnMainThread(() -> reloads.request(dataType));
    }

    // Off until asked: watcher().watchAll(), watcher().watch(ResourceSelection...), command watch
    public ResourceWatcher watcher() {
        return watcher;
    }

    // Any thread: every registered data with its registered type (weakly consistent snapshot)
    void forEachRegistered(java.util.function.BiConsumer<Class<? extends DataType>, DataType> action) {
        for (Map.Entry<Class<? extends DataType>, Map<String, DataType>> ofType : resources.entrySet()) {
            for (DataType data : ofType.getValue().values())
                action.accept(ofType.getKey(), data);
        }
    }

    // Data that reload can read again from a file (not generated by the code, not a placeholder)
    boolean isReloadable(DataType data) {
        return !data.preloadedByCode && data.hasSource() && !isFallback(data);
    }

    // At the exit: unload everything not being read, whatever its references
    public void unload(){
        watcher.stop();
        System.out.println("--Unload Resources");
        runOnMainThread(() -> {
            shuttingDown = true;
            for (Map<String, DataType> ofType : resources.values()) {
                for (DataType dataType : ofType.values())
                    update(dataType);
            }
        });
    }

    // ---- Data added by the code ----

    public void addCategoryAndLoader(Class<? extends DataType> category, ResourceLoader loader) {
        if (resources.containsKey(category))
            return;

        resources.put(category, new ConcurrentHashMap<>());
        Loaders.add(loader);
    }

    // Data created by the code, read by its loader then loaded; referenced as "manually"
    public void addResourceAndSetUp(Class<? extends DataType> type, DataType dataType) {
        runOnMainThread(() -> {
            resources.get(type).put(dataType.getDataName(), dataType);
            addReferenceAndUpdate(dataType, "manually");
        });
    }

    // Data created and preloaded (setPreloaded) by the code, then loaded; referenced as "manually"
    public void addPreLoadedResource(DataType dataType) {
        runOnMainThread(() -> {
            dataType.preloadedByCode = true;
            resources.get(dataType.getClass()).put(dataType.getDataName(), dataType);
            addReferenceAndUpdate(dataType, "manually");
        });
    }

    /**
     * Register a resource owned by another one (textures of a model...), callable from a worker thread.
     * If a resource with the same name already exists it is shared and returned instead of the given one.
     * The owner adds a reference and must call releaseOwnedResource with the same owner name.
     */
    public <T extends DataType> T addOwnedResource(Class<T> type, T dataType, String owner) {
        DataType registered = resources.get(type).putIfAbsent(dataType.getDataName(), dataType);
        if (registered == null) {
            registered = dataType;
            registered.setNotManuallyCreated();
        }

        DataType shared = registered;
        runOnMainThread(() -> addReferenceAndUpdate(shared, owner));

        return type.cast(registered);
    }

    // Remove the reference added by addOwnedResource, the resource is unloaded when nothing uses it anymore
    public void releaseOwnedResource(DataType dataType, String owner) {
        runOnMainThread(() -> removeReferenceAndUpdate(dataType, owner));
    }

    // Unload then remove a data created by the code
    public void deleteResource(Class<? extends DataType> type, String name) {
        DataType dataType = resources.get(type).get(name);
        deleteResource(dataType);
    }

    public void deleteResource(DataType dataType) {
        if (dataType == null)
            return;

        if (!dataType.isManuallyCreated())
            throw new RuntimeException("Can't delete resource: " + dataType.getDataName() + " because it's not manually created");

        runOnMainThread(() -> {
            dataType.deleteWanted = true;
            update(dataType);
        });
    }
}
