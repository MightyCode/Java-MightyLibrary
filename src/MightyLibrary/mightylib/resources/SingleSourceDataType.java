package MightyLibrary.mightylib.resources;

public abstract class SingleSourceDataType extends DataType {
    protected final String path;
    public SingleSourceDataType(TYPE_SET_UP typeSetUp, String dataName, String path) {
        super(typeSetUp, dataName);

        this.path = path;
    }

    public final boolean validPath() {
        return !(path == null || path.isEmpty());
    }

    public final String path(){ return this.path; }
}
