package io.github.mightycode.mightylib.resources;

import java.util.Map;

// The batches (resources/batchs/*.batch, JSON): reads its files itself, whatever loader the JSON files have
public class BatchLoader extends ResourceLoader {
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
        exploreResourcesFile(data, resources().folder() + "batchs", new String[]{});
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

        BatchResources batch = (BatchResources) dataType;
        batch.setJsonObject(FileMethods.readJSONObject(batch.path()));
    }
}
