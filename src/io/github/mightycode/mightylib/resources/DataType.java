package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.utils.LoadingElement;
import io.github.mightycode.mightylib.utils.math.UUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data read from the disk (preload) then computed (load), managed by Resources.
 * States and references are changed by the main thread only (Resources is their single owner);
 * workers only run initWithFile and internLoad on a data that Resources does not touch meanwhile.
 */
public abstract class DataType extends UUID implements LoadingElement {
    protected final Resources resources;

    protected final String dataName;

    // Volatile: written by the main thread, read by any thread
    // Step where data are read from disk and dependencies are listed
    private volatile boolean correctlyPreLoaded;

    // When dependencies are loaded, this data can be loaded and well computed
    private volatile boolean correctlyLoaded;

    // Names of what uses this data (batches, "manually"...), the same name can appear several times
    protected final List<String> references;

    // Filled while preloading in a worker thread, read from the main thread
    private final CopyOnWriteArrayList<DataType> dependencies;

    private final TYPE_SET_UP typeSetUp;

    private boolean manuallyCreated;

    // Pipeline state, main thread only (see Resources)
    boolean jobRunning;
    // Set while a step runs and its result is no longer wanted: read by the loaders (checkCancelled)
    private volatile boolean cancelled;
    boolean failed;
    boolean reloadWanted;
    boolean deleteWanted;
    boolean waitingDependencies;
    // Wanted now (by a scene, the code, a batch loaded normally), not only preloaded: its steps go first (see Resources.isUrgent)
    boolean urgent;
    // Added by addPreLoadedResource: no file to read again on reload
    boolean preloadedByCode;
    // Data waiting for this one to be loaded
    final List<DataType> waiters = new ArrayList<>();
    // Dependencies this data references (see DependencyGraph.acquire), released with the data
    final List<DataType> acquiredDependencies = new ArrayList<>();
    // Loaded data that depend on this one, told when its content changes (copies are not listed)
    final List<DataType> dependents = new ArrayList<>();
    // Hot reload: the copy reading the new version of a loaded data, and the data a copy is for
    DataType reloadCopy;
    DataType reloadOf;

    // Increased each time the content changes (load, reload): users compare it to the version they used
    private volatile int version;

    public enum TYPE_SET_UP {
        IMMEDIATELY_IN_CURRENT_CONTEXT,
        MAIN_CONTEXT,
        THREAD_CONTEXT
    }

    public DataType(TYPE_SET_UP typeSetUp, String dataName){
        super();
        resources = Resources.getInstance();

        this.dataName = dataName;
        this.correctlyLoaded = false;
        this.typeSetUp = typeSetUp;

        // Owned resources (model textures) get their references from worker threads
        references = Collections.synchronizedList(new ArrayList<>());
        dependencies = new CopyOnWriteArrayList<>();
        manuallyCreated = true;
    }

    final void setNotManuallyCreated(){
        manuallyCreated = false;
    }

    public final boolean isManuallyCreated(){
        return manuallyCreated;
    }

    public final TYPE_SET_UP getTypeSetUp(){
        return typeSetUp;
    }

    public final boolean hasDependencies(){
        return !dependencies.isEmpty();
    }

    final List<DataType> getDependencies(){
        return dependencies;
    }

    void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * For the long steps of a loader (worker thread), called between two phases, before the costly one: stops the step
     * when its result would be thrown away (data no longer wanted, or read again from the start). Whatever the step
     * allocated must be released by try/finally, as for any exception.
     */
    public final void checkCancelled() {
        if (cancelled)
            throw new java.util.concurrent.CancellationException();
    }

    // A dependency is referenced by this data while it is wanted: it does not have to be listed by a batch
    public final void addDependency(DataType dependency){
        if (dependency == null)
            throw new IllegalArgumentException("Null dependency for " + getClass().getSimpleName() + " " + dataName
                    + " (unknown resource name?)");

        // Preloading again (reload) adds the same dependencies again
        dependencies.addIfAbsent(dependency);
    }

    // Name of the reference this data adds to its dependencies
    final String dependencyReference() {
        return Resources.DEPENDENCY_REFERENCE_PREFIX + getClass().getSimpleName() + ":" + dataName;
    }

    public final void removeDependency(DataType dependency){
        dependencies.remove(dependency);
    }

    public final boolean canBeLoad(){
        for (DataType dependency : dependencies){
            if (!dependency.isLoaded()){
                return false;
            }
        }

        return true;
    }

    public final void addReference(String reference) {
        references.add(reference);
    }

    // Remove one occurrence of the reference
    public final void removeReference(String reference){
        references.remove(reference);
    }

    // Main thread: failed and not reloaded since (its fallback is used, if its type has one)
    public final boolean hasFailed() {
        return failed;
    }

    public final boolean notReferenced() {
        return references.isEmpty();
    }

    public final String getDataName(){
        return this.dataName;
    }

    // When set up manually, call this function to notify the ending of the preloading
    public final void setPreloaded() {
        correctlyPreLoaded = true;
    }

    protected abstract boolean internLoad();

    // Load step run by Resources (maybe on a worker): the state is set afterward by loadDone, on the main thread
    final boolean runLoad() {
        if (!correctlyPreLoaded)
            throw new RuntimeException("Data " + dataName + " is not preloaded");

        return internLoad();
    }

    // On failure the data stays preloaded: Resources discards it (internCancelPreload)
    final void loadDone(boolean success) {
        if (success) {
            correctlyLoaded = true;
            ++version;
        }
    }

    // False for a data generated by the code (no file): nothing to read again
    public boolean hasSource() {
        return true;
    }

    // Files this data is read from (watched for hot reload); empty when it has no file
    public List<String> sourcePaths() {
        return Collections.emptyList();
    }

    /**
     * Memory (bytes) this data takes once loaded on the CPU side. Until it is loaded, a guess from its files:
     * the size of the sources. Types that expand (textures) or compress override it.
     */
    public long estimatedCpuBytes() {
        long total = 0;
        for (String path : sourcePaths()) {
            long length = new java.io.File(path).length();
            if (length > 0)
                total += length;
        }

        return total;
    }

    // Memory (bytes) of the card this data takes once its GL element is loaded, 0 for the data that are not sent to it
    public long estimatedGpuBytes() {
        return 0;
    }

    public final int getVersion() {
        return version;
    }

    /**
     * Hot reload: a new data of the same type, name and source, read and loaded apart while this one stays usable,
     * then given to swapContent. Null (default) when the type does not support it: the data is emptied then read again.
     */
    protected DataType createReloadCopy() {
        return null;
    }

    /**
     * Exchange the content (everything read or computed, not the pipeline state) with the loaded copy:
     * this data gets the new version, the copy gets the previous one and is unloaded right after (internUnload).
     */
    protected void swapContent(DataType copy) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " has a reload copy but no swapContent");
    }

    // A data managed by its owner outside Resources changed its content (see TextureDataAtlas.replaceContentWith)
    protected final void markContentChanged() {
        ++version;
    }

    // Main thread (Resources): the loaded copy becomes the content of this data, its dependencies with it
    final void takeReloadedContent(DataType copy) {
        swapContent(copy);

        List<DataType> previous = new ArrayList<>(dependencies);
        dependencies.clear();
        dependencies.addAll(copy.dependencies);
        copy.dependencies.clear();
        copy.dependencies.addAll(previous);

        ++version;
    }

    // Back to the initial state, whether the data was loaded or only preloaded
    final void discard() {
        if (correctlyLoaded)
            internUnload();
        else if (correctlyPreLoaded)
            internCancelPreload();

        correctlyPreLoaded = false;
        correctlyLoaded = false;
    }

    // Preloaded but no longer wanted before being loaded: release what initWithFile kept (decoded files, owned resources)
    protected void internCancelPreload() {}

    // Compute again a loaded data (its content changed)
    protected final void load() {
        if (!correctlyPreLoaded)
            throw new RuntimeException("Data " + dataName + " is not preloaded");

        if (internLoad()) {
            correctlyLoaded = true;
            ++version;
        } else {
            // Back to the initial state: an unload would otherwise wait forever for a loading that failed
            correctlyPreLoaded = false;
            System.err.println("--Can't load " + getClass().getSimpleName() + " " + dataName);
        }
    }

    protected final void unload() {
        if (!correctlyLoaded)
            return;

        internUnload();
        correctlyPreLoaded = false;
        correctlyLoaded = false;
    }

    protected abstract void internUnload();

    @Override
    public final boolean isPreLoaded() {
        return correctlyPreLoaded;
    }

    @Override
    public final boolean isLoaded() {
        return correctlyLoaded;
    }
}
