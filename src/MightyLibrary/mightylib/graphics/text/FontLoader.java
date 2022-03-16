package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class FontLoader extends ResourceLoader {
    public FontLoader(){
        super(FontFace.class);
    }

     @Override
    public void create(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/textures/fonts/fonts.json"));
        obj = obj.getJSONObject("fonts");

        create(data, obj);
    }

    private void create(Map<String, DataType> data, JSONObject node){
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


    @Override
    public boolean load(DataType dataType) {
        if (dataType.getType() != EDataType.Font)
            return false;

        FontFace fontFace = (FontFace) dataType;

        fontFace.getFontFile().load();

        return true;
    }
}
