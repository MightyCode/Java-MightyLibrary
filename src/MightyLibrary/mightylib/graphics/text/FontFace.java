package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import MightyLibrary.mightylib.resources.Resources;

public class FontFace extends DataType {
    private static final String PATH = "resources/textures/fonts/";
    private String name;
    private Texture fontAtlas;
    private FontFile fontFile;

    FontFace(String fontFaceName, String textureName, String infoPath) {
        super(EDataType.Font, fontFaceName, PATH + infoPath);

        name = fontFaceName;
        fontAtlas = Resources.getInstance().getResource(Texture.class, textureName);

        fontFile = new FontFile(PATH + infoPath);

    }



    @Override
    public boolean load() {
        return fontFile.load();
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

    @Override
    public boolean unload() {
        return true;
    }
}
