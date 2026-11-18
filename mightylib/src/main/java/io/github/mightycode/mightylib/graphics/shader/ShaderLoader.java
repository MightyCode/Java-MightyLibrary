package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * The shaders declared by the shader service (IShaderService.declarations). Their sources are read and preprocessed
 * by the workers (ShaderPreprocessor: prelude of the service, #include searched next to the including file, then in
 * the folder of the shaders).
 */
public final class ShaderLoader extends ResourceLoader {
    // Read when the loader works: the service is created after the registration of the plugins
    private final Supplier<IShaderService> shaderService;

    @Override
    public Class<? extends DataType> getType() {
        return ShaderData.class;
    }

    public ShaderLoader(IShaderService shaderService) {
        this(() -> shaderService);
    }

    // The plugin of the engine registers it before the services exist (Engine.shaders)
    public ShaderLoader(Supplier<IShaderService> shaderService) {
        this.shaderService = Objects.requireNonNull(shaderService, "shaderService");
    }

    private IShaderService service() {
        IShaderService service = shaderService.get();
        if (service == null)
            throw new IllegalStateException("The loader of the shaders is used before the shader service exists");

        return service;
    }

    @Override
    public String getResourceNameType() {
        return "ShaderData";
    }

    @Override
    public DataType createFallback() {
        return new FallbackShaderData();
    }

    @Override
    public void create(Map<String, DataType> data) {
        IShaderService service = service();
        JSONObject shadersList = service.declarations();

        Iterator<String> arrayShader = shadersList.keys();
        while (arrayShader.hasNext()) {
            // Name of the shader key (used by renderer)
            String currentShader = arrayShader.next();

            JSONObject JShader = shadersList.getJSONObject(currentShader);

            // From the working directory: the files are watched for hot reload
            JSONArray files = JShader.getJSONArray("files");
            String[] sArrayFiles = new String[files.length()];
            for (int i = 0; i < files.length(); ++i) {
                sArrayFiles[i] = service.folder() + files.getString(i);
            }

            JSONArray tags = JShader.getJSONArray("tags");

            ShaderData shader = new ShaderData(
                    currentShader,
                    isTagIn(tags, "2D") || isTagIn(tags, "none"), entryDefines(JShader), sArrayFiles);

            data.put(currentShader, shader);
        }
    }

    // "defines": {"NAME": "value"} of an entry, "" for a define without value; checked when the shader is read
    static Map<String, String> entryDefines(JSONObject entry) {
        JSONObject defines = entry.optJSONObject("defines");
        if (defines == null)
            return Collections.emptyMap();

        Map<String, String> result = new TreeMap<>();
        for (String name : defines.keySet()) {
            Object value = defines.get(name);
            // A number or a boolean is kept as written
            result.put(name, value instanceof String ? (String) value : String.valueOf(value));
        }

        return result;
    }

    private boolean isTagIn(JSONArray array, String tag){
        for(int i = 0; i < array.length(); ++i){
            if(array.getString(i).equals(tag)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {}

    @Override
    public String filterFile(String path) { return null; }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof ShaderData) || !dataType.hasSource())
            return;

        ShaderData shader = (ShaderData) dataType;

        IShaderService service = service();

        // An invalid define of the entry makes the shader fail (fallback); its files stay watched
        for (Map.Entry<String, String> define : shader.getDefines().entrySet()) {
            try {
                service.checkDefine(define.getKey(), define.getValue());
            } catch (IllegalArgumentException e) {
                throw new ResourceException("shader " + shader.getDataName() + ": " + e.getMessage());
            }
        }

        // Once per reading: the same global defines for every stage, even if they change meanwhile
        String prelude = service.prelude(shader.getDefines());

        // Worker thread: files and preprocessing only, the compilation is made by its GL element (Shader).
        // A missing file or include makes the preload fail: the fallback replaces the shader. The files read, and
        // those missing, are watched either way: changing or creating one reads the shader again
        Set<String> watched = new LinkedHashSet<>();
        List<List<String>> stageFiles = new ArrayList<>();
        try {
            String vertex = preprocess(service, prelude, shader.getSourcePath(0), watched, stageFiles);
            shader.checkCancelled();
            String fragment = preprocess(service, prelude, shader.getSourcePath(1), watched, stageFiles);

            String geometry = null;
            if (shader.usesGeometryShader()) {
                shader.checkCancelled();
                geometry = preprocess(service, prelude, shader.getSourcePath(2), watched, stageFiles);
            }

            shader.setSources(vertex, fragment, geometry);
            shader.setStageFiles(stageFiles);
        } finally {
            shader.setSourcePaths(watched);
        }
    }

    private String preprocess(IShaderService service, String prelude, String path, Set<String> watched,
                              List<List<String>> stageFiles) {
        ShaderPreprocessor preprocessor = new ShaderPreprocessor(this::readIfFile, service.folder(), prelude);
        try {
            String source = preprocessor.process(path);
            stageFiles.add(List.copyOf(preprocessor.files()));
            return source;
        } finally {
            watched.addAll(preprocessor.watchedFiles());
        }
    }

    private String readIfFile(String path) {
        return ReadIfFile(resources().sources(), path);
    }

    // The file served by the highest source that has it, null if none (a missing include is searched elsewhere)
    static String ReadIfFile(ResourceSources sources, String path) {
        try {
            if (sources.size(path) < 0)
                return null;
        } catch (IllegalArgumentException e) {
            // Absolute, or out of the sources by its ".."
            return null;
        }

        return FileMethods.readString(sources, path);
    }
}
