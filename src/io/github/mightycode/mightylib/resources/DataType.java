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
    private final List<DataType> dependencies;

    private final TYPE_SET_UP typeSetUp;

    private boolean manuallyCreated;

    // Pipeline state, main thread only (see Resources)
    boolean jobRunning;
    boolean failed;
    boolean reloadWanted;
    boolean deleteWanted;
    boolean waitingDependencies;
    // Added by addPreLoadedResource: no file to read again on reload
    boolean preloadedByCode;
    // Data waiting for this one to be loaded
    final List<DataType> waiters = new ArrayList<>();

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

    public final void addDependency(DataType dependency){
        dependencies.add(dependency);
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

    final void loadDone(boolean success) {
        if (success) {
            correctlyLoaded = true;
        } else {
            correctlyPreLoaded = false;
            System.err.println("--Can't load " + getClass().getSimpleName() + " " + dataName);
        }
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
