package MightyLibrary.render.texture;

import MightyLibrary.util.FileMethods;
import MightyLibrary.util.Id;
import MightyLibrary.util.ManagerList;
import org.json.JSONObject;
import java.util.Iterator;

public class TextureManager {
    private ManagerList<Texture> textures;

    public TextureManager(){
        textures = new ManagerList<>();
    }

    public void init(){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/textures/textures.json"));
        obj = obj.getJSONObject("textures");

        Texture error = new Texture("error", "error.png");
        error.setId(0).setName("error");

        textures.add(error);

        load(obj, "");
    }

    public void load(JSONObject node, String path){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        do{
            String currentNode = arrayNodes.next();

            if(node.get(currentNode) instanceof JSONObject){
                load(node.getJSONObject(currentNode), path + currentNode + "/");
            } else {
                textures.add(new Texture(currentNode, path + node.getString(currentNode)));
                textures.get(new Id(textures.size()-1)).setName(currentNode);
            }
        } while(arrayNodes.hasNext());
    }

    public void bind(Id id){
        textures.get(id).bind();
    }

    public Id getIdShaderFromString(String shaderId) {
        return textures.getIdFromString(shaderId);
    }

    public void unload() {
        for(int i = 0; i < textures.size(); ++i){
            textures.get(new Id(i)).unload();
        }
    }
}
