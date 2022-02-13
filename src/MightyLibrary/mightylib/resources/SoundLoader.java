package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.sounds.SoundData;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class SoundLoader {
    public static void load(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/sounds/sounds.json"));
        obj = obj.getJSONObject("sounds");

        SoundLoader.load(data, obj, "");
    }

    private static void load(Map<String, DataType> data, JSONObject node, String currentPath){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        do{
            String currentNode = arrayNodes.next();

            if(node.get(currentNode) instanceof JSONObject){
                SoundLoader.load(data, node.getJSONObject(currentNode), currentPath + currentNode + "/");
            } else {
                data.put(currentNode, new SoundData(currentNode, currentPath + node.getString(currentNode)));
            }
        } while(arrayNodes.hasNext());
    }
}
