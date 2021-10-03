package MightyLibrary.mightylib.graphics.shape.font;

import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.util.ManagerList;
import org.json.JSONObject;
import java.util.Iterator;

public class TextManager {
    private static final String FONTS_INFO_PATH = "resources/textures/fonts/fonts.json";

    private ManagerList<Font> fonts;

    public TextManager(){
        fonts = new ManagerList<>();
        JSONObject file = new JSONObject(FileMethods.readFileAsString(FONTS_INFO_PATH));
        file = file.getJSONObject("fonts");

        Iterator<String> arrayShader = file.keys();


        do{
            // Name of the font key (used by text)
            String currentFont = arrayShader.next();

            JSONObject JShader = file.getJSONObject(currentFont);

            String textureName = JShader.getString("texture");
            String info = JShader.getString("info");

            fonts.add(new Font(currentFont, textureName, info));
        } while(arrayShader.hasNext());
    }


    public void bind(){

    }


    public void constructVbo(){

    }
}
