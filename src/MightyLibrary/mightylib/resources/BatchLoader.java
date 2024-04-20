package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.resources.data.JSONLoader;

import java.util.Map;

public class BatchLoader extends ResourceLoader {
    private final JSONLoader jsonLoader;

    public BatchLoader(JSONLoader jsonLoader){
        this.jsonLoader = jsonLoader;
    }

    @Override
    public Class<?> getType() {
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
    public void load(DataType dataType) {
        if (!(dataType instanceof BatchResources))
            return;

        jsonLoader.load(dataType);
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        BatchResources batchResources = new BatchResources(resourceName, resourcePath);
        data.put(resourceName, batchResources);

        load(batchResources);
    }
}
