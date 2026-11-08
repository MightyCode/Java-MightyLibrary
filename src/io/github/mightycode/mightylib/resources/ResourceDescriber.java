package io.github.mightycode.mightylib.resources;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * Main thread: state of the registered data as JSON, for the debug commands.
 */
class ResourceDescriber {
    private static final int DESCRIBE_SELECTION_LIMIT = 100;

    private final Resources resources;

    ResourceDescriber(Resources resources) {
        this.resources = resources;
    }

    /**
     * By type, the number of loaded, preloaded and referenced data, and the names of the data whose
     * state does not match their references (loaded but not referenced = leak, referenced but not loaded = still loading
     * or failed); then the state of the pipeline.
     */
    JSONObject describe() {
        JSONObject types = new JSONObject();
        long totalCpu = 0, totalGpu = 0;
        for (Map.Entry<Class<? extends DataType>, Map<String, DataType>> ofType : resources.registry().entrySet()) {
            int loaded = 0, preloaded = 0, referenced = 0;
            long cpuBytes = 0, gpuBytes = 0;
            JSONArray loadedNotReferenced = new JSONArray();
            JSONArray referencedNotLoaded = new JSONArray();
            JSONArray onlyAsDependency = new JSONArray();
            JSONArray usingFallback = new JSONArray();
            JSONArray reloading = new JSONArray();

            for (DataType data : ofType.getValue().values()) {
                boolean isReferenced = !data.notReferenced();
                if (data.reloadCopy != null)
                    reloading.put(data.getDataName());
                if (data.isLoaded()) {
                    ++loaded;
                    cpuBytes += data.estimatedCpuBytes();
                    gpuBytes += data.estimatedGpuBytes();
                }
                if (data.isPreLoaded()) ++preloaded;
                if (isReferenced) ++referenced;
                if (isReferenced && onlyReferencedAsDependency(data))
                    onlyAsDependency.put(data.getDataName());
                if (isReferenced && data.failed && resources.hasFallback(data))
                    usingFallback.put(data.getDataName());

                if (data.isLoaded() && !isReferenced)
                    loadedNotReferenced.put(data.getDataName());
                else if (!data.isLoaded() && isReferenced)
                    referencedNotLoaded.put(data.getDataName() + (data.failed ? " (failed)" : ""));
            }

            if (loaded == 0 && referenced == 0)
                continue;

            JSONObject type = new JSONObject();
            type.put("loaded", loaded);
            type.put("preloaded", preloaded);
            type.put("referenced", referenced);
            // Estimates (DataType.estimatedCpuBytes/estimatedGpuBytes) of the loaded data
            type.put("cpuBytes", cpuBytes);
            type.put("gpuBytes", gpuBytes);
            totalCpu += cpuBytes;
            totalGpu += gpuBytes;
            if (!loadedNotReferenced.isEmpty())
                type.put("loadedNotReferenced", loadedNotReferenced);
            if (!referencedNotLoaded.isEmpty())
                type.put("referencedNotLoaded", referencedNotLoaded);
            // Wanted only because another data depends on it (not listed by a batch nor added by the code)
            if (!onlyAsDependency.isEmpty())
                type.put("onlyAsDependency", onlyAsDependency);
            // Failed, replaced by the fallback of the type until reloaded
            if (!usingFallback.isEmpty())
                type.put("usingFallback", usingFallback);
            // Loaded and reading their new version apart
            if (!reloading.isEmpty())
                type.put("reloading", reloading);

            types.put(ofType.getKey().getSimpleName(), type);
        }

        JSONObject result = new JSONObject();
        result.put("types", types);
        result.put("totalCpuBytes", totalCpu);
        result.put("totalGpuBytes", totalGpu);
        result.put("pipeline", resources.describePipeline());
        return result;
    }

    /**
     * The detail of each data matching the selection (state, references, version, sources),
     * at most DESCRIBE_SELECTION_LIMIT of them.
     */
    JSONObject describe(ResourceSelection selection) {
        JSONArray list = new JSONArray();
        int[] count = {0};
        resources.forEachRegistered((type, data) -> {
            if (!selection.matches(type, data))
                return;
            if (++count[0] > DESCRIBE_SELECTION_LIMIT)
                return;

            JSONObject item = new JSONObject();
            item.put("type", type.getSimpleName());
            item.put("name", data.getDataName());
            item.put("state", data.isLoaded() ? "loaded" : data.failed ? "failed"
                    : data.isPreLoaded() ? "preloaded" : "unloaded");
            item.put("version", data.getVersion());
            JSONArray references;
            synchronized (data.references) {
                references = new JSONArray(data.references);
            }
            item.put("references", references);
            if (data.reloadCopy != null)
                item.put("reloading", true);
            if (resources.isFallback(data))
                item.put("fallback", true);
            if (data.failed && resources.hasFallback(data))
                item.put("usingFallback", true);
            if (!data.sourcePaths().isEmpty())
                item.put("sources", new JSONArray(data.sourcePaths()));
            list.put(item);
        });

        JSONObject result = new JSONObject();
        result.put("selection", selection.toString());
        result.put("count", count[0]);
        if (count[0] > DESCRIBE_SELECTION_LIMIT)
            result.put("truncatedTo", DESCRIBE_SELECTION_LIMIT);
        result.put("data", list);
        return result;
    }

    private static boolean onlyReferencedAsDependency(DataType data) {
        synchronized (data.references) {
            for (String reference : data.references) {
                if (!reference.startsWith(Resources.DEPENDENCY_REFERENCE_PREFIX))
                    return false;
            }
        }

        return true;
    }
}
