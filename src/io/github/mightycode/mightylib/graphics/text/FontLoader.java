package io.github.mightycode.mightylib.graphics.text;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.FallbackTextureData;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

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

    /**
     * Each character is a magenta and black box. The font shader reads a rectangle texture: the fallback font has its
     * own checkerboard of this type, owned for the whole program (the TextureData loader must be registered first).
     */
    @Override
    public DataType createFallback() {
        TextureData atlas = resources().addOwnedResource(TextureData.class,
                new FallbackTextureData(FALLBACK_ATLAS_NAME, GL_TEXTURE_RECTANGLE), "fallback font");

        return new FontFace(atlas, FontFile.CreateBoxes(FallbackTextureData.SIZE));
    }

    private static final String FALLBACK_ATLAS_NAME = "error-font";

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
                data.put(currentNode, new FontFace(resources(), currentNode, values.getString("texture"), values.getString("info")));

        } while(arrayNodes.hasNext());
    }


    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof FontFace))
            return;

        FontFace fontFace = (FontFace) dataType;
        // Failing here makes the preload fail: the fallback replaces the font
        fontFace.getFontFile().load();
    }
}
