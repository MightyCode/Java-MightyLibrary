package MightyLibrary.mightylib.resources;

import java.util.Map;

public abstract class ResourceLoader {

    public ResourceLoader(){ }

    public abstract Class<?> getType();

    public abstract String getResourceNameType();

    public abstract void create(Map<String, DataType> data);

    public abstract void load(DataType dataType);

    public abstract void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath);
}
