package MightyLibrary.mightylib.resources.data;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class JsonLoader extends ResourceLoader {
    @Override
    public Class<?> getType() {
        return JSONFile.class;
    }

    @Override
    public String getResourceNameType() {
        return "JSONFile";
    }

    @Override
    public void create(Map<String, DataType> data){
        create(data, "resources/");
    }

    private void create(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            String ending = path.substring(path.lastIndexOf("."));

            if (ending.equals(".json"))
                data.put(name, new JSONFile(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                create(data, path + "/" + childPath);
            }
        }
    }

    @Override
    public void load(DataType dataType) {
        if (!(dataType instanceof JSONFile))
            return;

        JSONFile json = (JSONFile) dataType;

        json.init(new JSONObject(FileMethods.readFileAsString(dataType.getPath())));
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        JSONFile file = new JSONFile(resourceName, resourcePath);
        load(file);
        data.put(resourceName, file);
    }
}
