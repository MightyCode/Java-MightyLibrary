package MightyLibrary.mightylib.render.shape.font;

import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.render.texture.TextureManager;
import MightyLibrary.mightylib.util.FileMethods;
import MightyLibrary.mightylib.util.Id;
import MightyLibrary.mightylib.util.ObjectId;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.util.HashMap;

public class Font extends ObjectId {
    private static final String PATH = "resources/textures/fonts/";

    private static final int POS_X = 0;
    private static final int POS_Y = 1;
    private static final int SIZE_X = 2;
    private static final int SIZE_Y = 3;

    private int sizeX;
    private int sizeY;

    private int numberChar;
    private HashMap<Integer, Vector4i> chars;

    private TextureManager textureManager;
    private Id fontTextureId;

    public Font(String fontName, String textureName, String info){
        super(fontName);

        textureManager = ManagerContainer.getInstance().textureManager;
        fontTextureId = textureManager.getIdShaderFromString(textureName);

        sizeX = 0;
        sizeY = 0;

        numberChar = 0;
        chars = new HashMap<>();
        load(info);
    }


    public void load(String info){
        String file = FileMethods.readFileAsString(PATH + info);
        String[] lines = file.split("\n");

        sizeX = searchValueOfI(lines, "scaleW=");
        sizeY = searchValueOfI(lines, "scaleH=");
        numberChar = searchValueOfI(lines, "chars count=");

        int count = 0;
        boolean beginChar = false;
        while (!beginChar){
            if (lines[count].contains("char id="))  beginChar = true;
            else count++;
        }

        for(int i = 0; i < numberChar; i++){
            int id = getValueOfI(lines[count], "id=");

            int x = getValueOfI(lines[count], "x=");
            int y = getValueOfI(lines[count], "y=");
            int width = getValueOfI(lines[count], "width=");
            int height = getValueOfI(lines[count], "height=");

            count++;

            chars.put(id, new Vector4i(x, y, width, height));
            //System.out.println(chars.get(id).x + " " + chars.get(id).y + " " + chars.get(id).z  + " " +chars.get(id).w );
        }
    }

    private int searchValueOfI(String[] lines, String key){
        for (int i = 0; i < lines.length; i++){
            if (lines[i].contains(key)){
                return getValueOfI(lines[i], key);
            }
        }

        return -1;
    }

    private int getValueOfI(String line, String key){
        int beginIndex = line.indexOf(key) + key.length();
        int lastIndex = line.indexOf(' ', beginIndex);
        if (lastIndex == -1) lastIndex = line.indexOf('\n', beginIndex);
        if (lastIndex == -1) lastIndex = line.length();

        return Integer.parseInt(line.substring(beginIndex, lastIndex));
    }


    public float[] constructVbo(){
        return null;
    }

    public void bind(){
        textureManager.bind(fontTextureId);
    }
}
