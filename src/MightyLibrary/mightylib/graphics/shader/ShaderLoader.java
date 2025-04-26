package MightyLibrary.mightylib.graphics.shader;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static MightyLibrary.mightylib.graphics.shader.ShaderManager.SHADER_INFO_PATH;

public class ShaderLoader extends ResourceLoader {

    /*
     * Shader library
     * The shader library is a HashMap that contains the shader utility file name as key and the shader content as value.
     */
    private Map<String, String> shaderLibrary;

    private final ShaderManager shaderManager = ShaderManager.getInstance();
    @Override
    public Class<? extends DataType> getType() {
        return Shader.class;
    }

    public ShaderLoader() {
        shaderLibrary = new HashMap<>();
    }

    @Override
    public String getResourceNameType() {
        return "Shader";
    }

    @Override
    public void create(Map<String, DataType> data) {
        JSONObject shadersInfo = shaderManager.getShadersInfo();

        // Load first the library files
        JSONObject libraryObject = shadersInfo.getJSONObject("library");
        for (String key : libraryObject.keySet()) {
            String content = FileMethods.readFileAsString(SHADER_INFO_PATH + shaderManager.getVersion() + "/" + libraryObject.getString(key));
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
            String[] sArrayFiles = new String[files.length()];
            for (int i = 0; i < files.length(); ++i) {
                sArrayFiles[i] = files.getString(i);
            }

            JSONArray tags = JShader.getJSONArray("tags");
            // Get id the access the shader more easily

            Shader shader = new Shader(
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
        if (!(dataType instanceof Shader)) {
            return;
        }

        Shader shader = (Shader) dataType;

        boolean useGeometryShader = shader.numberSource() == 3;

        String vertexShaderPath = SHADER_INFO_PATH + shaderManager.getVersion() + "/" + shader.getSourcePath(0);
        String fragmentShaderPath = SHADER_INFO_PATH + shaderManager.getVersion() + "/" + shader.getSourcePath(1);

        String geometryShaderPath = null;
        if (useGeometryShader) {
            shader.activateGeometryShader();
            geometryShaderPath = SHADER_INFO_PATH + shaderManager.getVersion() + "/" + shader.getSourcePath(2);
        }

        String vertexShaderContent = "error";

        try {
            vertexShaderContent = FileMethods.readFileAsString(vertexShaderPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.print("Error path :" + vertexShaderPath + " not found");
        }

        shader.setShaderContent(0, completeWithLibraryFile(vertexShaderContent));

        String fragmentShaderContent = "error";

        try {
            fragmentShaderContent = FileMethods.readFileAsString(fragmentShaderPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.print("Error path :" + fragmentShaderPath + " not found");
        }

        shader.setShaderContent(1, completeWithLibraryFile(fragmentShaderContent));

        if (useGeometryShader) {
            String geometryShaderContent = "error";

            try {
                geometryShaderContent = FileMethods.readFileAsString(geometryShaderPath);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.print("Error path :" + geometryShaderPath + " not found");
            }

            shader.setShaderContent(2,  completeWithLibraryFile(geometryShaderContent));
        }
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
