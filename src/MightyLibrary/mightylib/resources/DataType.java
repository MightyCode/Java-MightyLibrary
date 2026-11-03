package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.utils.LoadingElement;
import MightyLibrary.mightylib.utils.math.UUID;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class DataType extends UUID implements LoadingElement {
    protected final Resources resources;

    protected final String dataName;

    // Volatile: states are written by worker threads and read by the main thread
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

        references = new ArrayList<>();
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

    protected final void load() {
        if (!correctlyPreLoaded)
            throw new RuntimeException("Data " + dataName + " is not preloaded");

        if (internLoad())
            correctlyLoaded = true;
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
