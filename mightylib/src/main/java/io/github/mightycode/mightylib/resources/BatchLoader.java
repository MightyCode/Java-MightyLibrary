package io.github.mightycode.mightylib.resources;

import org.apiguardian.api.API;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apiguardian.api.API.Status.INTERNAL;

// The batches (resources/batchs/*.batch, JSON): reads its files itself, whatever loader the JSON files have
@API(status = INTERNAL)
public final class BatchLoader extends ResourceLoader {
    // Keys of a description (read by BatchManager)
    private static final String LIST_KEY = "batchs";
    private static final String TYPE_KEY = "type";
    private static final String GPU_KEY = "gpu";
    private static final String FILES_KEY = "files";
    private static final String REGEX_KEY = "regex";

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
        exploreResourcesFile(data, "batchs", List.of());
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
        batch.setJsonObject(Merge(FileMethods.readJSONObjectLayers(resources().sources(), batch.path())));
    }

    /**
     * The descriptions of the same batch in several sources, from the lowest to the highest: their lists joined, by
     * type of data and "gpu" flag, without duplicate. A higher source adds to a batch, it can't remove from it.
     */
    static JSONObject Merge(List<JSONObject> layers) {
        if (layers.size() == 1)
            return layers.get(0);

        // By type and flag: the names and the regular expressions, in their order of appearance
        Map<String, Set<String>> files = new LinkedHashMap<>();
        Map<String, Set<String>> regex = new LinkedHashMap<>();
        Map<String, JSONObject> byType = new LinkedHashMap<>();
        for (JSONObject layer : layers) {
            JSONArray entries = layer.optJSONArray(LIST_KEY);
            if (entries == null)
                continue;

            for (int i = 0; i < entries.length(); ++i) {
                JSONObject entry = entries.getJSONObject(i);
                String type = entry.getString(TYPE_KEY);
                boolean gpu = entry.optBoolean(GPU_KEY, true);
                String key = type + "|" + gpu;
                byType.computeIfAbsent(key, k -> new JSONObject().put(TYPE_KEY, type).put(GPU_KEY, gpu));
                addAll(files.computeIfAbsent(key, k -> new LinkedHashSet<>()), entry.optJSONArray(FILES_KEY));
                addAll(regex.computeIfAbsent(key, k -> new LinkedHashSet<>()), entry.optJSONArray(REGEX_KEY));
            }
        }

        JSONArray merged = new JSONArray();
        for (Map.Entry<String, JSONObject> entry : byType.entrySet()) {
            merged.put(entry.getValue()
                    .put(FILES_KEY, new JSONArray(files.get(entry.getKey())))
                    .put(REGEX_KEY, new JSONArray(regex.get(entry.getKey()))));
        }

        return new JSONObject().put(LIST_KEY, merged);
    }

    private static void addAll(Set<String> into, JSONArray values) {
        if (values == null)
            return;

        for (int i = 0; i < values.length(); ++i)
            into.add(values.getString(i));
    }
}
