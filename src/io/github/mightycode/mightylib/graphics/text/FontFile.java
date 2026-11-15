package io.github.mightycode.mightylib.graphics.text;

import io.github.mightycode.mightylib.resources.ResourceException;
import org.joml.Vector4i;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {

            String line;
            Map<String, String> values = new HashMap<>();

            do {
                line = readLine(bufferedReader);
                Map<String, String> test = decodeLine(line);
                values.putAll(test);

            } while(!values.containsKey("count"));

            size = Integer.parseInt(required(values,"size"));
            if (size < 0)
                size *= -1;

            String[] tempPadding = required(values,"padding").split(",");


            // 0 top, 1 right, 2 bottom, 3 left
            padding = new Vector4i(Integer.parseInt(tempPadding[0]),
                                    Integer.parseInt(tempPadding[1]),
                                    Integer.parseInt(tempPadding[2]),
                                    Integer.parseInt(tempPadding[3]));

            lineHeight = (Integer.parseInt(required(values,"lineHeight"))) * 1.0 / size;

            atlasWidth = Integer.parseInt(required(values,"scaleW"));
            atlasHeight = Integer.parseInt(required(values,"scaleH"));

            numChars = Integer.parseInt(required(values,"count"));

            characters = new HashMap<>(numChars);

            for (int i = 0; i < numChars; i++) {
                line = readLine(bufferedReader);
                values = decodeLine(line);

                FontChar fontChar = new FontChar();

                int id = Integer.parseInt(required(values,"id"));

                fontChar.setId(id)
                        .setxAtlas(Integer.parseInt(required(values,"x")) + padding.w /*/ atlasWidth*/)
                        .setyAtlas(Integer.parseInt(required(values,"y")) + padding.x /*/ atlasHeight*/)
                        .setWidth((double) (Integer.parseInt(required(values,"width")) - padding.w - padding.y) / size)
                        .setHeight((double) (Integer.parseInt(required(values,"height")) - padding.x - padding.z) / size)
                        .setWidthAtlas(Integer.parseInt(required(values,"width")) - padding.w - padding.y /*/ atlasWidth*/)
                        .setHeightAtlas(Integer.parseInt(required(values,"height")) - padding.x - padding.z /*/ atlasHeight*/)
                        .setxOffset((double) (Integer.parseInt(required(values,"xoffset"))) / size)
                        .setyOffset((double) (Integer.parseInt(required(values,"yoffset"))) / size)
                        .setxAdvance((double) (Integer.parseInt(required(values,"xadvance"))) / size);

                if (fontChar.getxOffset() < 0)
                    fontChar.setxOffset(-fontChar.getxOffset());

                if (fontChar.getyOffset() < 0)
                    fontChar.setyOffset(-fontChar.getyOffset());

                characters.put(id, fontChar);
            }

        } catch (IOException e) {
            throw new ResourceException("can't read the font file " + path, e);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // A malformed value: the file is not a valid font, not a bug of the engine
            throw new ResourceException("malformed font file " + path + " (" + e.getMessage() + ")", e);
        }
    }

    private String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null)
            throw new ResourceException("malformed font file " + path + " (no " + key + ")");

        return value;
    }

    private String readLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null)
            throw new ResourceException("truncated font file " + path);

        return line;
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
