package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.Resources;

public class FontFace extends DataType {
    private static final String PATH = "resources/textures/fonts/";
    private final String name;
    private final Texture fontAtlas;
    private final FontFile fontFile;

    FontFace(String fontFaceName, String textureName, String infoPath) {
        super(fontFaceName, PATH + infoPath);

        name = fontFaceName;
        fontAtlas = Resources.getInstance().getResource(Texture.class, textureName);

        fontFile = new FontFile(PATH + infoPath);
    }


    public String getName(){
        return name;
    }

    /**
     * Get font atlas texture.
     *
     * @return fontAtlas.
     */
    public Texture getTexture() {
        return fontAtlas;
    }

    /**
     * Get font file.
     *
     * @return fontFile.
     */
    public FontFile getFontFile() {
        return fontFile;
    }

    void setCorrectlyLoaded() { correctlyLoaded = true; }

    @Override
    public void unload() {
        correctlyLoaded = false;
    }
}
