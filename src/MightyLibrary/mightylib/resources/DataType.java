package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.utils.LoadingElement;
import MightyLibrary.mightylib.utils.math.UUID;

import java.util.ArrayList;

public abstract class DataType extends UUID implements LoadingElement {
    protected final Resources resources;

    protected final String dataName;

    // Step where data are read from disk and dependencies are listed
    private boolean correctlyPreLoaded;

    // When dependencies are loaded, this data can be loaded and well computed
    private boolean correctlyLoaded;

    // Static table, max of 5 references
    public int MAX_REFERENCES = 5;
    protected String[] references;

    private final ArrayList<DataType> dependencies;

    private final TYPE_SET_UP typeSetUp;

    private boolean manuallyCreated;

    public enum TYPE_SET_UP {
        IMMEDIATELY_BY_MAIN_CONTEXT,
        MAIN_CONTEXT,
        THREAD_CONTEXT
    }

    public DataType(TYPE_SET_UP typeSetUp, String dataName){
        super();
        resources = Resources.getInstance();

        this.dataName = dataName;
        this.correctlyLoaded = false;
        this.typeSetUp = typeSetUp;

        references = new String[5];
        dependencies = new ArrayList<>();
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
        if (references[MAX_REFERENCES - 1] != null) {
            System.err.println("Reference table is full for resources" + dataName);
            return;
        }
        for (int i = 0; i < references.length; i++){
            if (references[i] == null){
                references[i] = reference;
                return;
            }
        }
    }

    // Remove and shift
    public final void removeReference(String reference){
        for (int i = 0; i < references.length; i++){
            if (references[i] != null && references[i].equals(reference)){
                references[i] = null;
                for (int j = i; j < references.length - 1; j++){
                    references[j] = references[j + 1];
                }
                references[references.length - 1] = null;
                return;
            }
        }
    }

    public final boolean notReferenced(){
        return references[0] == null;
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
