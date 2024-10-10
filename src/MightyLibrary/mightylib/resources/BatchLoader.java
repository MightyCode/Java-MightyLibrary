package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.resources.data.JSONLoader;

import java.util.Map;

public class BatchLoader extends ResourceLoader {
    private final JSONLoader jsonLoader;

    public BatchLoader(JSONLoader jsonLoader){
        this.jsonLoader = jsonLoader;
    }

    @Override
    public Class<? extends DataType> getType() {
        return BatchResources.class;
    }

    @Override
    public String getResourceNameType() {
        return "Batch";
    }

    @Override
    public void create(Map<String, DataType> data) {
        exploreResourcesFile(data, Resources.FOLDER + "batchs", new String[]{});
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new BatchResources(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending != null && ending.equals(".batch"))
            return getFileName(path);

        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof BatchResources))
            return;

        jsonLoader.initWithFile(dataType);
    }
}
