package io.github.mightycode.mightylib.resources;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
        load(batchName, batchName);
    }

    // The data are loaded after those wanted now, see Resources.preloadBatch
    void preload(String batchName) {
        load(batchName, Resources.PRELOAD_REFERENCE_PREFIX + batchName);
    }

    // The reference of the data is the batch name, or the batch name with the preload prefix
    private void load(String batchName, String reference) {
        resources.runOnMainThread(() -> {
            System.out.println("--" + (reference.equals(batchName) ? "Load" : "Preload") + " batch: " + batchName);
            List<DataType> listed = collectBatch(batchName);
            for (DataType data : listed)
                resources.addReferenceAndUpdate(data, reference);

            loadedBatches.computeIfAbsent(reference, name -> new ArrayList<>()).add(listed);
        });
    }

    void unload(String batchName) {
        release(batchName, batchName);
    }

    void cancelPreload(String batchName) {
        release(batchName, Resources.PRELOAD_REFERENCE_PREFIX + batchName);
    }

    private void release(String batchName, String reference) {
        resources.runOnMainThread(() -> {
            System.out.println("--" + (reference.equals(batchName) ? "Unload" : "Cancel preload of") + " batch: " + batchName);

            // The data referenced by the load, not by the description now (reloaded, new data matching a regex...)
            List<List<DataType>> loads = loadedBatches.get(reference);
            if (loads == null || loads.isEmpty()) {
                System.err.println("--Batch " + batchName + " unloaded but not loaded");
                return;
            }

            List<DataType> listed = loads.remove(loads.size() - 1);
            if (loads.isEmpty())
                loadedBatches.remove(reference);

            for (DataType data : listed)
                resources.removeReferenceAndUpdate(data, reference);
        });
    }

    private static String batchOf(String reference) {
        return reference.startsWith(Resources.PRELOAD_REFERENCE_PREFIX)
                ? reference.substring(Resources.PRELOAD_REFERENCE_PREFIX.length()) : reference;
    }

    /**
     * The description of a loaded batch changed (reload): its loads reference the new list, the data added are loaded,
     * those removed are released. A description that lists an unknown data keeps the previous list.
     */
    // New data may match the patterns of the loaded batches
    void refreshLoaded() {
        for (String reference : new ArrayList<>(loadedBatches.keySet()))
            refresh(reference);
    }

    void descriptionChanged(String batchName) {
        refresh(batchName);
        refresh(Resources.PRELOAD_REFERENCE_PREFIX + batchName);
    }

    private void refresh(String reference) {
        String batchName = batchOf(reference);
        List<List<DataType>> loads = loadedBatches.get(reference);
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
                resources.addReferenceAndUpdate(data, reference);
            for (DataType data : loads.get(i))
                resources.removeReferenceAndUpdate(data, reference);

            loads.set(i, listed);
        }
    }

    /**
     * What loading the batch means now: the data it lists plus the dependencies already known (they are known once
     * a data is read, "dependenciesKnown" says if all are), by type the loaded ones and the estimated memory
     * (DataType.estimatedCpuBytes/estimatedGpuBytes), and the memory still to load.
     */
    JSONObject preview(String batchName) {
        JSONObject result = new JSONObject();
        result.put("batch", batchName);

        List<DataType> listed;
        try {
            listed = collectBatch(batchName);
        } catch (RuntimeException e) {
            result.put("error", e.getMessage());
            return result;
        }

        Set<DataType> all = Collections.newSetFromMap(new IdentityHashMap<>());
        List<DataType> ordered = new ArrayList<>();
        ArrayDeque<DataType> toVisit = new ArrayDeque<>(listed);
        while (!toVisit.isEmpty()) {
            DataType data = toVisit.poll();
            if (!all.add(data))
                continue;

            ordered.add(data);
            toVisit.addAll(data.getDependencies());
        }

        boolean dependenciesKnown = true;
        long cpu = 0, gpu = 0, cpuToLoad = 0, gpuToLoad = 0;
        int loaded = 0;
        Map<String, long[]> byType = new TreeMap<>();
        for (DataType data : ordered) {
            if (!data.isPreLoaded() && !data.isLoaded())
                dependenciesKnown = false;

            long dataCpu = data.estimatedCpuBytes();
            long dataGpu = data.estimatedGpuBytes();
            cpu += dataCpu;
            gpu += dataGpu;
            if (data.isLoaded()) {
                ++loaded;
            } else {
                cpuToLoad += dataCpu;
                gpuToLoad += dataGpu;
            }

            // count, loaded, cpu bytes, gpu bytes
            long[] values = byType.computeIfAbsent(data.getClass().getSimpleName(), name -> new long[4]);
            ++values[0];
            values[1] += data.isLoaded() ? 1 : 0;
            values[2] += dataCpu;
            values[3] += dataGpu;
        }

        JSONObject types = new JSONObject();
        for (Map.Entry<String, long[]> entry : byType.entrySet()) {
            JSONObject type = new JSONObject();
            type.put("count", entry.getValue()[0]);
            type.put("loaded", entry.getValue()[1]);
            type.put("cpuBytes", entry.getValue()[2]);
            type.put("gpuBytes", entry.getValue()[3]);
            types.put(entry.getKey(), type);
        }

        result.put("listed", listed.size());
        result.put("withDependencies", ordered.size());
        result.put("loaded", loaded);
        result.put("dependenciesKnown", dependenciesKnown);
        result.put("cpuBytes", cpu);
        result.put("gpuBytes", gpu);
        result.put("cpuBytesToLoad", cpuToLoad);
        result.put("gpuBytesToLoad", gpuToLoad);
        result.put("types", types);
        return result;
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
