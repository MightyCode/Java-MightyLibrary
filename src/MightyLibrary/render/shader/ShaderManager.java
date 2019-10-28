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
    public static final int USE_PROJECTION_MATRIX = 0;

    private ManagerList<Shader> shaders;
    private ArrayList<Id> camReload;

    private Camera cam;

    public ShaderManager(Camera cam) {
        shaders = new ManagerList<>();

        camReload = new ArrayList<>();
        this.cam = cam;
        this.init();
    }

    public void init(){
        shaders.clear();
        camReload.clear();
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
            Shader shad = shaders.get(currentId);
            shad.setName(currentShader);
            shad.load();

            // Links-uniform creation
            JSONArray linksName = JShader.getJSONArray("links");
            for(int i = 0; i < linksName.length(); i++){
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

        reloadProjection();
        dispose();
    }

    public void reloadProjection(){
        for(int i = 0; i < shaders.size(); i++){
            if(shaders.get(new Id(i)).properties.contains(USE_PROJECTION_MATRIX)) reloadProjection(new Id(i));
        }
    }

    public void reloadProjection(Id id){
        shaders.get(id).glUniform("projection", cam.getProjection());
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

    public void reload(){
        unload();
        System.out.println("Reload shaders");
        init();
    }

    public void unload(){
        for(int i = 0; i < shaders.size(); ++i){
            shaders.get(i).unload();
        }
    }
}
