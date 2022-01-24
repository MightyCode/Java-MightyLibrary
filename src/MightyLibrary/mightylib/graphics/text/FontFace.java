package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.graphics.texture.Texture;

public class FontFace {
    Texture fontAtlas;

    FontFile fontFile;

    public FontFace(String name) {
        /*
        fontAtlas = new Texture("/fonts/" + name + ".png");

        fontFile = new FontFile("/fonts/" + name + ".fnt");*/
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
}
