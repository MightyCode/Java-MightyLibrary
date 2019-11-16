package MightyLibrary.mightylib.render.shape.font;

import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.render.texture.TextureManager;
import MightyLibrary.mightylib.util.FileMethods;
import MightyLibrary.mightylib.util.Id;
import MightyLibrary.mightylib.util.ObjectId;
import org.joml.Vector4f;

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
    private HashMap<Integer, Vector4f> chars;

    private TextureManager textureManager;
    private Id fontTextureId;

    public Font(String fontName, String textureName, String info){
        super(fontName);

        textureManager = ManagerContainer.getInstance().textureManager;
        fontTextureId = textureManager.getIdShaderFromString(textureName);

        sizeX = 0;
        sizeY = 0;

        numberChar = 1;
        chars = new HashMap<>();
        load(info);
    }


    public void load(String info){
        String file = FileMethods.readFileAsString(PATH + info);
    }


    public float[] constructVbo(){
        return null;
    }

    public void bind(){
        textureManager.bind(fontTextureId);
    }
}
