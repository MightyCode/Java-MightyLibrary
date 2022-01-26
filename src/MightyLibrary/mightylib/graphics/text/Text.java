package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class Text extends Renderer {
    private WindowInfo windowInfo;
    private float scale;
    private float computedW, computedH;

    private FontFace font;
    private String text;
    private float size;

    private Vector2f position, leftUpPosition;
    private EDirection align;


    private List<Vector2f> mesh;
    private List<Vector2f> textures;
    private int[] indices;

    public Text(WindowInfo windowInfo) {
        super("texture2D", true, true);
        this.windowInfo = windowInfo;

        scale = 1.0f;

        this.font = null;
        this.text = "";

        // Null let renderer's ancien setting for text
        this.align = EDirection.None;

        this.position = new Vector2f(0, 0);
        this.leftUpPosition = new Vector2f(0, 0);
        this.size = 0;

        mesh = new ArrayList<>();
        textures = new ArrayList<>();

        indices = { 0, 1, 2, 2, 0, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(calculatePosition(), 2, Shape.STATIC_STORE);
        textureIndex = shape.addVbo(texturePos(), 2, Shape.STATIC_STORE);
    }


    public void draw(){
        if (font == null)
            return;

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

        computeRghtUpPosition();

        return this;
    }

    public Text setText(String text){
        this.text = text;

        this.computeRghtUpPosition();

        return this;
    }

    public String text(){
        return this.text;
    }


    public Text setAlign(EDirection align){
        if (align == null)
            return this;

        this.align = align;
        this.computeRghtUpPosition();

        return this;
    }

    public EDirection align() {
        return this.align;
    }


    public Text setPosition(Vector2f position){
        if (position == null)
            return this;

        this.position = position;
        this.computeRghtUpPosition();

        return this;
    }


    public Vector2f position() {
        return this.position;
    }


    public float size(){
        return this.size;
    }


    public Vector2f leftUpPosition(){

        return this.leftUpPosition;
    }

    private void computeRghtUpPosition(){
        if (this.font == null)
            return;

        mesh.clear();
        textures.clear();

        String[] lines = text.split("\n");

        float lineY = 0;
        float currentX = 0;

        for (String line : lines) {
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                FontChar fontChar = font.getFontFile().getCharacter((int) c);

                mesh.add(new Vector2f(
                        currentX + fontChar.getxOffset(),
                        lineY + fontChar.getyOffset()
                ));
                mesh.add(new Vector2f(
                        currentX + fontChar.getxOffset() + fontChar.getWidth(),
                        lineY + fontChar.getyOffset()
                ));
                mesh.add(new Vector2f(
                        currentX + fontChar.getxOffset() + fontChar.getWidth(),
                        lineY + fontChar.getyOffset() + fontChar.getHeight()
                ));
                mesh.add(new Vector2f(
                        currentX + fontChar.getxOffset(),
                        lineY + fontChar.getyOffset() + fontChar.getHeight()
                ));

                textures.add(new Vector2f(
                        fontChar.getxAtlas(),
                        fontChar.getyAtlas()
                ));
                textures.add(new Vector2f(
                        fontChar.getxAtlas() + fontChar.getWidthAtlas(),
                        fontChar.getyAtlas()
                ));
                textures.add(new Vector2f(
                        fontChar.getxAtlas() + fontChar.getWidthAtlas(),
                        fontChar.getyAtlas() + fontChar.getHeightAtlas()
                ));
                textures.add(new Vector2f(
                        fontChar.getxAtlas(),
                        fontChar.getyAtlas() + fontChar.getHeightAtlas()
                ));

                currentX += fontChar.getxAdvance();
            }

            size = Math.max(0, currentX);

            currentX = 0;
            lineY += font.getFontFile().getLineHeight();
        }


        //this.sizeX = textWidth(this.text);


        this.leftUpPosition.x = this.position.x;
        this.leftUpPosition.y = this.position.y;

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
        text.setFont("arial")
                .setText(this.text)
                .setPosition(new Vector2f(this.position.x, this.position.y))
                .setAlign(this.align);

        return text;
    }

}
