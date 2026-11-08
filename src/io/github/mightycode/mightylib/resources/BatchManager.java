package io.github.mightycode.mightylib.resources;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and unloads the groups of data listed by the batch descriptions (BatchResources).
 * Main thread only (the calls from other threads are posted by Resources.runOnMainThread).
 */
class BatchManager {
    private interface BatchAction {
        void apply(DataType data);
    }

    private final Resources resources;
    // For each load of a batch (loaded once per load), the data it referenced, released by unload
    private final Map<String, List<List<DataType>>> loadedBatches = new HashMap<>();

    BatchManager(Resources resources) {
        this.resources = resources;
    }

    void load(String batchName) {
        resources.runOnMainThread(() -> {
            System.out.println("--Load batch: " + batchName);
            List<DataType> listed = collectBatch(batchName);
            for (DataType data : listed)
                resources.addReferenceAndUpdate(data, batchName);

            loadedBatches.computeIfAbsent(batchName, name -> new ArrayList<>()).add(listed);
        });
    }

    void unload(String batchName) {
        resources.runOnMainThread(() -> {
            System.out.println("--Unload batch: " + batchName);

            // The data referenced by the load, not by the description now (reloaded, new data matching a regex...)
            List<List<DataType>> loads = loadedBatches.get(batchName);
            if (loads == null || loads.isEmpty()) {
                System.err.println("--Batch " + batchName + " unloaded but not loaded");
                return;
            }

            List<DataType> listed = loads.remove(loads.size() - 1);
            if (loads.isEmpty())
                loadedBatches.remove(batchName);

            for (DataType data : listed)
                resources.removeReferenceAndUpdate(data, batchName);
        });
    }

    /**
     * The description of a loaded batch changed (reload): its loads reference the new list, the data added are loaded,
     * those removed are released. A description that lists an unknown data keeps the previous list.
     */
    // New data may match the patterns of the loaded batches
    void refreshLoaded() {
        for (String batchName : new ArrayList<>(loadedBatches.keySet()))
            descriptionChanged(batchName);
    }

    void descriptionChanged(String batchName) {
        List<List<DataType>> loads = loadedBatches.get(batchName);
        if (loads == null)
            return;

        List<DataType> listed;
        try {
            listed = collectBatch(batchName);
        } catch (RuntimeException e) {
            System.err.println("--Batch " + batchName + " kept as loaded: " + e.getMessage());
            return;
        }

        for (int i = 0; i < loads.size(); ++i) {
            // Added before releasing: a data in both lists is never unloaded meanwhile
            for (DataType data : listed)
                resources.addReferenceAndUpdate(data, batchName);
            for (DataType data : loads.get(i))
                resources.removeReferenceAndUpdate(data, batchName);

            loads.set(i, listed);
        }
    }

    // Data listed by the batch description now
    private List<DataType> collectBatch(String batchName) {
        List<DataType> listed = new ArrayList<>();
        forEachInBatch(batchName, listed::add);
        return listed;
    }

    // Calls the action on every data listed by the batch, by name or by regular expression
    private void forEachInBatch(String batchName, BatchAction action) {
        BatchResources batchResources = resources.getResource(BatchResources.class, batchName);
        if (batchResources == null || batchResources.getObject() == null)
            throw new RuntimeException("Batch not loaded: " + batchName);

        JSONArray content = batchResources.getObject().getJSONArray("batchs");
        for (int j = 0; j < content.length(); ++j) {
            JSONObject resource = content.getJSONObject(j);

            Class<? extends DataType> type = getClassFromBatchName(batchName, resource.getString("type"));
            Map<String, DataType> ofType = resources.registry().get(type);

            JSONArray resourceFiles = resource.getJSONArray("files");
            for (int i = 0; i < resourceFiles.length(); ++i) {
                String resourceName = resourceFiles.getString(i);
                DataType dataType = ofType.get(resourceName);

                if (dataType == null)
                    throw new RuntimeException("Can't find resource: " + resourceName + " of type: " + type.getName());

                action.apply(dataType);
            }

            JSONArray regularExpressions = resource.getJSONArray("regex");
            for (int i = 0; i < regularExpressions.length(); ++i) {
                Pattern pattern = Pattern.compile(regularExpressions.getString(i));

                for (Map.Entry<String, DataType> entry : ofType.entrySet()) {
                    Matcher matcher = pattern.matcher(entry.getKey());
                    if (matcher.matches())
                        action.apply(entry.getValue());
                }
            }
        }
    }

    private static Class<? extends DataType> getClassFromBatchName(String batchName, String name) {
        Class<? extends DataType> type = Resources.getClassFromName(name);
        if (type == null)
            throw new RuntimeException("Unknown resource type: " + name + " in batch: " + batchName);

        return type;
    }
}
