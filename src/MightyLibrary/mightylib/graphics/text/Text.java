package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class Text extends Renderer {
    private static final int NUMBER_INDICES = 4;
    private static final int SIZE_INDICES = 6;
    private static final int SIZE_COORDONATES = 8;


    private WindowInfo windowInfo;
    private float fontSize;
    private float computedW, computedH;
    private final int positionIndex;
    private final int textureIndex;

    private FontFace font;
    private String text;
    private float size;

    private Vector2f textPosition, leftUpPosition;
    private EDirection align;

    public Text(WindowInfo windowInfo) {
        super("texture2D", true, true);
        this.windowInfo = windowInfo;

        fontSize = 10.0f;

        this.font = null;
        this.text = "";

        // Null let renderer's ancien setting for text
        this.align = EDirection.None;

        this.textPosition = new Vector2f(0, 0);
        this.leftUpPosition = new Vector2f(0, 0);
        this.size = 0;

        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(new int[0]);

        positionIndex = shape.addVbo(new float[0], 2, Shape.DYNAMIC_STORE);
        textureIndex = shape.addVbo(new float[0], 2, Shape.DYNAMIC_STORE);
    }


    public void display(){
        if (font == null || text == null || text.trim().equals(""))
            return;

        font.getTexture().bind();
        super.draw();

        /*if (this.strokeColor == -1){
            renderer.noStroke();
        } else {
            renderer.stroke(this.strokeColor);
        }

        if (this.textColor != -1){
            renderer.fill(this.textColor);
        }

        if (this.fontSize != -1){
            renderer.textSize(this.fontSize);
        }*/

        switch(this.align){
            case None:

                break;
            case Up:

                break;
            case LeftUp:

                break;
            case Left:

                break;
            case LeftDown:

                break;
            case Down:

                break;
            case RightDown:

                break;
            case Right:

                break;
            case RightUp:

                break;
        }
    }


    public Text setFont(String fontName){
        this.font = Resources.getInstance().getResource(FontFace.class, fontName);
        setTexture(font.getTexture());

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


    public Text setAlign(EDirection align){
        if (align == null)
            return this;

        this.align = align;
        this.computeRightUpPosition();

        return this;
    }

    public EDirection align() {
        return this.align;
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


    public float size(){
        return this.size;
    }

    public Text setFontSize(float size){
        this.fontSize = size;

        computeRightUpPosition();
        return this;
    }

    public float getFontSize(){
        return this.fontSize;
    }


    public Vector2f leftUpPosition(){

        return this.leftUpPosition;
    }

    private void computeRightUpPosition(){
        if (this.font == null)
            return;

        int charNumber = text.replace("\n", "").length();

        int[] indices = new int[charNumber * 6];
        float[] texturePosition = new float[charNumber * 8];
        float[] position = new float[charNumber * 8];

        String[] lines = text.split("\n");

        float lineY = 0;
        float currentX = 0;
        int charCount = 0;

        Vector2f sizeTemp = new Vector2f();
        Vector2f posTemp = new Vector2f();
        Vector4f temp = new Vector4f();

        for (String line : lines) {
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                indices[charCount * SIZE_INDICES] =     i * NUMBER_INDICES;
                indices[charCount * SIZE_INDICES + 1] = i * NUMBER_INDICES + 1;
                indices[charCount * SIZE_INDICES + 2] = i * NUMBER_INDICES + 2;
                indices[charCount * SIZE_INDICES + 3] = i * NUMBER_INDICES + 2;
                indices[charCount * SIZE_INDICES + 4] = i * NUMBER_INDICES ;
                indices[charCount * SIZE_INDICES + 5] = i * NUMBER_INDICES + 3;

                FontChar fontChar = font.getFontFile().getCharacter(c);

                sizeTemp.x = fontChar.getWidth() * fontSize / windowInfo.getVirtualSizeRef().x * 2.0f - 1.0f;

                sizeTemp.y = fontChar.getHeight() * fontSize / windowInfo.getVirtualSizeRef().y * 2.0f - 1.0f;

                posTemp.x = (currentX + fontChar.getxOffset() + textPosition.x) * 2.0f / windowInfo.getVirtualSizeRef().x;
                posTemp.y = (lineY + fontChar.getyOffset() + textPosition.y) * 2.0f / windowInfo.getVirtualSizeRef().y;

                System.out.println(sizeTemp.y + " " + posTemp.y);

                temp.x = -1.0f + posTemp.x;
                temp.y = sizeTemp.x + posTemp.x;
                temp.z = 1.0f - posTemp.y;
                temp.w = -sizeTemp.y - posTemp.y;

                position[charCount * SIZE_COORDONATES] = temp.x;
                position[charCount * SIZE_COORDONATES + 1] = temp.z;

                position[charCount * SIZE_COORDONATES + 2] = temp.x;
                position[charCount * SIZE_COORDONATES + 3] = temp.w;

                position[charCount * SIZE_COORDONATES + 4] = temp.y;
                position[charCount * SIZE_COORDONATES + 5] = temp.w;

                position[charCount * SIZE_COORDONATES + 6] = temp.y;
                position[charCount * SIZE_COORDONATES + 7] = temp.z;

                temp.x = fontChar.getxAtlas();
                temp.y = temp.x + fontChar.getWidthAtlas();
                temp.z = fontChar.getyAtlas();
                temp.w = temp.z + fontChar.getHeightAtlas();

                texturePosition[charCount * SIZE_COORDONATES] = temp.x;
                texturePosition[charCount * SIZE_COORDONATES + 1] = temp.z;

                texturePosition[charCount * SIZE_COORDONATES + 2] = temp.x;
                texturePosition[charCount * SIZE_COORDONATES + 3] = temp.w;

                texturePosition[charCount * SIZE_COORDONATES + 4] = temp.y;
                texturePosition[charCount * SIZE_COORDONATES + 5] = temp.w;

                texturePosition[charCount * SIZE_COORDONATES + 6] = temp.y;
                texturePosition[charCount * SIZE_COORDONATES + 7] = temp.z;

                currentX += fontChar.getxAdvance() * fontSize;

                ++charCount;
            }

            size = Math.max(0, currentX);

            currentX = 0;
            lineY += font.getFontFile().getLineHeight() * fontSize;
        }

        shape.setEbo(indices);
        shape.updateVbo(texturePosition, textureIndex);
        shape.updateVbo(position, positionIndex);

        this.leftUpPosition.x = this.textPosition.x;
        this.leftUpPosition.y = this.textPosition.y;

        switch(this.align){
            case None:
            case Up:
            case Down:
                this.leftUpPosition.x -= this.size / 2;
                break;
            case RightDown:
            case Right:
            case RightUp:
                this.leftUpPosition.x -= this.size;
                break;
        }

        switch(this.align){
            case None:
            case Left:
            case Right:
                //this.leftUpPosition.y -= this.fontSize / 2;
                break;
            case LeftDown:
            case Down:
            case RightDown:
                //this.leftUpPosition.y -= this.fontSize;
                break;
        }
    }


    public Text createCopy(){
        Text text = new Text(windowInfo);
        text.setFont(this.font.getName())
                .setFontSize(fontSize)
                .setPosition(new Vector2f(this.textPosition.x, this.textPosition.y))
                .setAlign(this.align)
                .setText(this.text);

        return text;
    }
}
