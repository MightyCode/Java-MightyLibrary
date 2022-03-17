package MightyLibrary.mightylib.resources;

public abstract class DataType {
    protected final String dataName;
    protected final String path;

    public DataType(String dataName, String path){
        this.dataName = dataName;
        this.path = path;
    }

    public String getDataName(){
        return this.dataName;
    }

    public String getPath(){ return this.path; }

    public final boolean load(ResourceLoader loader) {
        return loader.load(this);
    }

    public boolean reload(ResourceLoader loader){
        if (!unload())
            return false;

        return load(loader);
    }

    public abstract boolean unload();
}
