package MightyLibrary.mightylib.resources;

public abstract class DataType {
    protected final String dataName;
    protected final String path;
    protected boolean correctlyLoaded;

    public DataType(String dataName, String path){
        this.dataName = dataName;
        this.path = path;
        this.correctlyLoaded = false;
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

            if (correctlyLoaded) {
                return;
            }
        }

        load(loader);
    }

    public final boolean isCorrectlyLoaded() {
        return correctlyLoaded;
    }

    public abstract void unload();
}
