package MightyLibrary.mightylib.resources;

public abstract class DataType {
    protected EDataType type;
    protected String dataName;
    protected String path;

    public DataType(EDataType type, String dataName, String path){
        this.type = type;
        this.dataName = dataName;
        this.path = path;
    }


    public EDataType getType(){
        return this.type;
    }

    public String getDataName(){
        return this.dataName;
    }

    public String getPath(){ return this.path; }

    public abstract boolean load();

    public boolean reload(){
        if (!unload()) return false;

        return load();
    }

    public abstract boolean unload();
}
