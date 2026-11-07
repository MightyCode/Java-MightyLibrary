package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.shader.ShaderLoader;
import io.github.mightycode.mightylib.graphics.text.FontLoader;
import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.main.Workers;
import io.github.mightycode.mightylib.resources.animation.AnimationDataLoader;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import io.github.mightycode.mightylib.resources.texture.IconLoader;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.resources.texture.TextureDataLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String FALLBACK_REFERENCE = "fallback";

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
    // Fallback by loader type, filled by init then only read
    private final Map<Class<? extends DataType>, DataType> fallbacks;
    // Unknown names already reported by getResourceOrFallback (main thread)
    private final Set<String> reportedUnknownNames;
    // Types without reload copy, reported once (main thread)
    private final Set<Class<?>> reportedNoReloadCopy = new HashSet<>();
    private final LoadingMethod loadingMethod;

    private final int maxNumberOfWorkers;

    // Any thread -> main thread: calls from workers and ends of worker steps
    private final ConcurrentLinkedQueue<Runnable> inbox;

    // Main thread only
    private final ArrayDeque<DataType> toUpdate;
    private final ArrayDeque<Runnable> mainContextSteps;
    private final ArrayDeque<Runnable> workerSteps;
    private int runningWorkerSteps;
    private int waitingDependenciesCount;
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
        fallbacks = new ConcurrentHashMap<>();
        reportedUnknownNames = new HashSet<>();
        Loaders = new ArrayList<>();

        this.maxNumberOfWorkers = Math.max(1, maxNumberOfWorkers);

        inbox = new ConcurrentLinkedQueue<>();
        toUpdate = new ArrayDeque<>();
        mainContextSteps = new ArrayDeque<>();
        workerSteps = new ArrayDeque<>();

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

    public void init() {
        if (initialized)
            return;

        // Init all loaders
        for (ResourceLoader loader : Loaders){
            Map<String, DataType> map = new ConcurrentHashMap<>();
            loader.create(map);
            resources.put(loader.getType(), map);
            registerFallback(loader, map);
        }

        initialized = true;
    }

    private void registerFallback(ResourceLoader loader, Map<String, DataType> map) {
        DataType fallback = loader.createFallback();
        if (fallback == null)
            return;

        if (!FALLBACK_NAME.equals(fallback.getDataName()))
            throw new IllegalStateException("The fallback of " + loader.getResourceNameType() + " must be named " + FALLBACK_NAME);

        if (map.containsKey(FALLBACK_NAME))
            System.err.println("--The " + loader.getResourceNameType() + " named " + FALLBACK_NAME
                    + " is replaced by the fallback of its type");

        fallback.setNotManuallyCreated();
        map.put(FALLBACK_NAME, fallback);
        fallbacks.put(loader.getType(), fallback);
    }

    // Referenced for the whole program: loaded first, unloaded at the exit
    void loadFallbacks() {
        runOnMainThread(() -> {
            for (DataType fallback : fallbacks.values())
                addReferenceAndUpdate(fallback, FALLBACK_REFERENCE);
        });
    }

    // ---- Fallbacks ----

    // Fallback of the type, or of its closest parent type with a loader; null without fallback
    public <T extends DataType> T getFallback(Class<T> type) {
        DataType fallback = fallbackOf(type);
        return type.isInstance(fallback) ? type.cast(fallback) : null;
    }

    // A ColorTextureData uses the TextureData fallback
    private DataType fallbackOf(Class<?> type) {
        for (Class<?> current = type; current != null && DataType.class.isAssignableFrom(current); current = current.getSuperclass()) {
            DataType fallback = fallbacks.get(current);
            if (fallback != null)
                return fallback;
        }

        return null;
    }

    private boolean hasFallback(DataType data) {
        return fallbackOf(data.getClass()) != null;
    }

    public boolean isFallback(DataType data) {
        return data != null && fallbackOf(data.getClass()) == data;
    }

    /**
     * What to use for this data now: the data itself when loaded, otherwise the fallback of the type
     * (data failed, still loading or null), or the data as is when the type has no fallback.
     */
    public <T extends DataType> T usable(Class<T> type, T data) {
        if (data != null && data.isLoaded())
            return data;

        T fallback = getFallback(type);
        return fallback != null ? fallback : data;
    }

    // getResource, with the fallback of the type for an unknown name (reported once per name)
    public <T extends DataType> T getResourceOrFallback(Class<T> type, String name) {
        T data = getResource(type, name);
        if (data != null)
            return data;

        T fallback = getFallback(type);
        String key = type.getSimpleName() + " " + name;
        if (strict)
            throw new IllegalStateException("Strict resources: unknown " + key);

        if (reportedUnknownNames.add(key))
            System.err.println("--Unknown " + key + (fallback != null ? ", its fallback is used" : ""));

        return fallback;
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

        long startTime = System.currentTimeMillis();
        boolean first = true;

        while (first || allowedMilliseconds == -1 || System.currentTimeMillis() - startTime < allowedMilliseconds) {
            first = false;

            Runnable fromInbox;
            while ((fromInbox = inbox.poll()) != null)
                fromInbox.run();

            DataType data;
            while ((data = toUpdate.poll()) != null)
                update(data);

            startWorkerSteps();

            Runnable step = mainContextSteps.poll();
            if (step == null)
                return;

            step.run();
        }
    }

    // Main thread: true when no data is being loaded or unloaded
    public boolean finishedCurrentLoading() {
        return inbox.isEmpty() && toUpdate.isEmpty() && mainContextSteps.isEmpty() && workerSteps.isEmpty()
                && runningWorkerSteps == 0 && waitingDependenciesCount == 0;
    }

    /**
     * Main thread: by type, the number of loaded, preloaded and referenced data, and the names of the data whose
     * state does not match their references (loaded but not referenced = leak, referenced but not loaded = still loading
     * or failed); then the state of the pipeline.
     */
    public JSONObject describe() {
        JSONObject types = new JSONObject();
        for (Map.Entry<Class<? extends DataType>, Map<String, DataType>> ofType : resources.entrySet()) {
            int loaded = 0, preloaded = 0, referenced = 0;
            JSONArray loadedNotReferenced = new JSONArray();
            JSONArray referencedNotLoaded = new JSONArray();
            JSONArray onlyAsDependency = new JSONArray();
            JSONArray usingFallback = new JSONArray();
            JSONArray reloading = new JSONArray();

            for (DataType data : ofType.getValue().values()) {
                boolean isReferenced = !data.notReferenced();
                if (data.reloadCopy != null)
                    reloading.put(data.getDataName());
                if (data.isLoaded()) ++loaded;
                if (data.isPreLoaded()) ++preloaded;
                if (isReferenced) ++referenced;
                if (isReferenced && onlyReferencedAsDependency(data))
                    onlyAsDependency.put(data.getDataName());
                if (isReferenced && data.failed && hasFallback(data))
                    usingFallback.put(data.getDataName());

                if (data.isLoaded() && !isReferenced)
                    loadedNotReferenced.put(data.getDataName());
                else if (!data.isLoaded() && isReferenced)
                    referencedNotLoaded.put(data.getDataName() + (data.failed ? " (failed)" : ""));
            }

            if (loaded == 0 && referenced == 0)
                continue;

            JSONObject type = new JSONObject();
            type.put("loaded", loaded);
            type.put("preloaded", preloaded);
            type.put("referenced", referenced);
            if (!loadedNotReferenced.isEmpty())
                type.put("loadedNotReferenced", loadedNotReferenced);
            if (!referencedNotLoaded.isEmpty())
                type.put("referencedNotLoaded", referencedNotLoaded);
            // Wanted only because another data depends on it (not listed by a batch nor added by the code)
            if (!onlyAsDependency.isEmpty())
                type.put("onlyAsDependency", onlyAsDependency);
            // Failed, replaced by the fallback of the type until reloaded
            if (!usingFallback.isEmpty())
                type.put("usingFallback", usingFallback);
            // Loaded and reading their new version apart
            if (!reloading.isEmpty())
                type.put("reloading", reloading);

            types.put(ofType.getKey().getSimpleName(), type);
        }

        JSONObject pipeline = new JSONObject();
        pipeline.put("runningWorkerSteps", runningWorkerSteps);
        pipeline.put("waitingWorkerSteps", workerSteps.size());
        pipeline.put("mainContextSteps", mainContextSteps.size());
        pipeline.put("waitingDependencies", waitingDependenciesCount);
        pipeline.put("finished", finishedCurrentLoading());

        JSONObject result = new JSONObject();
        result.put("types", types);
        result.put("pipeline", pipeline);
        return result;
    }

    private static boolean onlyReferencedAsDependency(DataType data) {
        synchronized (data.references) {
            for (String reference : data.references) {
                if (!reference.startsWith(DEPENDENCY_REFERENCE_PREFIX))
                    return false;
            }
        }

        return true;
    }

    // ---- Reconciliation, main thread only ----

    private void runOnMainThread(Runnable task) {
        if (MainThread.isMainThread())
            task.run();
        else
            inbox.add(task);
    }

    private void requestUpdate(DataType data) {
        // Immediate data are set up in the call that wants them (batch descriptions are read right after)
        if (data.getTypeSetUp() == DataType.TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT)
            update(data);
        else
            toUpdate.add(data);
    }

    private boolean isWanted(DataType data) {
        // A reload copy is wanted while it is the current copy of a wanted data
        if (data.reloadOf != null)
            return data.reloadOf.reloadCopy == data && isWanted(data.reloadOf);

        return !shuttingDown && !data.deleteWanted && !data.notReferenced();
    }

    private void update(DataType data) {
        if (data.jobRunning)
            return;

        if (!isWanted(data)) {
            stopWaitingDependencies(data);
            releaseDependencies(data);
            data.failed = false;
            data.reloadWanted = false;
            data.discard();

            // The new version being read is not needed either
            DataType copy = data.reloadCopy;
            if (copy != null) {
                data.reloadCopy = null;
                update(copy);
            }

            if (data.deleteWanted) {
                Map<String, DataType> map = resources.get(data.getClass());
                if (map != null)
                    map.remove(data.getDataName(), data);
            }
            return;
        }

        if (data.reloadWanted) {
            data.reloadWanted = false;

            // Loaded: the new version is read apart, the data stays usable meanwhile
            if (data.isLoaded() && data.reloadOf == null && startReloadCopy(data))
                return;

            data.failed = false;
            stopWaitingDependencies(data);
            releaseDependencies(data);
            data.discard();
        }

        if (data.failed || data.isLoaded())
            return;

        if (!data.isPreLoaded()) {
            startStep(data, true);
            return;
        }

        acquireDependencies(data);

        for (DataType dependency : data.getDependencies()) {
            // A failed dependency with a fallback is replaced by it (Resources.usable): the data can be loaded
            if (dependency.failed && !hasFallback(dependency)) {
                fail(data, "its dependency " + dependency.getClass().getSimpleName() + " "
                        + dependency.getDataName() + " failed");
                return;
            }
        }

        if (!dependenciesReady(data)) {
            waitDependencies(data);
        } else {
            stopWaitingDependencies(data);
            startStep(data, false);
        }
    }

    // Loaded (or replaced by its fallback) and not reading a new version: computed once, with the latest one
    private boolean dependencyReady(DataType dependency) {
        return (dependency.isLoaded() && dependency.reloadCopy == null) || (dependency.failed && hasFallback(dependency));
    }

    private boolean dependenciesReady(DataType data) {
        for (DataType dependency : data.getDependencies()) {
            if (!dependencyReady(dependency))
                return false;
        }

        return true;
    }

    // A wanted data references its dependencies (known once preloaded), so they are loaded without being listed
    private void acquireDependencies(DataType data) {
        for (DataType dependency : data.getDependencies()) {
            if (!data.acquiredDependencies.contains(dependency)) {
                data.acquiredDependencies.add(dependency);
                addDependent(dependency, data);
                addReferenceAndUpdate(dependency, data.dependencyReference());
            }
        }
    }

    private void releaseDependencies(DataType data) {
        if (data.acquiredDependencies.isEmpty())
            return;

        List<DataType> released = new ArrayList<>(data.acquiredDependencies);
        data.acquiredDependencies.clear();
        for (DataType dependency : released) {
            dependency.dependents.remove(data);
            removeReferenceAndUpdate(dependency, data.dependencyReference());
        }
    }

    // A copy is not told: it waits for its dependencies to finish their own reload (dependencyReady)
    private static void addDependent(DataType dependency, DataType dependent) {
        if (dependent.reloadOf == null && !dependency.dependents.contains(dependent))
            dependency.dependents.add(dependent);
    }

    // ---- Hot reload ----

    // Request to read again a data: apart if it is loaded (see update), in place otherwise
    private void requestReload(DataType data) {
        // Preloaded or generated by the code: nothing to read again (computed again when a dependency changes)
        if (data.preloadedByCode || !data.hasSource() || isFallback(data))
            return;

        // A failed data is read again too: fixed, it replaces its fallback
        if (data.isLoaded() || data.jobRunning || data.failed) {
            data.reloadWanted = true;
            requestUpdate(data);
        }
    }

    // False when the type has no reload copy: the data is then emptied and read again in place
    private boolean startReloadCopy(DataType data) {
        DataType copy = data.reloadCopy;
        if (copy != null) {
            // Already reading a new version: read it once more, the source may have changed again
            copy.reloadWanted = true;
            requestUpdate(copy);
            return true;
        }

        copy = data.createReloadCopy();
        if (copy == null) {
            if (reportedNoReloadCopy.add(data.getClass()))
                System.err.println("--" + data.getClass().getSimpleName()
                        + " has no reload copy: emptied while it is read again");
            return false;
        }

        copy.reloadOf = data;
        data.reloadCopy = copy;
        requestUpdate(copy);
        return true;
    }

    // The copy is loaded: its content replaces the one of its data, the previous content is unloaded with the copy
    private void commitReload(DataType copy) {
        DataType data = copy.reloadOf;
        copy.loadDone(true);

        for (DataType dependency : data.acquiredDependencies)
            dependency.dependents.remove(data);

        data.takeReloadedContent(copy);

        // The references to the dependencies follow them: the copy releases the previous ones
        List<DataType> previous = new ArrayList<>(data.acquiredDependencies);
        data.acquiredDependencies.clear();
        data.acquiredDependencies.addAll(copy.acquiredDependencies);
        copy.acquiredDependencies.clear();
        copy.acquiredDependencies.addAll(previous);
        for (DataType dependency : data.acquiredDependencies)
            addDependent(dependency, data);

        data.reloadCopy = null;
        releaseDependencies(copy);
        copy.discard();

        wakeWaiters(data);
        contentChanged(data);
    }

    // The loaded data using this one computed their content with the previous version: they are read again
    private void contentChanged(DataType data) {
        for (DataType dependent : new ArrayList<>(data.dependents)) {
            // A fallback is never emptied: it must not depend on what can change
            if (!dependent.isLoaded() || isFallback(dependent))
                continue;

            DataType copy = dependent.reloadCopy;
            if (copy == null) {
                dependent.reloadWanted = true;
                requestUpdate(dependent);
            // A copy still reading waits for its dependencies; one being computed may use the previous version
            } else if (copy.jobRunning && copy.isPreLoaded()) {
                copy.reloadWanted = true;
            }
        }
    }

    // The new version could not be read: the data keeps the previous one
    private void failReload(DataType copy, String message) {
        DataType data = copy.reloadOf;
        boolean wanted = isWanted(copy);

        stopWaitingDependencies(copy);
        releaseDependencies(copy);
        copy.discard();

        if (!wanted)
            return;

        data.reloadCopy = null;
        System.err.println("--" + message + ", the previous version is kept");
        wakeWaiters(data);
    }

    /**
     * The data cannot be loaded: back to the initial state (its dependencies released) until it is no longer wanted
     * or reloaded; the data waiting for it fail too, instead of waiting forever.
     */
    private void fail(DataType data, String reason) {
        String message = data.getClass().getSimpleName() + " " + data.getDataName() + " failed: " + reason;
        // A fallback must never fail: nothing could replace it
        if (data.reloadOf != null && !isWanted(data)) {
            failReload(data, message);
            return;
        }

        if (strict || isFallback(data))
            throw new IllegalStateException((strict ? "Strict resources: " : "Fallback failed: ") + message);

        if (data.reloadOf != null) {
            failReload(data, message);
            return;
        }

        boolean replaced = hasFallback(data);
        System.err.println("--" + message + (replaced ? ", its fallback is used" : ""));

        data.failed = true;
        stopWaitingDependencies(data);
        releaseDependencies(data);
        data.discard();

        List<DataType> waiters = new ArrayList<>(data.waiters);
        data.waiters.clear();
        for (DataType waiter : waiters) {
            if (!waiter.waitingDependencies || waiter.failed)
                continue;

            // Replaced by the fallback: the waiters go on with it
            if (replaced)
                toUpdate.add(waiter);
            else
                fail(waiter, "its dependency " + data.getClass().getSimpleName() + " " + data.getDataName() + " failed");
        }
    }

    private void waitDependencies(DataType data) {
        if (!data.waitingDependencies) {
            data.waitingDependencies = true;
            ++waitingDependenciesCount;
        }

        for (DataType dependency : data.getDependencies()) {
            if (!dependencyReady(dependency) && !dependency.waiters.contains(data))
                dependency.waiters.add(data);
        }
    }

    private void stopWaitingDependencies(DataType data) {
        if (data.waitingDependencies) {
            data.waitingDependencies = false;
            --waitingDependenciesCount;
        }
    }

    private void wakeWaiters(DataType loaded) {
        for (DataType waiter : loaded.waiters) {
            if (waiter.waitingDependencies)
                toUpdate.add(waiter);
        }

        loaded.waiters.clear();
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

        switch (data.getTypeSetUp()) {
            case IMMEDIATELY_IN_CURRENT_CONTEXT:
                work.run();
                end.run();
                break;
            case MAIN_CONTEXT:
                mainContextSteps.add(() -> {
                    work.run();
                    end.run();
                });
                break;
            case THREAD_CONTEXT:
                workerSteps.add(() -> {
                    work.run();
                    inbox.add(() -> {
                        --runningWorkerSteps;
                        end.run();
                    });
                });
                startWorkerSteps();
                break;
        }
    }

    private static class StepResult {
        boolean success = true;
        Throwable error;
    }

    private void endStep(DataType data, boolean preload, StepResult result) {
        data.jobRunning = false;

        if (result.error != null) {
            result.error.printStackTrace();
            fail(data, (preload ? "preload" : "load") + " threw " + result.error);
        } else if (preload) {
            data.setPreloaded();
        } else if (result.success) {
            if (data.reloadOf != null && isWanted(data)) {
                commitReload(data);
            } else {
                data.loadDone(true);
                wakeWaiters(data);
                contentChanged(data);
            }
        } else {
            fail(data, "load returned false");
        }

        // Wanted state may have changed meanwhile: unloaded if no longer referenced, next step otherwise
        requestUpdate(data);
    }

    private void startWorkerSteps() {
        Runnable step;
        while (runningWorkerSteps < maxNumberOfWorkers && (step = workerSteps.poll()) != null) {
            ++runningWorkerSteps;
            Workers.execute(step);
        }
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

    private void addReferenceAndUpdate(DataType data, String reference) {
        data.addReference(reference);
        requestUpdate(data);
    }

    private void removeReferenceAndUpdate(DataType data, String reference) {
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

    private static Class<? extends DataType> getClassFromBatchName(String batchName, String name) {
        Class<? extends DataType> type = getClassFromName(name);
        if (type == null)
            throw new RuntimeException("Unknown resource type: " + name + " in batch: " + batchName);

        return type;
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

    // ---- Batches ----

    private interface BatchAction {
        void apply(DataType data);
    }

    // Calls the action on every data listed by the batch, by name or by regular expression
    private void forEachInBatch(String batchName, BatchAction action) {
        BatchResources batchResources = getResource(BatchResources.class, batchName);
        if (batchResources == null || batchResources.getObject() == null)
            throw new RuntimeException("Batch not loaded: " + batchName);

        JSONArray content = batchResources.getObject().getJSONArray("batchs");
        for (int j = 0; j < content.length(); ++j) {
            JSONObject resource = content.getJSONObject(j);

            Class<? extends DataType> type = getClassFromBatchName(batchName, resource.getString("type"));
            Map<String, DataType> ofType = this.resources.get(type);

            JSONArray resourceFiles = resource.getJSONArray("files");
            for (int i = 0; i < resourceFiles.length(); ++i) {
                String resourceName = resourceFiles.getString(i);
                DataType dataType = ofType.get(resourceName);

                if (dataType == null)
                    throw new RuntimeException("Can't find resource: " + resourceName + " of type: " + type.getName());

                action.apply(dataType);
            }

            JSONArray regularExpressions = resource.getJSONArray("regex");
            for (int i = 0; i < regularExpressions.length(); ++i) {
                Pattern pattern = Pattern.compile(regularExpressions.getString(i));

                for (Map.Entry<String, DataType> entry : ofType.entrySet()) {
                    Matcher matcher = pattern.matcher(entry.getKey());
                    if (matcher.matches())
                        action.apply(entry.getValue());
                }
            }
        }
    }

    public void loadBatch(String batchName) {
        if (!(loadingMethod instanceof BatchResourcesMethod)) {
            System.err.println("Can't load batch: " + batchName + " when loading method is not BatchResources");
            return;
        }

        runOnMainThread(() -> {
            System.out.println("--Load batch: " + batchName);
            forEachInBatch(batchName, data -> addReferenceAndUpdate(data, batchName));
        });
    }

    public void unloadBatch(String batchName) {
        if (!(loadingMethod instanceof BatchResourcesMethod)) {
            System.err.println("Can't unload batch: " + batchName + " when loading method is not BatchResources");
            return;
        }

        runOnMainThread(() -> {
            System.out.println("--Unload batch: " + batchName);
            forEachInBatch(batchName, data -> removeReferenceAndUpdate(data, batchName));
        });
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

    // Read again from the disk the loaded data of this type
    public void reloadAllOfType(Class<? extends DataType> typeOfResource) {
        runOnMainThread(() -> {
            for (DataType dataType : resources.get(typeOfResource).values())
                requestReload(dataType);
        });
    }

    /**
     * Read again one data from its source. A loaded data stays usable with its previous version until the new one
     * is loaded (or kept if it fails); then the data using it are read again. A failed data is read again in place.
     */
    public void reload(DataType dataType) {
        runOnMainThread(() -> requestReload(dataType));
    }

    // At the exit: unload everything not being read, whatever its references
    public void unload(){
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
