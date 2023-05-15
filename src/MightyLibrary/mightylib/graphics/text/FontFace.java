package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.Resources;
import org.joml.Vector2f;
import org.joml.Vector4f;

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

    /**
     * Compute the size of given text
     * @param text Text's size to compute
     * @param fontSize Size of the font
     *
     * @return A vector representing the size of the text (in px)
     */
    public Vector2f computeSize(String text, float fontSize){
        Vector2f result = new Vector2f();
        Vector2f currentCharOffset = new Vector2f();

        char chr;
        int numberLine = 1;

        for (int i = 0; i < text.length(); i++) {
            chr = text.charAt(i);

            if (chr == '\n'){
                result.x = Math.max(result.x, currentCharOffset.x);
                currentCharOffset.x = 0;
                ++numberLine;
            } else {
                currentCharOffset.x += getFontFile().getCharacter(chr).getxAdvance() * fontSize;
            }
        }

        result.x = Math.max(result.x, currentCharOffset.x);

        result.y += getFontFile().getLineHeight() * fontSize * numberLine;

        return result;
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
