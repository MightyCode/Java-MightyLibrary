package io.github.mightycode.mightylib.graphics.text;

import io.github.mightycode.mightylib.resources.ResourceException;
import org.joml.Vector4i;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FontFile {
    private static final int SPACE_CHAR_CODE = 32;
    private final String path;
    private Vector4i padding;

    private int size;
    private double lineHeight;
    private int atlasWidth, atlasHeight;
    private int numChars;

    private Map<Integer, FontChar> characters;

    public FontFile(String path) {
        this.path = path;
    }

    // Font of the fallback: each printable character is the whole atlas (a box), the space is empty
    static FontFile CreateBoxes(int atlasSize) {
        FontFile file = new FontFile(null);
        file.size = atlasSize;
        file.padding = new Vector4i();
        file.lineHeight = 1.2;
        file.atlasWidth = atlasSize;
        file.atlasHeight = atlasSize;
        file.characters = new HashMap<>();

        for (int code = SPACE_CHAR_CODE; code < 127; ++code) {
            boolean space = code == SPACE_CHAR_CODE;
            FontChar fontChar = new FontChar();
            fontChar.setId(code)
                    .setxAtlas(0).setyAtlas(0)
                    .setWidthAtlas(atlasSize).setHeightAtlas(atlasSize)
                    .setWidth(space ? 0 : 0.6).setHeight(space ? 0 : 0.8)
                    .setxOffset(0).setyOffset(0)
                    .setxAdvance(0.7);
            file.characters.put(code, fontChar);
        }

        file.numChars = file.characters.size();
        return file;
    }

    // A missing or unreadable file is a failure of the data (ResourceException): the fallback font replaces it
    public void load(){
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {

            String line;
            Map<String, String> values = new HashMap<>();

            do {
                line = bufferedReader.readLine();
                Map<String, String> test = decodeLine(line);
                values.putAll(test);

            } while(!values.containsKey("count"));

            size = Integer.parseInt(values.get("size"));
            if (size < 0)
                size *= -1;

            String[] tempPadding = values.get("padding").split(",");


            // 0 top, 1 right, 2 bottom, 3 left
            padding = new Vector4i(Integer.parseInt(tempPadding[0]),
                                    Integer.parseInt(tempPadding[1]),
                                    Integer.parseInt(tempPadding[2]),
                                    Integer.parseInt(tempPadding[3]));

            lineHeight = (Integer.parseInt(values.get("lineHeight"))) * 1.0 / size;

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
                        .setxAtlas(Integer.parseInt(values.get("x")) + padding.w /*/ atlasWidth*/)
                        .setyAtlas(Integer.parseInt(values.get("y")) + padding.x /*/ atlasHeight*/)
                        .setWidth((double) (Integer.parseInt(values.get("width")) - padding.w - padding.y) / size)
                        .setHeight((double) (Integer.parseInt(values.get("height")) - padding.x - padding.z) / size)
                        .setWidthAtlas(Integer.parseInt(values.get("width")) - padding.w - padding.y /*/ atlasWidth*/)
                        .setHeightAtlas(Integer.parseInt(values.get("height")) - padding.x - padding.z /*/ atlasHeight*/)
                        .setxOffset((double) (Integer.parseInt(values.get("xoffset"))) / size)
                        .setyOffset((double) (Integer.parseInt(values.get("yoffset"))) / size)
                        .setxAdvance((double) (Integer.parseInt(values.get("xadvance"))) / size);

                if (fontChar.getxOffset() < 0)
                    fontChar.setxOffset(-fontChar.getxOffset());

                if (fontChar.getyOffset() < 0)
                    fontChar.setyOffset(-fontChar.getyOffset());

                characters.put(id, fontChar);
            }

        } catch (IOException e) {
            throw new ResourceException("can't read the font file " + path, e);
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
        if (!characters.containsKey(code))
            return characters.get(SPACE_CHAR_CODE);

        return characters.get(code);
    }


    public double getLineHeight() {
        return lineHeight;
    }

    public int getReferenceSize() { return size; }

    public int atlasWidth(){ return atlasWidth; }

    public int atlasHeight(){ return atlasHeight; }
}
