package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.texture.Texture;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class TextureLoader {

    public static void load(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/textures/textures.json"));
        obj = obj.getJSONObject("textures");

        data.put("error", new Texture("error", "error.png"));
        TextureLoader.load(data, obj, "");
    }

    private static void load(Map<String, DataType> data, JSONObject node, String currentPath){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        do{
            String currentNode = arrayNodes.next();

            if(node.get(currentNode) instanceof JSONObject){
                TextureLoader.load(data, node.getJSONObject(currentNode), currentPath + currentNode + "/");
            } else {
                data.put(currentNode, new Texture(currentNode, currentPath + node.getString(currentNode)));
            }
        } while(arrayNodes.hasNext());
    }
}
