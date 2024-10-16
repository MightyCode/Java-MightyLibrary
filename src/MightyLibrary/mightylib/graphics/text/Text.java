package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.graphics.shader.ShaderValue;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Text extends Renderer implements Cloneable {
    private static final int NUMBER_INDICES = 4;
    private static final int SIZE_INDICES = 6;
    private static final int SIZE_COORDINATES = 8;

    private float fontSize;
    private final int positionIndex;
    private final int textureIndex;

    private FontFace font;
    private String text;

    private final Vector2f leftUpPosition;
    private final Vector2f rectangleSize;

    private EDirection reference;
    private ETextAlignment alignment;

    private float[] charPositions;

    private final ShaderValue color;

    public Text() {
        super("coloredText", true);

        fontSize = 10.0f;

        this.font = null;
        this.text = "";

        color = setColorMode(new Color4f(0, 0, 0, 1));

        // Null let renderer's ancien setting for text
        this.reference = EDirection.None;
        this.alignment = ETextAlignment.Left;

        this.rectangleSize = new Vector2f(0, 0);
        this.leftUpPosition = new Vector2f(0, 0);

        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(new int[0]);

        positionIndex = shape.addVboFloat(new float[0], 2, Shape.STATIC_STORE);
        textureIndex = shape.addVboFloat(new float[0], 2, Shape.STATIC_STORE);
    }

    private boolean shouldNotDrawText(){
        return font == null || text == null || text.trim().equals("");
    }

    public void display(){
        if (shouldNotDrawText())
            return;

        super.display();
    }


    public Text setFont(String fontName) {
        // check if the font is already loaded
        if (this.font != null && this.font.getFontName().equals(fontName)) {
            return this;
        }

        this.font = Resources.getInstance().getResource(FontFace.class, fontName);

        setMainTextureChannel(fontName);

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

    public Text fitText(String text, Vector2f boxSize, boolean removeEscapeChars, String breakLineFirstChar){
        String clean = (removeEscapeChars) ? text.replace("\n", "") : text;

        String result = "";
        String temp = "";

        Vector2f sizeTaken = new Vector2f();

        for (char c : clean.toCharArray()){
            temp = result + c;

            if (c == '\n'){
                if (sizeTaken.y > boxSize.y){
                    break;
                }

                result = temp;
            } else {
                sizeTaken = font.computeSize(temp, fontSize);

                if (sizeTaken.x > boxSize.x){
                    result = result + "\n" + breakLineFirstChar + c;
                } else {
                    result = temp;
                }
            }
        }

        setText(result);

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

    public EDirection reference() {
        return this.reference;
    }


    public Text setPosition(Vector2f position){
        if (position == null || position.x == this.position.x && position.y == this.position.y)
            return this;

        super.setPosition(new Vector3f(position.x, position.y, 0.0f));

        return this;
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


    private void computeRightUpPosition() {
        if (shouldNotDrawText())
            return;

        leftUpPosition.x = position.x;
        leftUpPosition.y = position.y;

        int charNumber = text.replace("\n", "").length();

        int[] indices = new int[charNumber * 6];
        float[] texturePosition = new float[charNumber * 8];
        charPositions = new float[charNumber * 8];

        String[] lines = text.split("\n");
        float[] linesSize = new float[lines.length];

        int charCount = 0;

        FontChar fontChar;
        Vector4f currentCharOffset = new Vector4f();

        Vector2f sizeTemp = new Vector2f();
        Vector2f posTemp = new Vector2f();
        Vector4f temp = new Vector4f();

        Vector2f textReference = new Vector2f();
        float lineAlignmentOffset = 0;

        rectangleSize.set(font.computeSize(text, fontSize));

        for (int j = 0; j < lines.length; ++j) {
            for (int i = 0; i < lines[j].length(); i++) {
                currentCharOffset.x += font.getFontFile().getCharacter(lines[j].charAt(i)).getxAdvance() * fontSize;
                ++charCount;
            }

            linesSize[j] = currentCharOffset.x;
            currentCharOffset.x = 0;
        }

        currentCharOffset.x = 0;
        charCount = 0;

        switch(this.reference){
            case None:
            case Up:
            case Down:
                textReference.x = rectangleSize.x / 2;
                break;
            case RightDown:
            case Right:
            case RightUp:
                textReference.x = rectangleSize.x;
                break;
        }

        switch(this.reference){
            case None:
            case Left:
            case Right:
                textReference.y = rectangleSize.y / 2;
                break;
            case LeftDown:
            case Down:
            case RightDown:
                textReference.y = rectangleSize.y;
                break;
        }

        leftUpPosition.x -= textReference.x;
        leftUpPosition.y -= textReference.y;

        for (int j = 0; j < lines.length; ++j) {
            switch(this.alignment){
                case Center:
                    lineAlignmentOffset = (rectangleSize.x - linesSize[j]) / 2;
                    break;
                case Right:
                    lineAlignmentOffset = rectangleSize.x - linesSize[j];
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

                fontChar = font.getFontFile().getCharacter(c);

                sizeTemp.x = (float)((fontChar.getWidth()) * fontSize);
                sizeTemp.y = (float)((fontChar.getHeight()) * fontSize);

                posTemp.x = currentCharOffset.x /*+ fontChar.getxOffset() * fontSize*/ - textReference.x + lineAlignmentOffset;
                posTemp.y = currentCharOffset.y /*+ fontChar.getyOffset() * fontSize*/ - textReference.y;

                temp.x = (posTemp.x);
                temp.y = (sizeTemp.x + posTemp.x);
                temp.z = (posTemp.y);
                temp.w = (sizeTemp.y + posTemp.y);

                charPositions[charCount * SIZE_COORDINATES] = temp.x;
                charPositions[charCount * SIZE_COORDINATES + 1] = temp.z;

                charPositions[charCount * SIZE_COORDINATES + 2] = temp.x;
                charPositions[charCount * SIZE_COORDINATES + 3] = temp.w;

                charPositions[charCount * SIZE_COORDINATES + 4] = temp.y;
                charPositions[charCount * SIZE_COORDINATES + 5] = temp.w;

                charPositions[charCount * SIZE_COORDINATES + 6] = temp.y;
                charPositions[charCount * SIZE_COORDINATES + 7] = temp.z;

                temp.x = (float)(fontChar.getxAtlas());
                temp.y = temp.x + (float)(fontChar.getWidthAtlas());
                temp.z = (float)(fontChar.getyAtlas());
                temp.w = temp.z + (float)(fontChar.getHeightAtlas());

                texturePosition[charCount * SIZE_COORDINATES] = temp.x;
                texturePosition[charCount * SIZE_COORDINATES + 1] = temp.z;

                texturePosition[charCount * SIZE_COORDINATES + 2] = temp.x;
                texturePosition[charCount * SIZE_COORDINATES + 3] = temp.w;

                texturePosition[charCount * SIZE_COORDINATES + 4] = temp.y;
                texturePosition[charCount * SIZE_COORDINATES + 5] = temp.w;

                texturePosition[charCount * SIZE_COORDINATES + 6] = temp.y;
                texturePosition[charCount * SIZE_COORDINATES + 7] = temp.z;

                currentCharOffset.x += fontChar.getxAdvance() * fontSize;

                ++charCount;
            }

            currentCharOffset.x = 0;
            currentCharOffset.y += font.getFontFile().getLineHeight() * fontSize;
        }

        shape.setEbo(indices);
        shape.updateVbo(texturePosition, textureIndex);
        shape.updateVbo(charPositions, positionIndex);
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
                .setPosition(new Vector2f(this.position.x, this.position.y))
                .setAlignment(alignment)
                .setReference(this.reference)
                .setText(this.text);

        text.setReferenceCamera(referenceCamera);

        return text;
    }

    public Text copyTo(Text copy){
        copy.setText("")
                .setFont(this.font.getFontName())
                .setColor(getColor().copy())
                .setFontSize(fontSize)
                .setPosition(new Vector2f(this.position.x, this.position.y))
                .setAlignment(alignment)
                .setReference(this.reference)
                .setText(this.text);

        copy.setReferenceCamera(referenceCamera);

        return copy;
    }

    @Override
    public Text clone() {
        return copyTo(new Text());
    }

    @Override
    public void unload(int remainingMilliseconds) {
        super.unload(remainingMilliseconds);
    }
}
