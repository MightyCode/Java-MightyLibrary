package MightyLibrary.mightylib.resources;

import java.util.Map;

public abstract class ResourceLoader {
    private Class<?> type = null;

    public ResourceLoader(Class<?> type){
        this.type = type;
    }

    public Class<?> getType() { return type; }

    public abstract void load(Map<String, DataType> data);
}
