package MightyLibrary.mightylib.graphics.shader;

import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.scenes.Camera2D;
import MightyLibrary.mightylib.scenes.Camera3D;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import static org.lwjgl.opengl.GL11C.glGetString;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

public class ShaderManager {

    private static ShaderManager singletonInstance = null;
    public static ShaderManager getInstance(){
        if (singletonInstance == null) singletonInstance = new ShaderManager();

        return singletonInstance;
    }
    private static final String SHADER_INFO_PATH = "resources/shaders/";

    public static final String GENERIC_PROJECTION_FIELD_NAME = "projection";
    public static final String GENERIC_VIEW_FIELD_NAME = "view";
    public static final String GENERIC_MODEL_FIELD_NAME = "model";
    public static final String GENERIC_COLOR_FIELD_NAME = "color";

    private final HashMap<String, Shader> shaders;
    private int version;

    private Camera3D mainCamera3D;
    private Camera2D mainCamera2D;

    private ShaderManager() {
        shaders = new HashMap<>();
        version = -1;

        mainCamera3D = null;
        mainCamera2D = null;
    }

    public void forceShaderVersion(int version){
        this.version = version;
    }

    public void load(){
        System.out.println("--Load ShaderManager");
        shaders.clear();

        String glslVersion = glGetString(GL_SHADING_LANGUAGE_VERSION);
        String shaderPath = SHADER_INFO_PATH;

        if (version == 140 || (glslVersion != null && glslVersion.contains("1.40"))){
            shaderPath += "1_40/shaders.json";
            Shader.PATH += "1_40/";
            System.out.println("--Version of shader GLSL 140");
        } else {
            shaderPath += "3_30/shaders.json";
            Shader.PATH += "3_30/";
            System.out.println("--Version of shader GLSL 330");
        }

        JSONObject file = new JSONObject(FileMethods.readFileAsString(shaderPath));
        file = file.getJSONObject("shaders");

        Iterator<String> arrayShader = file.keys();

        do{
            // Name of the shader key (used by renderer)
            String currentShader = arrayShader.next();

            JSONObject JShader = file.getJSONObject(currentShader);

            // "Table" of the string path
            JSONArray files = JShader.getJSONArray("files");

            String dimensionMode = JShader.getString("mode");
            // Get id the access the shader more easily

            Shader shader;

            if (files.length() == 2) {
                shader = new Shader(files.getString(0),
                        files.getString(1),
                        dimensionMode.equals("2D") || dimensionMode.equals("none"));
            } else {
                shader = new Shader(files.getString(0),
                        files.getString(1), files.getString(2),
                        dimensionMode.equals("2D") || dimensionMode.equals("none"));
            }

            shaders.put(currentShader, shader);
            shader.setName(currentShader);
            shader.load();

            // Links-uniform creation
            JSONArray linksName = JShader.getJSONArray("links");
            for(int i = 0; i < linksName.length(); ++i){
                shader.addLink(linksName.getString(i));
            }

        } while(arrayShader.hasNext());
    }

    public void setCameras(Camera2D mainCamera2D, Camera3D mainCamera3D){
        this.mainCamera2D = mainCamera2D;
        this.mainCamera3D = mainCamera3D;
    }

    public Shader getShader(String shaderName){
        return shaders.get(shaderName);
    }

    public void sendCameraToShader(Shader shader, boolean shouldSendProjection, boolean shouldSendView) {
        if (shader.isDimension2DShader()) {
            if (shouldSendProjection)
                shader.sendValueToShader(mainCamera2D.getProjection());

            if (shouldSendView)
                shader.sendValueToShader(mainCamera2D.getView());
        } else {
            if (shouldSendProjection)
                shader.sendValueToShader(mainCamera3D.getProjection());

            if (shouldSendView)
                shader.sendValueToShader(mainCamera3D.getView());
        }
    }

    public Camera2D getMainCamera2D(){ return (Camera2D) mainCamera2D; }
    public Camera3D getMainCamera3D(){ return (Camera3D) mainCamera3D; }

    public void reload(){
        unload();
        System.out.println("Reload shaders");
        load();
    }

    public void unload(){
        System.out.println("--Unload ShaderManager");
        for (Shader shader : shaders.values())
            shader.unload();

        shaders.clear();
    }
}
