package MightyLibrary.mightylib.graphics.shape.font;

import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.graphics.texture.TextureManager;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.util.Id;
import MightyLibrary.mightylib.util.ObjectId;
import MightyLibrary.mightylib.util.math.MightyMath;
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
            else ++count;
        }

        for(int i = 0; i < numberChar; ++i){
            int id = getValueOfI(lines[count], "id=");

            int x = getValueOfI(lines[count], "x=");
            int y = getValueOfI(lines[count], "y=");
            int width = getValueOfI(lines[count], "width=");
            int height = getValueOfI(lines[count], "height=");

            ++count;

            chars.put(id, new Vector4i(x, y, width, height));
            //System.out.println(chars.get(id).x + " " + chars.get(id).y + " " + chars.get(id).z  + " " +chars.get(id).w );
        }
    }

    private int searchValueOfI(String[] lines, String key){
        for (int i = 0; i < lines.length; ++i){
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


    public float[][] constructVbo(String text, float size){
        return constructVbo(text, size, "center");
    }

    public float[][] constructVbo(String text, float size, String centering){
        String[] lines = text.split("\n");
        int numberChars = text.length() - lines.length + 1;
        int[] partAdvancement = new int[lines.length];

        float[][] vbo = new float[2][];
        float[] positions = new float[4 * 2 * numberChars];
        float[] texturePos = new float[4 * 2 * numberChars];

        // Calculations and securities
        for (int i = 0; i < lines.length; ++i){
            if (i != 0) {
                partAdvancement[i] = partAdvancement[i - 1];
                partAdvancement[i] += lines[i - 1].length();
            }

            float[] sizes = new float[lines[i].length()];
            for (int j = 0; j < lines[i].length(); ++j){
                if (!chars.containsValue(chars.get((int)lines[i].charAt(j)))){
                    lines[i].replace(lines[i].charAt(j), ' ');
                }

                sizes[j] = (size * chars.get((int)lines[i].charAt(j)).get(SIZE_X) /
                        chars.get((int)lines[i].charAt(j)).get(SIZE_Y));
            }

            // Left
            float halfSizeX = 0;
            // Up
            float halfSizeY = 0;

            if (centering.contains("right")){
                halfSizeX = MightyMath.sum(sizes);
            // Center
            } else {
                halfSizeX = MightyMath.sum(sizes) / 2;
            }

            if (centering.contains("down")){
                halfSizeY = size * (3.0f / 2 * lines.length - 0.5f);
            // Center
            } else {
                halfSizeY = size * (3.0f / 2 * lines.length - 0.5f) / 2;
            }

            for (int j = 0; j < sizes.length; ++j){
                // posX
                positions[(partAdvancement[i] + j) * 8] = MightyMath.sum(sizes, 0, j) - halfSizeX;
                positions[(partAdvancement[i] + j) * 8 + 2] = MightyMath.sum(sizes, 0, j+1) - halfSizeX;
                positions[(partAdvancement[i] + j) * 8 + 4] = positions[(partAdvancement[i] + j) * 8 + 2];
                positions[(partAdvancement[i] + j) * 8 + 6] = positions[(partAdvancement[i] + j) * 8];

            }

        }

        vbo[0] = positions;
        vbo[1] = texturePos;

        return vbo;
    }

    public void bind(){
        textureManager.bind(fontTextureId);
    }
}
