package MightyLibrary.mightylib.render.shape.font;

import MightyLibrary.mightylib.util.FileMethods;
import MightyLibrary.mightylib.util.ManagerList;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Iterator;

public class TextManager {
    private static final String FONTS_INFO_PATH = "resources/textures/fonts/fonts.json";

    private ManagerList<Font> fonts;

    public TextManager(){
        fonts = new ManagerList<>();
        JSONObject file = new JSONObject(FileMethods.readFileAsString(FONTS_INFO_PATH));
        file = file.getJSONObject("shaders");

        Iterator<String> arrayShader = file.keys();


        do{
            // Name of the font key (used by text)
            String currentFont = arrayShader.next();

            JSONObject JShader = file.getJSONObject(currentFont);
            // "Table" of the string path

            JSONArray textureName = JShader.getJSONArray("texture");
            JSONArray info = JShader.getJSONArray("info");

            fonts.add(new Font(currentFont, textureName.getString(0), info.getString(0)));
        } while(arrayShader.hasNext());
    }


    public void bind(){

    }


    public void constructVbo(){

    }
}
