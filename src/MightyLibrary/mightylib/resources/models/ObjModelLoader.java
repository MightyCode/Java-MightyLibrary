package MightyLibrary.mightylib.resources.models;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.sound.SoundData;

import java.util.Map;

public class ObjModelLoader extends ResourceLoader {
    @Override
    public Class<?> getType() {
        return Model.class;
    }

    @Override
    public String getResourceNameType() {
        return "Model";
    }

    @Override
    public void create(Map<String, DataType> data) {
        exploreResourcesFile(data, "resources");
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new Model(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending.equals(".obj"))
            return getFileName(path);

        return null;
    }

    @Override
    public void load(DataType dataType) {
        if (!(dataType instanceof Model))
            return;

        Model model = (Model) dataType;
        System.out.println(model.getPath());
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        Model model = new Model(resourceName, resourcePath);
        data.put(resourceName, model);

        load(model);
    }
}
