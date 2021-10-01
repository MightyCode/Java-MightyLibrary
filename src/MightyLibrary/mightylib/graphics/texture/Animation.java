package MightyLibrary.mightylib.graphics.texture;

import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;

public class Animation {
    private Vector2f referencePoint;
    private Texture texture;

    private ArrayList<Float> framesTime;
    private Vector4f framesPosition;
    private ArrayList<Vector2f> framesOffset;

    private int frameNumber;

    private float elapsedTime;
    private float speed;
    private float scale;

    private int currentFrame;
    private boolean looping;
    private boolean animationFinished;

    public Animation(Vector2f centerPoint){
        this.referencePoint = centerPoint;
        this.texture = null;

        this.frameNumber = 0;
        this.looping = false;
        this.speed = 1f;
        this.scale = 1f;

        restart();
    }


    public void restart(){

    }


}
