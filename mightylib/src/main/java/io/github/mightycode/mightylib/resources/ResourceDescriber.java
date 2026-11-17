package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.resources.source.ClasspathSource;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.IResourceSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Optional;

/**
 * Main thread: state of the registered data as JSON, for the debug commands.
 */
class ResourceDescriber {
    private static final int DESCRIBE_SELECTION_LIMIT = 100;

    private final Resources resources;

    ResourceDescriber(Resources resources) {
        this.resources = resources;
    }

    // Any thread: the stack is immutable. "message": a summary for the player of the command
    static JSONObject DescribeSources(ResourceSources sources, String path) {
        JSONArray stack = new JSONArray();
        for (IResourceSource source : sources.sources())
            stack.put(DescribeSource(sources, source));

        JSONObject result = new JSONObject().put("sources", stack);
        StringBuilder message = new StringBuilder();
        if (path == null) {
            message.append(sources.sources().size()).append(" sources, from the lowest to the highest:");
            for (IResourceSource source : sources.sources())
                message.append(' ').append(source.name());

            return result.put("message", message.toString());
        }

        String logical = ResourceSources.normalize(path);
        JSONArray layers = new JSONArray();
        for (IResourceSource layer : sources.layers(logical))
            layers.put(DescribeSource(sources, layer).put("size", layer.size(logical)));

        result.put("path", logical).put("layers", layers);
        Optional<IResourceSource> served = sources.resolve(logical);
        if (served.isPresent()) {
            result.put("servedBy", served.get().name());
            message.append(logical).append(": served by ").append(served.get().name());
            if (layers.length() > 1)
                message.append(" (in ").append(layers.length()).append(" sources)");
        } else {
            message.append(logical).append(": in no source");
        }

        return result.put("message", message.toString());
    }

    private static JSONObject DescribeSource(ResourceSources sources, IResourceSource source) {
        String owner = sources.owner(source);
        String kind;
        if (Identifier.ENGINE_NAMESPACE.equals(owner))
            kind = "engine";
        else if (ResourceSources.GAME_OWNER.equals(owner))
            kind = "game";
        else
            kind = "plugin";

        String location;
        if (source instanceof FolderSource)
            location = "disk";
        else if (source instanceof ClasspathSource && !((ClasspathSource) source).isPresent())
            location = "classpath, prefix absent";
        else if (source instanceof ClasspathSource)
            location = source.localFile("").isPresent() ? "classpath folder" : "classpath jar";
        else
            location = source.getClass().getSimpleName();

        return new JSONObject()
                .put("name", source.name())
                .put("owner", owner)
                .put("kind", kind)
                .put("location", location)
                // Only the sources on the disk are watched (hot reload)
                .put("watchable", source.localFile("").isPresent());
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
                boolean isReferenced = data.isReferenced();
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
            if (data instanceof MultiSourceDataType)
                item.put("sourceFiles", describeSources((MultiSourceDataType) data));
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

    // Each file of the data and whether its last reading read it
    private static JSONArray describeSources(MultiSourceDataType data) {
        JSONArray files = new JSONArray();
        for (int i = 0; i < data.sourceCount(); ++i) {
            JSONObject file = new JSONObject();
            file.put("path", String.valueOf(data.sourcePath(i)));
            file.put("read", data.isSourceRead(i));
            files.put(file);
        }

        return files;
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
