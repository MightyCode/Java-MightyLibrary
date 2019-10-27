package MightyLibrary.render.shader;

import MightyLibrary.scene.Camera;
import MightyLibrary.util.FileMethods;
import MightyLibrary.util.Id;
import MightyLibrary.util.ManagerList;
import org.joml.Matrix4f;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;

public class ShaderManager {
    public static final int TEXTURE = 0;
    public static final int BOX = 1;

    public static final int CAMERA_RELOADING = 0;

    private ManagerList<Shader> shaders;
    private ArrayList<Id> camReload;

    private Camera cam;

    public ShaderManager(Camera cam){
        shaders = new ManagerList<>();

        camReload = new ArrayList<>();
        this.cam = cam;

        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/shaders/shaders.json"));
        obj = obj.getJSONObject("shaders");

        Iterator<String> arrayShader = obj.keys();

        do{
            // Name
            String currentShader = arrayShader.next();

            JSONObject JShader = obj.getJSONObject(currentShader);
            // "Table" of the string path
            JSONArray files = JShader.getJSONArray("files");

            // Get id the access the shader more easily
            Id currentId = shaders.add(new Shader(files.getString(0), files.getString(1)));
            shaders.get(currentId).setName(currentShader);
            shaders.get(currentId).load();

            // Links-uniform creation
            JSONArray linksName = JShader.getJSONArray("links");
            for(int i = 0; i < linksName.length(); i++){
                shaders.get(currentId).addLink(linksName.getString(i));
            }

            // Cam mode initialization
            String camMode = JShader.getJSONArray("mode").getString(0);

            if(camMode.equals("static")){
                Matrix4f model = new Matrix4f();
                model._m32(-8.572f);
                FloatBuffer temp = BufferUtils.createFloatBuffer(16);
                shaders.get(currentId).glUniform("view", model.get(temp));
            } else if(camMode.equals("camView")){
                camReload.add(currentId);
                shaders.get(currentId).glUniform("projection", cam.getProjection());
            }



        } while(arrayShader.hasNext());

        dispose();
    }

    public void dispose(){
        for (Id current : camReload){
            shaders.get(current).use().glUniform("view", cam.getView());
        }
    }

    public Id getIdShaderFromString(String shaderId) {
        return shaders.getIdFromString(shaderId);
    }

    public Shader getShader(Id shaderId){
        return shaders.get(shaderId);
    }

    public void unload(){
        for(int i = 0; i < shaders.size(); ++i){
            shaders.get(i).unload();
        }
    }
}
