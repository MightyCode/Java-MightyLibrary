package MightyLibrary.mightylib.graphics.text;

import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;

public class Text {
    private String text;
    private float sizeX;

    private Vector2f position, leftUpPosition;
    private EDirection align;

    Text(){
        this.text = "";

        // Null let renderer's ancien setting for text
        this.align = EDirection.None;

        this.position = new Vector2f(0, 0);
        this.leftUpPosition = new Vector2f(0, 0);
        this.sizeX = 0;
    }


    public void draw(){
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


    public float sizeX(){
        return this.sizeX;
    }


    public Vector2f leftUpPosition(){

        return this.leftUpPosition;
    }

    protected void computeRghtUpPosition(){
        //this.sizeX = textWidth(this.text);

        this.leftUpPosition.x = this.position.x;
        this.leftUpPosition.y = this.position.y;

        switch(this.align){
            case None:
            case Up:
            case Down:
                this.leftUpPosition.x -= this.sizeX / 2;
                break;
            case RightDown:
            case Right:
            case RightUp:
                this.leftUpPosition.x -= this.sizeX;
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
        Text text = new Text();
        text.setText(this.text)
                .setPosition(new Vector2f(this.position.x, this.position.y))
                .setAlign(this.align);

        return text;
    }

}
