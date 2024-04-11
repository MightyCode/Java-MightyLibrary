package MightyLibrary.mightylib.resources;

public abstract class DataType {
    protected final String dataName;
    protected final String path;
    protected boolean correctlyLoaded;

    // Static table, max of 5 references
    public int MAX_REFERENCES = 5;
    protected String[] references;

    public DataType(String dataName, String path){
        this.dataName = dataName;
        this.path = path;
        this.correctlyLoaded = false;

        references = new String[5];
    }

    public final void addReference(String reference) {
        if (references[MAX_REFERENCES - 1] != null){
            System.err.println("Reference table is full for resources" + dataName + " of path " + path);
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

    public final boolean isReferenced(){
        return references[0] != null;
    }

    public final String getDataName(){
        return this.dataName;
    }

    public final String getPath(){ return this.path; }

    public final void load(ResourceLoader loader) {
        loader.load(this);
    }

    public void reload(ResourceLoader loader){
        if (correctlyLoaded) {
            unload();

            if (correctlyLoaded)
                return;
        }

        load(loader);
    }

    public final boolean isCorrectlyLoaded() {
        return correctlyLoaded;
    }

    public abstract void unload();
}
