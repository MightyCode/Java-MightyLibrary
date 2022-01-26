package MightyLibrary.mightylib.graphics.text;

import org.joml.Vector4i;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FontFile {
    private String path;

    private Vector4i padding;


    private int size;

    private float lineHeight;


    private int atlasWidth, atlasHeight;


    private int numChars;


    private Map<Integer, FontChar> characters;

    private static final int NEEDED_PADDING = 2;

    public FontFile(String path) {
        this.path = path;
    }


    public boolean load(){
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));

            String line;
            Map<String, String> values = new HashMap<>();

            do {
                line = bufferedReader.readLine();
                Map<String, String> test = decodeLine(line);
                values.putAll(test);

            } while(!values.containsKey("count"));

            size = Integer.parseInt(values.get("size"));

            String[] tempPadding = values.get("padding").split(",");


            // 0 top, 1 right, 2 bottom, 3 left
            padding = new Vector4i(Integer.parseInt(tempPadding[0]),
                                    Integer.parseInt(tempPadding[1]),
                                    Integer.parseInt(tempPadding[2]),
                                    Integer.parseInt(tempPadding[3]));


            lineHeight = (float) (Integer.parseInt(values.get("lineHeight")) - padding.x - padding.z) / size;

            atlasWidth = Integer.parseInt(values.get("scaleW"));
            atlasHeight = Integer.parseInt(values.get("scaleH"));

            numChars = Integer.parseInt(values.get("count"));

            characters = new HashMap<>(numChars);

            for (int i = 0; i < numChars; i++) {
                line = bufferedReader.readLine();
                values = decodeLine(line);

                FontChar fontChar = new FontChar();

                int id = Integer.parseInt(values.get("id"));

                fontChar.setId(id)
                        .setxAtlas((float) (Integer.parseInt(values.get("x")) + padding.w - NEEDED_PADDING) / atlasWidth)
                        .setyAtlas((float) (Integer.parseInt(values.get("y")) + padding.x - NEEDED_PADDING) / atlasHeight)
                        .setWidth((float) (Integer.parseInt(values.get("width")) - (padding.w - padding.y) + (2 * NEEDED_PADDING)) / size)
                        .setHeight((float) (Integer.parseInt(values.get("height")) - (padding.x - padding.z) + (2 * NEEDED_PADDING)) / size)
                        .setWidthAtlas((float) (Integer.parseInt(values.get("width")) - (padding.w - padding.y) + (2 * NEEDED_PADDING)) / atlasWidth)
                        .setHeightAtlas((float) (Integer.parseInt(values.get("height")) - (padding.x - padding.z) + (2 * NEEDED_PADDING)) / atlasHeight)
                        .setxOffset((float) (Integer.parseInt(values.get("xoffset")) + padding.w - NEEDED_PADDING) / size)
                        .setyOffset((float) (Integer.parseInt(values.get("yoffset")) + padding.x - NEEDED_PADDING) / size)
                        .setxAdvance((float) (Integer.parseInt(values.get("xadvance")) - padding.w - padding.y) / size);

                characters.put(id, fontChar);
            }

            bufferedReader.close();

            return true;

        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    private Map<String, String> decodeLine(String line) {
        String[] parts = line.split(" ");
        Map<String, String> values = new HashMap<>();

        for (String part : parts) {
            String[] valueData = part.split("=");

            if (valueData.length == 2) {
                values.put(valueData[0], valueData[1]);
            }
        }

        return values;
    }


    public FontChar getCharacter(int code) {
        return characters.get(code);
    }


    public float getLineHeight() {
        return lineHeight;
    }


}
