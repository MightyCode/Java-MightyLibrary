package io.github.mightycode.mightylib.resources.data;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.Resources;

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

        if (ending != null && ending.equals(".json")) return getFileName(path);

        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof JSONFile))
            return;

        JSONFile json = (JSONFile) dataType;
        json.setJsonObject(FileMethods.readJSONObject(json.path()));
    }
}
