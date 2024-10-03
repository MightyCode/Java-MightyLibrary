package MightyLibrary.mightylib.resources.data;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.Resources;
import org.json.JSONObject;

import java.util.Map;

public class JSONLoader extends ResourceLoader {
    @Override
    public Class<? extends DataType> getType() {
        return JSONFile.class;
    }

    @Override
    public String getResourceNameType() {
        return "JSONFile";
    }

    @Override
    public void create(Map<String, DataType> data){ exploreResourcesFile(data, Resources.FOLDER); }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new JSONFile(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending.equals(".json"))
            return getFileName(path);

        return null;
    }

    @Override
    public void load(DataType dataType) {
        if (!(dataType instanceof JSONFile))
            return;

        JSONFile json = (JSONFile) dataType;

        json.init(new JSONObject(FileMethods.readFileAsString(json.path())));
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        JSONFile file = new JSONFile(resourceName, resourcePath);
        load(file);
        data.put(resourceName, file);
    }
}
