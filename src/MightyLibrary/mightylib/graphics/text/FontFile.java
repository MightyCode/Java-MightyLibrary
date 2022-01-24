package MightyLibrary.mightylib.graphics.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class FontFile {
    private int paddingLeft,
            paddingRight,
            paddingTop,
            paddingBottom;


    private int size;

    private float lineHeight;


    private int atlasWidth, atlasHeight;


    private int numChars;


    private Map<Integer, FontChar> characters;

    private static final int NEEDED_PADDING = 2;

    public FontFile(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(path)));

            String line;
            Map<String, String> values = new HashMap<>();

            line = bufferedReader.readLine();
            values = decodeLine(line);

            size = Integer.parseInt(values.get("size"));

            String[] padding = values.get("padding").split(",");

            paddingTop = Integer.parseInt(padding[0]);
            paddingRight = Integer.parseInt(padding[1]);
            paddingBottom = Integer.parseInt(padding[2]);
            paddingLeft = Integer.parseInt(padding[3]);

            line = bufferedReader.readLine();
            values = decodeLine(line);

            lineHeight = (float) (Integer.parseInt(values.get("lineHeight")) - paddingTop - paddingBottom) / size;

            atlasWidth = Integer.parseInt(values.get("scaleW"));
            atlasHeight = Integer.parseInt(values.get("scaleH"));

            bufferedReader.readLine();

            line = bufferedReader.readLine();
            values = decodeLine(line);

            numChars = Integer.parseInt(values.get("count"));

            characters = new HashMap<Integer, FontChar>(numChars);

            for (int i = 0; i < numChars; i++) {
                line = bufferedReader.readLine();
                values = decodeLine(line);

                FontChar fontChar = new FontChar();

                int id = Integer.parseInt(values.get("id"));
                fontChar.setId(id)
                        .setxAtlas((float) (Integer.parseInt(values.get("x")) + paddingLeft - NEEDED_PADDING) / atlasWidth)
                        .setyAtlas((float) (Integer.parseInt(values.get("y")) + paddingTop - NEEDED_PADDING) / atlasHeight)
                        .setWidth((float) (Integer.parseInt(values.get("width")) - paddingLeft - paddingRight + (2 * NEEDED_PADDING)) / size)
                        .setHeight((float) (Integer.parseInt(values.get("height")) - paddingTop - paddingBottom + (2 * NEEDED_PADDING)) / size)
                        .setWidthAtlas((float) (Integer.parseInt(values.get("width")) - paddingLeft - paddingRight + (2 * NEEDED_PADDING)) / atlasWidth)
                        .setHeightAtlas((float) (Integer.parseInt(values.get("height")) - paddingTop - paddingBottom + (2 * NEEDED_PADDING)) / atlasHeight)
                        .setxOffset((float) (Integer.parseInt(values.get("xoffset")) + paddingLeft - NEEDED_PADDING) / size)
                        .setyOffset((float) (Integer.parseInt(values.get("yoffset")) + paddingTop - NEEDED_PADDING) / size)
                        .setxAdvance((float) (Integer.parseInt(values.get("xadvance")) - paddingLeft - paddingRight) / size);

                characters.put(id, fontChar);
            }

            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }


    private Map<String, String> decodeLine(String line) {
        String[] parts = line.split(" ");
        Map<String, String> values = new HashMap<String, String>();

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
