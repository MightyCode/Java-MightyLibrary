package MightyLibrary.mightylib.resources;

public abstract class SingleSourceDataType extends DataType {
    protected final String path;
    public SingleSourceDataType(String dataName, String path) {
        super(dataName);

        this.path = path;
    }

    public final boolean validPath(){
        return !(path == null || path.equals(""));
    }
    public final String path(){ return this.path; }
}
