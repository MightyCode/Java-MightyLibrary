package MightyLibrary.mightylib.resources.models;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.sound.SoundData;

import java.util.Map;

public class ObjModelLoader extends ResourceLoader {
    @Override
    public Class<? extends DataType> getType() {
        return Model.class;
    }

    @Override
    public String getResourceNameType() {
        return "Model";
    }

    @Override
    public void create(Map<String, DataType> data) {
        exploreResourcesFile(data, Resources.FOLDER);
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new Model(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending != null && ending.equals(".obj"))
            return getFileName(path);

        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof Model))
            return;

        Model model = (Model) dataType;
        System.out.println(model.path());
    }
}
