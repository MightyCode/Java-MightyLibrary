package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;

public class ShaderLoader extends ResourceLoader {

    /*
     * Shader library
     * The shader library is a map (read by the workers) that contains the shader utility file name as key and the shader content as value.
     */
    private final Map<String, String> shaderLibrary;

    private final ShaderManager shaderManager;

    @Override
    public Class<? extends DataType> getType() {
        return ShaderData.class;
    }

    public ShaderLoader(ShaderManager shaderManager) {
        this.shaderManager = shaderManager;
        shaderLibrary = new ConcurrentHashMap<>();
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
        JSONObject shadersInfo = shaderManager.getShadersInfo();

        // Load first the library files
        JSONObject libraryObject = shadersInfo.getJSONObject("library");
        for (String key : libraryObject.keySet()) {
            String content = FileMethods.readFileAsString(shaderManager.versionFolder() + libraryObject.getString(key));
            shaderLibrary.put(key, content);
        }

        JSONObject shadersList = shadersInfo.getJSONObject("shaders");

        // Then load the shaders
        Iterator<String> arrayShader = shadersList.keys();
        do {
            // Name of the shader key (used by renderer)
            String currentShader = arrayShader.next();

            JSONObject JShader = shadersList.getJSONObject(currentShader);

            // "Table" of the string path
            JSONArray files = JShader.getJSONArray("files");
            // From the working directory: the files are watched for hot reload
            String[] sArrayFiles = new String[files.length()];
            for (int i = 0; i < files.length(); ++i) {
                sArrayFiles[i] = shaderManager.versionFolder() + files.getString(i);
            }

            JSONArray tags = JShader.getJSONArray("tags");
            // Get id the access the shader more easily

            ShaderData shader = new ShaderData(
                    currentShader,
                    isTagIn(tags, "2D") || isTagIn(tags, "none"), sArrayFiles);

            data.put(currentShader, shader);

        } while(arrayShader.hasNext());
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
        // A missing file makes the preload fail: the fallback replaces the shader
        String vertex = completeWithLibraryFile(readSource(shader.getSourcePath(0)));
        shader.checkCancelled();
        String fragment = completeWithLibraryFile(readSource(shader.getSourcePath(1)));

        String geometry = null;
        if (shader.usesGeometryShader()) {
            shader.checkCancelled();
            geometry = completeWithLibraryFile(readSource(shader.getSourcePath(2)));
        }

        shader.setSources(vertex, fragment, geometry);
    }

    private static String readSource(String path) {
        if (!new File(path).isFile())
            throw new ResourceException("shader file not found: " + path);

        return FileMethods.readFileAsString(path);
    }

    public String completeWithLibraryFile(String content) {
        String includeTag = "#include";

        if (content.contains(includeTag)) {
            String[] lines = content.split("\n");
            StringBuilder newContent = new StringBuilder();

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith(includeTag)) {
                    String fileName = line.substring(includeTag.length());
                    fileName = fileName.trim();

                    if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                        fileName = fileName.substring(1, fileName.length() - 1);
                        System.out.println("Loading shader library file: " + fileName);
                    } else {
                        System.err.println("Error: shader library file name " + fileName + " is not valid");
                        continue;
                    }

                    if (shaderLibrary.containsKey(fileName)) {
                        newContent.append(shaderLibrary.get(fileName)).append("\n");
                    } else {
                        System.err.println("Error: shader library file " + fileName + " not found");
                    }
                } else {
                    newContent.append(line).append("\n");
                }
            }

            return newContent.toString();
        }

        return content;
    }
}
