package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.resources.map.Tileset;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class TextureLoader extends ResourceLoader {

    public TextureLoader(){
        super(Texture.class);
    }

    @Override
    public void create(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/textures/textures.json"));
        obj = obj.getJSONObject("textures");

        data.put("error", new Texture("error", "error.png"));
        load(data, obj, "");
    }

    private void load(Map<String, DataType> data, JSONObject node, String currentPath){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        do{
            String currentNode = arrayNodes.next();

            if(node.get(currentNode) instanceof JSONObject){
                load(data, node.getJSONObject(currentNode), currentPath + currentNode + "/");
            } else {
                data.put(currentNode, new Texture(currentNode, currentPath + node.getString(currentNode)));
            }
        } while(arrayNodes.hasNext());
    }
}
