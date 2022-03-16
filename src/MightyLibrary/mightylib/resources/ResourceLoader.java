package MightyLibrary.mightylib.resources;

import java.util.Map;

public abstract class ResourceLoader {
    private Class<?> type;

    public ResourceLoader(Class<?> type){
        this.type = type;
    }

    public Class<?> getType() { return type; }

    public abstract void create(Map<String, DataType> data);

    public abstract boolean load(DataType dataType);
}
