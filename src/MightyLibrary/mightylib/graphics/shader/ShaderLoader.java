package MightyLibrary.mightylib.graphics.shader;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class ShaderLoader extends ResourceLoader {
    private static final String SHADER_INFO_PATH = "resources/shaders/";
    private static final String LIST_SHADER_FILE_NAME = "shaders.json";

    private final ShaderManager shaderManager = ShaderManager.getInstance();
    @Override
    public Class<? extends DataType> getType() {
        return Shader.class;
    }

    @Override
    public String getResourceNameType() {
        return "Shader";
    }

    @Override
    public void create(Map<String, DataType> data) {
        String shaderListPath = SHADER_INFO_PATH + shaderManager.getVersion() + "/" + LIST_SHADER_FILE_NAME;

        JSONObject file = new JSONObject(FileMethods.readFileAsString(shaderListPath));
        file = file.getJSONObject("shaders");

        Iterator<String> arrayShader = file.keys();

        do {
            // Name of the shader key (used by renderer)
            String currentShader = arrayShader.next();

            JSONObject JShader = file.getJSONObject(currentShader);

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

        shader.setShaderContent(0, vertexShaderContent);

        String fragmentShaderContent = "error";

        try {
            fragmentShaderContent = FileMethods.readFileAsString(fragmentShaderPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.print("Error path :" + fragmentShaderPath + " not found");
        }

        shader.setShaderContent(1, fragmentShaderContent);

        if (useGeometryShader) {
            String geometryShaderContent = "error";

            try {
                geometryShaderContent = FileMethods.readFileAsString(geometryShaderPath);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.print("Error path :" + geometryShaderPath + " not found");
            }

            shader.setShaderContent(2, geometryShaderContent);
        }
    }
}
