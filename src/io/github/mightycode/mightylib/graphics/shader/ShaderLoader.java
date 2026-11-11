package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The shaders listed in shaders.json of the GLSL version (ShaderManager). Their sources are read and preprocessed by
 * the workers (ShaderPreprocessor: #include searched next to the including file, then in the version folder).
 */
public class ShaderLoader extends ResourceLoader {
    private final ShaderManager shaderManager;

    @Override
    public Class<? extends DataType> getType() {
        return ShaderData.class;
    }

    public ShaderLoader(ShaderManager shaderManager) {
        this.shaderManager = shaderManager;
    }

    @Override
    public String getResourceNameType() {
        return "ShaderData";
    }

    @Override
    public DataType createFallback() {
        return new FallbackShaderData(shaderManager.getVersion());
    }

    @Override
    public void create(Map<String, DataType> data) {
        JSONObject shadersList = shaderManager.getShadersInfo().getJSONObject("shaders");

        Iterator<String> arrayShader = shadersList.keys();
        while (arrayShader.hasNext()) {
            // Name of the shader key (used by renderer)
            String currentShader = arrayShader.next();

            JSONObject JShader = shadersList.getJSONObject(currentShader);

            // From the working directory: the files are watched for hot reload
            JSONArray files = JShader.getJSONArray("files");
            String[] sArrayFiles = new String[files.length()];
            for (int i = 0; i < files.length(); ++i) {
                sArrayFiles[i] = shaderManager.versionFolder() + files.getString(i);
            }

            JSONArray tags = JShader.getJSONArray("tags");

            ShaderData shader = new ShaderData(
                    currentShader,
                    isTagIn(tags, "2D") || isTagIn(tags, "none"), sArrayFiles);

            data.put(currentShader, shader);
        }
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

        // Worker thread: files and preprocessing only, the compilation is made by its GL element (Shader).
        // A missing file or include makes the preload fail: the fallback replaces the shader. The files read, and
        // those missing, are watched either way: changing or creating one reads the shader again
        Set<String> watched = new LinkedHashSet<>();
        try {
            String vertex = preprocess(shader.getSourcePath(0), watched);
            shader.checkCancelled();
            String fragment = preprocess(shader.getSourcePath(1), watched);

            String geometry = null;
            if (shader.usesGeometryShader()) {
                shader.checkCancelled();
                geometry = preprocess(shader.getSourcePath(2), watched);
            }

            shader.setSources(vertex, fragment, geometry);
        } finally {
            shader.setSourcePaths(watched);
        }
    }

    private String preprocess(String path, Set<String> watched) {
        // GLSL 140: the line after "#line n" is n + 1 (n from GLSL 330)
        int lineOffset = "1_40".equals(shaderManager.getVersion()) ? -1 : 0;
        ShaderPreprocessor preprocessor = new ShaderPreprocessor(ShaderLoader::readIfFile,
                shaderManager.versionFolder(), lineOffset);
        try {
            return preprocessor.process(path);
        } finally {
            watched.addAll(preprocessor.watchedFiles());
        }
    }

    private static String readIfFile(String path) {
        return new File(path).isFile() ? FileMethods.readFileAsString(path) : null;
    }
}
