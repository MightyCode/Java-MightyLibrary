package io.github.mightycode.mightylib.graphics.text;

import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.Direction;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class Text extends MeshRenderer {
    private static final int NUMBER_INDICES = 4;
    private static final int SIZE_INDICES = 6;
    private static final int SIZE_COORDINATES = 8;

    private float fontSize;
    private final int positionIndex;
    private final int textureIndex;

    private FontFace font;
    private FontFace builtWithFont;
    // Version of the font the characters were placed with (a reloaded font file changes them)
    private int builtWithVersion;
    private String text;
    // Widest line, 0 = no wrapping (setMaxWidth)
    private float maxWidth;

    private final Vector2f leftUpPosition;
    // Text and padding : what the reference and size() are about
    private final Vector2f rectangleSize;
    // Space between the edges of the rectangle and the text : x left, y up, z right, w down
    private final Vector4f padding;

    private EDirection reference;
    private ETextAlignment alignment;

    private float[] charPositions;

    private final ShaderValue color;

    public Text() {
        super(EngineResources.COLORED_TEXT, true);

        fontSize = 10.0f;

        this.font = null;
        this.text = "";

        color = setColorMode(new Color4f(0, 0, 0, 1));

        // Null let renderer's ancien setting for text
        this.reference = EDirection.None;
        this.alignment = ETextAlignment.Left;

        this.rectangleSize = new Vector2f(0, 0);
        this.padding = new Vector4f(0, 0, 0, 0);
        this.leftUpPosition = new Vector2f(0, 0);

        // Batched with the other 2D draws (MeshRenderer.submitToBatch): the batch reads the characters on the CPU
        getMesh().keepCpuData();
        // Dynamic: text content can change every frame (debug info, counters...)
        getMesh().setEboStorage(Mesh.DYNAMIC_STORE);
        getMesh().setEbo(new int[0]);

        positionIndex = getMesh().addVboFloat(new float[0], 2, Mesh.DYNAMIC_STORE);
        textureIndex = getMesh().addVboFloat(new float[0], 2, Mesh.DYNAMIC_STORE);

        // The font of the interface of the engine, until setFont
        setFont(Views.engine().config().getDefaultFont());
    }

    private boolean shouldNotDrawText(){
        return font == null || text == null || text.trim().isEmpty();
    }

    @Override
    public void display(){
        if (shouldNotDrawText())
            return;

        FontFace used = usedFont();
        if (used != builtWithFont || used.getVersion() != builtWithVersion)
            computeRightUpPosition();

        if (charPositions != null && charPositions.length >= SIZE_COORDINATES)
            DrawnTexts.record(text, charPositions[0] + position().x, charPositions[1] + position().y,
                    rectangleSize.x - padding.x - padding.z, rectangleSize.y - padding.y - padding.w, fontSize);

        super.display();
    }


    public Text setFont(String fontName) {
        // check if the font is already loaded
        if (this.font != null && this.font.getFontName().equals(fontName)) {
            return this;
        }

        // Unknown name: the fallback font
        this.font = resources().getResourceOrFallback(FontFace.class, fontName);

        computeRightUpPosition();

        return this;
    }

    public Text setText(String text) {
        if (text == null || text.equals(this.text))
            return this;

        this.text = text;

        this.computeRightUpPosition();

        return this;
    }

    /**
     * Wraps the text at the spaces so that no line is wider than this (padding not counted; a word longer than that
     * is cut), the line feeds of the text are kept. text() is still the text given, the characters keep their index
     * (getPositionOfChar). 0: no wrapping, the default.
     */
    public Text setMaxWidth(float maxWidth) {
        maxWidth = Math.max(0, maxWidth);
        if (maxWidth == this.maxWidth)
            return this;

        this.maxWidth = maxWidth;
        computeRightUpPosition();

        return this;
    }

    public float getMaxWidth() {
        return maxWidth;
    }

    public Text fitText(String text, Vector2f boxSize, boolean removeEscapeChars, String breakLineFirstChar){
        String clean = (removeEscapeChars) ? text.replace("\n", "") : text;

        StringBuilder result = new StringBuilder();
        Vector2f sizeTaken = new Vector2f();

        for (int index = 0; index < clean.length(); ++index){
            char c = clean.charAt(index);
            if (c == '\n'){
                if (sizeTaken.y > boxSize.y){
                    break;
                }

                result.append(c);
            } else {
                result.append(c);
                sizeTaken = usedFont().computeSize(result.toString(), fontSize);

                if (sizeTaken.x > boxSize.x){
                    result.setLength(result.length() - 1);
                    result.append('\n').append(breakLineFirstChar).append(c);
                }
            }
        }

        setText(result.toString());

        return this;
    }

    public String text(){
        return this.text;
    }


    public Text setReference(EDirection reference){
        if (reference == null || reference == this.reference)
            return this;

        this.reference = reference;
        this.computeRightUpPosition();

        return this;
    }

    /**
     * Space kept between the edges of the rectangle and the text. The rectangle (size(), leftUpPosition(), the
     * reference and the position) includes it : a text with a padding is a bigger box, the text inside.
     */
    public Text setPadding(float left, float up, float right, float down) {
        if (left < 0 || up < 0 || right < 0 || down < 0)
            throw new IllegalArgumentException("A padding can't be negative : " + left + ", " + up + ", " + right + ", " + down);

        if (padding.x == left && padding.y == up && padding.z == right && padding.w == down)
            return this;

        padding.set(left, up, right, down);
        computeRightUpPosition();

        return this;
    }

    // A copy : x left, y up, z right, w down
    public Vector4f getPadding() {
        return new Vector4f(padding);
    }

    public EDirection reference() {
        return this.reference;
    }


    public Text setPosition(Vector2f position){
        if (position == null || (position.x == position().x && position.y == position().y))
            return this;

        setPosition(new Vector3f(position.x, position.y, 0.0f));

        return this;
    }

    // The box of the text (leftUpPosition) follows the position
    @Override
    public void setPosition(Vector3f position) {
        super.setPosition(position);
        computeRightUpPosition();
    }

    public Vector2f leftUpPosition() { return this.leftUpPosition; }

    public Vector2f size() { return this.rectangleSize; }

    public ETextAlignment getAlignment() {
        return alignment;
    }

    public Text setAlignment(ETextAlignment alignment) {
        if (alignment == null || alignment == this.alignment)
            return this;

        this.alignment = alignment;
        this.computeRightUpPosition();

        return this;
    }


    public Text setFontSize(float size) {
        if (size == this.fontSize)
            return this;

        this.fontSize = size;

        computeRightUpPosition();
        return this;
    }

    public float getFontSize() {
        return this.fontSize;
    }

    public Text setColor(Color4f color) {
        this.color.setObject(color);
        return this;
    }

    public Color4f getColor(){
        return color.getObjectTyped(Color4f.class);
    }


    // Horizontal advance of a character with the font used now and the font size (cutting long lines, cursors)
    public float charAdvance(char c) {
        return (float) (usedFont().getFontFile().getCharacter(c).getxAdvance() * fontSize);
    }

    // Width of one line (no line break) with the font used now, linear in its length
    public float measureWidth(CharSequence line) {
        FontFile file = usedFont().getFontFile();
        double width = 0;
        for (int i = 0; i < line.length(); ++i)
            width += file.getCharacter(line.charAt(i)).getxAdvance();

        return (float) (width * fontSize);
    }

    // Distance between two lines with the font used now
    public float lineHeight() {
        return (float) (usedFont().getFontFile().getLineHeight() * fontSize);
    }

    // The font if loaded, otherwise the fallback one (failed or still loading)
    private FontFace usedFont() {
        return resources().usable(FontFace.class, font);
    }

    private void computeRightUpPosition() {
        if (shouldNotDrawText()) {
            emptyBox();
            return;
        }

        // Built with this font: rebuilt by display when the usable font changes (loaded, failed, reloaded)
        FontFace used = usedFont();
        if (used != builtWithFont) {
            builtWithFont = used;
            setMainTextureChannel(used.getTexture().getDataName());
        }
        builtWithVersion = used.getVersion();

        leftUpPosition.x = position().x;
        leftUpPosition.y = position().y;

        // The line feeds added by the wrapping are not characters: the index of each one is the one of text
        String laidOut = TextWrap.Wrap(text, maxWidth, this::measureWidth);

        int charNumber = laidOut.replace("\n", "").length();

        int[] indices = new int[charNumber * 6];
        float[] texturePosition = new float[charNumber * 8];
        charPositions = new float[charNumber * 8];

        String[] lines = laidOut.split("\n");
        float[] linesSize = new float[lines.length];

        FontChar fontChar;
        Vector4f currentCharOffset = new Vector4f();

        Vector4f temp = new Vector4f();

        // Offset of the left top corner of the rectangle from the position
        Vector2f referenceOffset = new Vector2f();
        float lineAlignmentOffset = 0;

        float widestLine = 0;
        for (int j = 0; j < lines.length; ++j) {
            // A wrapped text: the spaces at the end of a line take no room (alignment, size), like in a text editor
            int measured = maxWidth > 0 ? TextWrap.TrimmedLength(lines[j]) : lines[j].length();
            for (int i = 0; i < lines[j].length(); i++) {
                if (i < measured)
                    currentCharOffset.x += (float) (usedFont().getFontFile().getCharacter(lines[j].charAt(i)).getxAdvance() * fontSize);
            }

            linesSize[j] = currentCharOffset.x;
            widestLine = Math.max(widestLine, linesSize[j]);
            currentCharOffset.x = 0;
        }

        rectangleSize.set(usedFont().computeSize(laidOut, fontSize));
        if (maxWidth > 0)
            rectangleSize.x = widestLine;
        float textWidth = rectangleSize.x;
        rectangleSize.add(padding.x + padding.z, padding.y + padding.w);

        currentCharOffset.x = 0;
        int charCount = 0;

        Direction.AnchorOffset(this.reference, rectangleSize.x, rectangleSize.y, referenceOffset);
        leftUpPosition.add(referenceOffset);

        for (int j = 0; j < lines.length; ++j) {
            switch(this.alignment){
                case Center:
                    lineAlignmentOffset = (textWidth - linesSize[j]) / 2;
                    break;
                case Right:
                    lineAlignmentOffset = textWidth - linesSize[j];
                    break;
                default:
                    // Nothing to do for the other values
                    break;
            }

            for (int i = 0; i < lines[j].length(); i++) {
                char c = lines[j].charAt(i);

                indices[charCount * SIZE_INDICES] =     charCount * NUMBER_INDICES;
                indices[charCount * SIZE_INDICES + 1] = charCount * NUMBER_INDICES + 1;
                indices[charCount * SIZE_INDICES + 2] = charCount * NUMBER_INDICES + 2;
                indices[charCount * SIZE_INDICES + 3] = charCount * NUMBER_INDICES + 2;
                indices[charCount * SIZE_INDICES + 4] = charCount * NUMBER_INDICES;
                indices[charCount * SIZE_INDICES + 5] = charCount * NUMBER_INDICES + 3;

                fontChar = usedFont().getFontFile().getCharacter(c);

                // Left, right, top, bottom of the character, its pen at the left top
                TextLayout.CharacterQuad(fontChar, fontSize,
                        currentCharOffset.x + referenceOffset.x + lineAlignmentOffset + padding.x,
                        currentCharOffset.y + referenceOffset.y + padding.y, temp);

                charPositions[charCount * SIZE_COORDINATES] = temp.x;
                charPositions[charCount * SIZE_COORDINATES + 1] = temp.z;

                charPositions[charCount * SIZE_COORDINATES + 2] = temp.x;
                charPositions[charCount * SIZE_COORDINATES + 3] = temp.w;

                charPositions[charCount * SIZE_COORDINATES + 4] = temp.y;
                charPositions[charCount * SIZE_COORDINATES + 5] = temp.w;

                charPositions[charCount * SIZE_COORDINATES + 6] = temp.y;
                charPositions[charCount * SIZE_COORDINATES + 7] = temp.z;

                usedFont().getFontFile().textureBounds(fontChar, temp);

                texturePosition[charCount * SIZE_COORDINATES] = temp.x;
                texturePosition[charCount * SIZE_COORDINATES + 1] = temp.z;

                texturePosition[charCount * SIZE_COORDINATES + 2] = temp.x;
                texturePosition[charCount * SIZE_COORDINATES + 3] = temp.w;

                texturePosition[charCount * SIZE_COORDINATES + 4] = temp.y;
                texturePosition[charCount * SIZE_COORDINATES + 5] = temp.w;

                texturePosition[charCount * SIZE_COORDINATES + 6] = temp.y;
                texturePosition[charCount * SIZE_COORDINATES + 7] = temp.z;

                currentCharOffset.x += (float) (fontChar.getxAdvance() * fontSize);

                ++charCount;
            }

            currentCharOffset.x = 0;
            currentCharOffset.y += (float) (usedFont().getFontFile().getLineHeight() * fontSize);
        }

        getMesh().setEbo(indices);
        getMesh().updateVbo(texturePosition, textureIndex);
        getMesh().updateVbo(charPositions, positionIndex);
    }

    // Nothing drawn (no font yet, an empty or blank text): the box is the padding alone at its place, not the box of
    // the previous text (a GUI would still find the mouse over it)
    private void emptyBox() {
        rectangleSize.set(padding.x + padding.z, padding.y + padding.w);
        leftUpPosition.set(position().x, position().y);
        leftUpPosition.add(Direction.AnchorOffset(reference, rectangleSize.x, rectangleSize.y, new Vector2f()));
    }

    // X Y Z W
    public Vector4f getPositionOfChar(int index){
        return new Vector4f(
                charPositions[index * SIZE_COORDINATES] + position().x,
                charPositions[index * SIZE_COORDINATES + 4] + position().x,
                charPositions[index * SIZE_COORDINATES + 1] + position().y,
                charPositions[index * SIZE_COORDINATES + 3] + position().y);
    }

    public Text createCopy(){
        Text text = new Text();
        text.setFont(this.font.getFontName())
                .setColor(getColor().copy())
                .setFontSize(fontSize)
                .setPadding(padding.x, padding.y, padding.z, padding.w)
                .setPosition(new Vector2f(position().x, position().y))
                .setAlignment(alignment)
                .setReference(this.reference)
                .setMaxWidth(maxWidth)
                .setText(this.text);

        text.setReferenceCamera(getReferenceCamera());

        return text;
    }

    public Text copyTo(Text copy){
        copy.setText("")
                .setFont(this.font.getFontName())
                .setColor(getColor().copy())
                .setFontSize(fontSize)
                .setPadding(padding.x, padding.y, padding.z, padding.w)
                .setPosition(new Vector2f(position().x, position().y))
                .setAlignment(alignment)
                .setReference(this.reference)
                .setMaxWidth(maxWidth)
                .setText(this.text);

        copy.setReferenceCamera(getReferenceCamera());

        return copy;
    }

    public Text copy() {
        return copyTo(new Text());
    }
}
