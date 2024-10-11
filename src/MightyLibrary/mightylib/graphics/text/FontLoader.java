package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.glGetError;

public class FontLoader extends ResourceLoader {
    @Override
    public Class<? extends DataType> getType() {
        return FontFace.class;
    }

    @Override
    public String getResourceNameType() {
        return "FontFace";
    }

    @Override
    public void create(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/textures/fonts/fonts.json"));
        obj = obj.getJSONObject("fonts");

        create(data, obj);
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {}

    @Override
    public String filterFile(String path) {
        return null;
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
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof FontFace))
            return;

        FontFace fontFace = (FontFace) dataType;
        fontFace.getFontFile().load();
    }
}
