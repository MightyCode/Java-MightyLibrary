package io.github.mightycode.mightylib.resources;

/**
 * Data read from one file (path, may be empty for a data made by the code). Its hooks are the ones of DataType.
 */
public abstract class SingleSourceDataType extends DataType {
    private final String path;
    public SingleSourceDataType(TYPE_SET_UP typeSetUp, String dataName, String path) {
        super(typeSetUp, dataName);

        this.path = path;
    }

    public final boolean validPath() {
        return !(path == null || path.isEmpty());
    }

    public final String path(){ return this.path; }

    @Override
    public final java.util.List<String> sourcePaths() {
        return validPath() ? java.util.Collections.singletonList(path) : java.util.Collections.emptyList();
    }

    @Override
    public final boolean hasSource() {
        return validPath();
    }
}
