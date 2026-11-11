package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.shader.ShaderLoader;
import io.github.mightycode.mightylib.graphics.text.FontLoader;
import io.github.mightycode.mightylib.resources.animation.AnimationDataLoader;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import io.github.mightycode.mightylib.resources.texture.IconLoader;
import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.resources.texture.TextureDataLoader;
import io.github.mightycode.mightylib.utils.Logger;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

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
    public static final String DEFAULT_FOLDER = "resources/";

    // Reference added by loadAllOfType (AllResourcesMethod, batch descriptions)
    private static final String ALL_REFERENCE = "all";
    // Prefix of the references a data adds to its dependencies
    static final String DEPENDENCY_REFERENCE_PREFIX = "dependency:";
    // Reference of a batch loaded in advance (preloadBatch): its data are loaded after those wanted now
    static final String PRELOAD_REFERENCE_PREFIX = "preload:";
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

    // Fixed at the construction: the mandatory loaders, then those of the project, one per type of data
    private final List<ResourceLoader> loaders;
    private final String folder;

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

    /**
     * The mandatory loaders are created (the shaders come from the shader manager: GLSL version and list), then those
     * of the project are added after them (IProjectLoading.loaders). The list can't change afterwards.
     * The loaders read their files in the given folder (EngineConfig.setResourcesFolder).
     */
    public Resources(LoadingMethod loadingMethod, int maxNumberOfWorkers, String folder, ShaderManager shaderManager,
                     List<ResourceLoader> projectLoaders) {
        this(loadingMethod, maxNumberOfWorkers, folder, withMandatoryLoaders(shaderManager, projectLoaders));
    }

    // Only the given loaders are used (no GL loader): tests
    Resources(LoadingMethod loadingMethod, int maxNumberOfWorkers, String folder, List<ResourceLoader> loaders) {
        this.loadingMethod = loadingMethod;
        this.folder = NormalizeFolder(folder);
        resources = new ConcurrentHashMap<>();
        this.loaders = checkedLoaders(loaders);

        scheduler = new ResourceScheduler(this, maxNumberOfWorkers);

        initialized = false;
        firstLoadDone = false;
    }

    private static List<ResourceLoader> withMandatoryLoaders(ShaderManager shaderManager,
                                                             List<ResourceLoader> projectLoaders) {
        List<ResourceLoader> all = new ArrayList<>();
        all.add(new ShaderLoader(shaderManager));
        all.add(new IconLoader());
        all.add(new TextureDataLoader());
        all.add(new AnimationDataLoader());
        all.add(new FontLoader());
        JSONLoader jsonLoader = new JSONLoader();
        all.add(jsonLoader);
        all.add(new BatchLoader(jsonLoader));

        all.addAll(projectLoaders);
        return all;
    }

    // An unmodifiable copy; two loaders of the same type of data would share (and overwrite) one registry
    private static List<ResourceLoader> checkedLoaders(List<ResourceLoader> loaders) {
        Set<Class<? extends DataType>> types = new HashSet<>();
        for (ResourceLoader loader : loaders) {
            if (!types.add(loader.getType()))
                throw new IllegalArgumentException("Two loaders for the type " + loader.getType().getSimpleName()
                        + " (" + loader.getResourceNameType() + ")");
        }

        return List.copyOf(loaders);
    }

    // The loaders, in their order (mandatory then project); unmodifiable
    List<ResourceLoader> loaders() {
        return loaders;
    }

    // Separators turned to '/', with a final '/': loaders concatenate the paths
    public static String NormalizeFolder(String folder) {
        String normalized = Objects.requireNonNull(folder, "folder").replace('\\', '/');
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    /**
     * Folder read by the loaders, ending with '/' (default "resources/"). Final and set by the constructor: safe to read
     * from any thread (workers, resource watcher).
     */
    public String folder() {
        return folder;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    // Where the batches hold the GL elements of their data (GLResources); null: nothing is sent to the card
    private IGLElementSink glElementSink;

    public void setGLElementSink(IGLElementSink glElementSink) {
        this.glElementSink = glElementSink;
    }

    IGLElementSink glElementSink() {
        return glElementSink;
    }

    // Where a sound buffer is detached from the sources holding it, before it is deleted (SoundManager); none in tests
    private IntConsumer soundBufferDetacher;

    public void setSoundBufferDetacher(IntConsumer soundBufferDetacher) {
        this.soundBufferDetacher = soundBufferDetacher;
    }

    // Before deleting a sound buffer: the sources still holding it let it go
    public void detachSoundBuffer(int bufferId) {
        if (soundBufferDetacher != null)
            soundBufferDetacher.accept(bufferId);
    }

    // Reloads reading their new version at the same time (both versions stay in memory); the others wait
    public void setMaxReloadCopies(int maxReloadCopies) {
        reloads.setMaxCopies(maxReloadCopies);
    }

    public void init() {
        if (initialized)
            return;

        // Init all loaders
        for (ResourceLoader loader : loaders){
            loader.attachTo(this);
            Map<String, DataType> map = new ConcurrentHashMap<>();
            loader.create(map);
            for (DataType data : map.values())
                data.attachTo(this);
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
            for (ResourceLoader loader : loaders){
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
        if (glElementSink != null)
            glElementSink.fallbackDataLoaded();
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

    // Main thread: same, until the deadline (System.nanoTime), at least one step. Called by MainLoop with the frame budget.
    public void processUntil(long deadlineNanos) {
        if (!initialized)
            throw new RuntimeException("Resources not initialized");

        scheduler.processUntil(deadlineNanos);
    }

    // Main thread: true when no data is being loaded or unloaded
    public boolean finishedCurrentLoading() {
        // What is only preloaded does not count: a loading screen does not wait for the next scene
        return scheduler.isIdle() && dependencies.waitingUrgentCount() == 0;
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
        int[] progress = wantedProgress();
        pipeline.put("wantedDone", progress[0]);
        pipeline.put("wantedTotal", progress[1]);
        return pipeline;
    }

    Map<Class<? extends DataType>, Map<String, DataType>> registry() {
        return resources;
    }

    // Names of the declared data of a type (loaded or not), empty for an unknown type
    public List<String> getResourceNames(Class<? extends DataType> type) {
        Map<String, DataType> ofType = resources.get(type);
        return ofType == null ? new ArrayList<>() : new ArrayList<>(ofType.keySet());
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

        return !shuttingDown && !data.deleteWanted && data.isReferenced();
    }

    void update(DataType data) {
        if (data.jobRunning) {
            // The running step is useless: the loader may stop it (DataType.checkCancelled)
            if (!isWanted(data) || data.reloadWanted)
                data.setCancelled(true);
            return;
        }

        if (!isWanted(data)) {
            dependencies.stopWaiting(data);
            dependencies.release(data);
            data.failed = false;
            data.reloadWanted = false;
            data.urgent = false;
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
        data.setCancelled(false);
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

        scheduler.submit(data, preload, work, end);
    }

    private static class StepResult {
        boolean success = true;
        Throwable error;
    }

    private void endStep(DataType data, boolean preload, StepResult result) {
        data.jobRunning = false;

        if (result.error instanceof CancellationException) {
            // Stopped by the loader: nothing to keep, update discards the data or starts it again
            scheduler.stepCancelled();
        } else if (result.error instanceof ResourceException) {
            // Expected (the data is wrong, not the code): one line, no stack trace
            fail(data, result.error.getMessage());
        } else if (result.error != null) {
            Logger.LogException("Unexpected error while loading " + data.getDataName(), result.error);
            fail(data, (preload ? "preload" : "load") + " threw " + result.error);
        } else if (preload) {
            data.setPreloaded();
        } else if (result.success) {
            if (data.loadContinues()) {
                // Part of the load done: the update below queues the next step
            } else if (data.reloadOf != null && isWanted(data)) {
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

        for (ResourceLoader candidate : loaders) {
            if (candidate.getType().isInstance(data))
                return candidate;
        }

        return null;
    }

    void addReferenceAndUpdate(DataType data, String reference) {
        data.addReference(reference);
        // A dependency reference is urgent as its dependent is (DependencyGraph.acquire)
        if (!reference.startsWith(PRELOAD_REFERENCE_PREFIX) && !reference.startsWith(DEPENDENCY_REFERENCE_PREFIX))
            makeUrgent(data);

        requestUpdate(data);
    }

    // True when the steps of the data go before the preload ones: wanted now, or not wanted any more (skipped quickly)
    boolean isUrgent(DataType data) {
        DataType original = data.reloadOf != null ? data.reloadOf : data;
        return data.urgent || original.urgent || !isWanted(data);
    }

    // The data and what it depends on are wanted now: their queued steps are promoted, the next ones go first
    void makeUrgent(DataType data) {
        if (data.urgent)
            return;

        data.urgent = true;
        scheduler.promote(data);
        for (DataType dependency : data.getDependencies())
            makeUrgent(dependency);
    }

    void removeReferenceAndUpdate(DataType data, String reference) {
        data.removeReference(reference);
        requestUpdate(data);
    }

    // ---- Registry ----

    public Class<? extends DataType> getClassFromName(String name) {
        for (ResourceLoader resourceLoader : loaders) {
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
        for (ResourceLoader loader : loaders){
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

    /**
     * Load a batch in advance (the next scene): its data are read and loaded after everything wanted now, and a
     * loading screen does not wait for them. Loading the batch for real (loadBatch) makes them urgent, and costs
     * nothing for what is already loaded. Cancel with cancelPreloadBatch once the batch is loaded or no longer wanted.
     */
    public void preloadBatch(String batchName) {
        if (!(loadingMethod instanceof BatchResourcesMethod)) {
            System.err.println("Can't preload batch: " + batchName + " when loading method is not BatchResources");
            return;
        }

        batches.preload(batchName);
    }

    // Release the reference of one preloadBatch: what nothing else wants is unloaded
    public void cancelPreloadBatch(String batchName) {
        if (!(loadingMethod instanceof BatchResourcesMethod))
            return;

        batches.cancelPreload(batchName);
    }

    // What a batch loads: data by type, loaded ones, estimated memory (see BatchManager.preview)
    public JSONObject describeBatch(String batchName) {
        return batches.preview(batchName);
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

    /**
     * Any thread (the resource watcher): looks for the files created since the start, and registers the data of those
     * the filter accepts, so they can be loaded by name or by a batch pattern. Existing data are left untouched.
     */
    void discoverNewFiles(java.util.function.BiPredicate<Class<? extends DataType>, DataType> accepted) {
        Map<Class<? extends DataType>, Map<String, DataType>> found = new LinkedHashMap<>();
        for (ResourceLoader loader : loaders) {
            Map<String, DataType> map = new HashMap<>();
            try {
                loader.discover(map);
            } catch (RuntimeException e) {
                System.err.println("--Watch: can't look for new " + loader.getResourceNameType() + " files: "
                        + e.getMessage());
                continue;
            }
            found.put(loader.getType(), map);
        }

        runOnMainThread(() -> {
            int added = 0;
            for (Map.Entry<Class<? extends DataType>, Map<String, DataType>> ofType : found.entrySet()) {
                Map<String, DataType> registered = resources.get(ofType.getKey());
                if (registered == null)
                    continue;

                for (Map.Entry<String, DataType> entry : ofType.getValue().entrySet()) {
                    if (registered.containsKey(entry.getKey()) || !accepted.test(ofType.getKey(), entry.getValue()))
                        continue;

                    entry.getValue().attachTo(this);
                    registered.put(entry.getKey(), entry.getValue());
                    ++added;
                    System.out.println("--Watch: new " + ofType.getKey().getSimpleName() + " " + entry.getKey());
                }
            }

            // A batch described by a pattern may list the new data
            if (added > 0)
                batches.refreshLoaded();
        });
    }

    // Any thread: every registered data with its registered type (weakly consistent snapshot)
    void forEachRegistered(java.util.function.BiConsumer<Class<? extends DataType>, DataType> action) {
        for (Map.Entry<Class<? extends DataType>, Map<String, DataType>> ofType : resources.entrySet()) {
            for (DataType data : ofType.getValue().values())
                action.accept(ofType.getKey(), data);
        }
    }

    /**
     * Main thread: {done, total} of the data wanted now (not the preloaded ones, the placeholders nor the reload
     * copies); a failed data counts as done (its fallback is used). For a loading screen: show done - done at the
     * start of the loading over total - done at the start. The total grows when a data read adds dependencies.
     */
    public int[] wantedProgress() {
        int[] progress = new int[2];
        forEachRegistered((type, data) -> {
            if (!isWanted(data) || !isUrgent(data) || isFallback(data))
                return;

            ++progress[1];
            if (data.isLoaded() || data.hasFailed())
                ++progress[0];
        });

        return progress;
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

    // Data created by the code, read by its loader then loaded; referenced as "manually"
    public void addResourceAndSetUp(Class<? extends DataType> type, DataType dataType) {
        runOnMainThread(() -> {
            dataType.attachTo(this);
            resources.get(type).put(dataType.getDataName(), dataType);
            addReferenceAndUpdate(dataType, "manually");
        });
    }

    // Data created and preloaded (setPreloaded) by the code, then loaded; referenced as "manually"
    public void addPreLoadedResource(DataType dataType) {
        runOnMainThread(() -> {
            dataType.preloadedByCode = true;
            dataType.attachTo(this);
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
        // Attached before being visible to other threads; a data given while another one is shared is dropped
        dataType.attachTo(this);
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
