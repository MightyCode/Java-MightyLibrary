package MightyLibrary.mightylib.resources.models;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;

import java.util.Map;

public class ObjModelLoader extends ResourceLoader {
    @Override
    public Class<?> getType() {
        return Model.class;
    }

    @Override
    public String getResourceNameType() {
        return "model";
    }

    @Override
    public void create(Map<String, DataType> data) {

    }

    @Override
    public void load(DataType dataType) {

    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {

    }
}
