package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class Text extends Renderer {
    private static final int NUMBER_INDICES = 4;
    private static final int SIZE_INDICES = 6;
    private static final int SIZE_COORDINATES = 8;

    private final WindowInfo windowInfo;
    private float fontSize;
    private final int positionIndex;
    private final int textureIndex;

    private FontFace font;
    private String text;

    private Vector2f textPosition, rectangleSize;
    private EDirection reference;
    private ETextAlignment alignment;

    public Text(WindowInfo windowInfo) {
        super("coloredText", true, true);
        this.windowInfo = windowInfo;

        fontSize = 10.0f;

        this.font = null;
        this.text = "";

        color = new Color4f(0, 0, 0, 1);

        // Null let renderer's ancien setting for text
        this.reference = EDirection.None;

        this.textPosition = new Vector2f(0, 0);
        this.rectangleSize = new Vector2f(0, 0);

        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(new int[0]);

        positionIndex = shape.addVbo(new float[0], 2, Shape.STATIC_STORE);
        textureIndex = shape.addVbo(new float[0], 2, Shape.STATIC_STORE);
    }

    private boolean shouldNotDrawText(){
        return font == null || text == null || text.trim().equals("");
    }

    public void display(){
        if (shouldNotDrawText())
            return;

        font.getTexture().bind();
        shadManager.getShader(shape.getShaderId()).glUniform("color", color.getR(), color.getG(), color.getB(), color.getA());
        super.draw();
    }


    public Text setFont(String fontName){
        this.font = Resources.getInstance().getResource(FontFace.class, fontName);
        switchToTextureMode(font.getTexture());

        computeRightUpPosition();

        return this;
    }

    public Text setText(String text){
        this.text = text;

        this.computeRightUpPosition();

        return this;
    }

    public String text(){
        return this.text;
    }


    public Text setReference(EDirection reference){
        if (reference == null)
            return this;

        this.reference = reference;
        this.computeRightUpPosition();

        return this;
    }

    public EDirection reference() {
        return this.reference;
    }


    public Text setPosition(Vector2f position){
        if (position == null)
            return this;

        this.textPosition = position;
        this.computeRightUpPosition();

        return this;
    }


    public Vector2f position() {
        return this.textPosition;
    }

    public ETextAlignment getAlignment() {
        return alignment;
    }

    public Text setAlignment(ETextAlignment alignment) {
        this.alignment = alignment;
        this.computeRightUpPosition();

        return this;
    }


    public Text setFontSize(float size){
        this.fontSize = size;

        computeRightUpPosition();
        return this;
    }

    public float getFontSize(){
        return this.fontSize;
    }

    public Text setColor(Color4f color){
        this.color = color;
        return this;
    }

    public Color4f getColor(){
        return color;
    }


    private void computeRightUpPosition(){
        if (shouldNotDrawText())
            return;

        rectangleSize.x = 0;
        rectangleSize.y = 0;

        int charNumber = text.replace("\n", "").length();

        int[] indices = new int[charNumber * 6];
        float[] texturePosition = new float[charNumber * 8];
        float[] position = new float[charNumber * 8];

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

        for (int j = 0; j < lines.length; ++j) {
            for (int i = 0; i < lines[j].length(); i++) {
                currentCharOffset.x += font.getFontFile().getCharacter(lines[j].charAt(i)).getxAdvance() * fontSize;
                ++charCount;
            }

            linesSize[j] = currentCharOffset.x;
            rectangleSize.x = Math.max(rectangleSize.x, currentCharOffset.x);
            currentCharOffset.x = 0;
        }

        currentCharOffset.x = 0;
        rectangleSize.y += font.getFontFile().getLineHeight() * fontSize * lines.length;
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

                sizeTemp.x = fontChar.getWidth() * fontSize / windowInfo.getVirtualSizeRef().x * 2.0f - 1.0f;
                sizeTemp.y = fontChar.getHeight() * fontSize / windowInfo.getVirtualSizeRef().y * 2.0f - 1.0f;

                posTemp.x = (currentCharOffset.x + fontChar.getxOffset() + textPosition.x - textReference.x + lineAlignmentOffset)
                        * 2.0f / windowInfo.getVirtualSizeRef().x;
                posTemp.y = (currentCharOffset.y + fontChar.getyOffset() + textPosition.y - textReference.y)
                            * 2.0f / windowInfo.getVirtualSizeRef().y;

                temp.x = -1.0f + posTemp.x;
                temp.y = sizeTemp.x + posTemp.x;
                temp.z = 1.0f - posTemp.y;
                temp.w = -sizeTemp.y - posTemp.y;

                position[charCount * SIZE_COORDINATES] = temp.x;
                position[charCount * SIZE_COORDINATES + 1] = temp.z;

                position[charCount * SIZE_COORDINATES + 2] = temp.x;
                position[charCount * SIZE_COORDINATES + 3] = temp.w;

                position[charCount * SIZE_COORDINATES + 4] = temp.y;
                position[charCount * SIZE_COORDINATES + 5] = temp.w;

                position[charCount * SIZE_COORDINATES + 6] = temp.y;
                position[charCount * SIZE_COORDINATES + 7] = temp.z;

                temp.x = fontChar.getxAtlas();
                temp.y = temp.x + fontChar.getWidthAtlas();
                temp.z = fontChar.getyAtlas();
                temp.w = temp.z + fontChar.getHeightAtlas();

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
        shape.updateVbo(position, positionIndex);
    }

    public Text createCopy(){
        Text text = new Text(windowInfo);
        text.setFont(this.font.getName())
                .setColor(color.copy())
                .setFontSize(fontSize)
                .setPosition(new Vector2f(this.textPosition.x, this.textPosition.y))
                .setReference(this.reference)
                .setText(this.text);

        return text;
    }
}