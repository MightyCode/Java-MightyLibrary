package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class FontLoader {
    public static void load(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/textures/fonts/fonts.json"));
        obj = obj.getJSONObject("fonts");

        FontLoader.load(data, obj);
    }

    private static void load(Map<String, DataType> data, JSONObject node){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext())
            return;

        do{
            String currentNode = arrayNodes.next();
            JSONObject values = node.getJSONObject(currentNode);

            if (values.length() == 2)
                data.put(currentNode, new FontFace(currentNode, values.getString("texture"), values.getString("info")));

        } while(arrayNodes.hasNext());
    }
}
