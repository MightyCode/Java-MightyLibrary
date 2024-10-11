package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.resources.SingleSourceDataType;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.TextureData;
import org.joml.Vector2f;

public class FontFace extends SingleSourceDataType {
    private static final String PATH = "resources/textures/fonts/";
    private final String name;
    private final TextureData fontAtlas;
    private final FontFile fontFile;

    FontFace(String fontFaceName, String textureName, String infoPath) {
        super(TYPE_SET_UP.THREAD_CONTEXT, fontFaceName, PATH + infoPath);

        name = fontFaceName;
        fontAtlas = Resources.getInstance().getResource(TextureData.class, textureName);
        addDependency(fontAtlas);

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

    public String getFontName(){
        return name;
    }

    /**
     * Get font atlas texture.
     *
     * @return fontAtlas.
     */
    public TextureData getTexture() {
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
    protected boolean internLoad() { return true; }

    @Override
    public void internUnload() {}
}
