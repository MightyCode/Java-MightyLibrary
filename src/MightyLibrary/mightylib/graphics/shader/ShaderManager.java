package MightyLibrary.mightylib.graphics.shader;

import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.scene.Camera;
import MightyLibrary.mightylib.util.Id;
import MightyLibrary.mightylib.util.ManagerList;
import org.joml.Matrix4f;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
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
    public static final int USE_PROJECTION_MATRIX = 0;

    private final ManagerList<Shader> shaders;
    private final ArrayList<Id> camReload;
    private int version;

    private ShaderManager() {
        shaders = new ManagerList<>();
        camReload = new ArrayList<>();

        version = -1;
    }

    public void forceShaderVersion(int version){
        this.version = version;
    }

    public void load(){
        System.out.println("--Load ShaderManager");
        shaders.clear();
        camReload.clear();

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

            // Get id the access the shader more easily
            Id currentId = shaders.add(new Shader(files.getString(0), files.getString(1)));
            Shader shad = shaders.get(currentId);
            shad.setName(currentShader);
            shad.load();

            // Links-uniform creation
            JSONArray linksName = JShader.getJSONArray("links");
            for(int i = 0; i < linksName.length(); ++i){
                shad.addLink(linksName.getString(i));
            }

            // Cam mode initialization
            String camMode = JShader.getJSONArray("mode").getString(0);

            if(camMode.equals("static")){
                Matrix4f model = new Matrix4f();
                model._m32(-8.572f);
                FloatBuffer temp = BufferUtils.createFloatBuffer(16);
                shad.glUniform("view", model.get(temp));
            } else if(camMode.equals("camView")){
                camReload.add(currentId);
                shad.properties.add(USE_PROJECTION_MATRIX);
            }
        } while(arrayShader.hasNext());
    }

    public void reloadProjection(Camera camera){
        System.out.println("--Reload projection for camera");
        Id current;
        for(int i = 0; i < shaders.size(); ++i){
            current = new Id(i);
            if(shaders.get(current).properties.contains(USE_PROJECTION_MATRIX)) reloadProjection(camera, current);
        }
    }

    public void reloadProjection(Camera camera, Id id){
        shaders.get(id).glUniform("projection", camera.getProjection());
    }

    public void dispose(Camera camera){
        Shader currentShader;
        for (Id current : camReload){
            currentShader = shaders.get(current);
            currentShader.use();
            currentShader.glUniform("view", camera.getView());
        }
    }

    public Id getIdShaderFromString(String shaderId) {
        return shaders.getIdFromString(shaderId);
    }

    public Shader getShader(Id shaderId){
        return shaders.get(shaderId);
    }

    public void reload(){
        unload();
        System.out.println("Reload shaders");
        load();
    }

    public void unload(){
        System.out.println("--Unload ShaderManager");
        for(int i = 0; i < shaders.size(); ++i){
            shaders.get(i).unload();
        }
    }
}
