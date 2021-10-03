package MightyLibrary.mightylib.graphics.texture;

import MightyLibrary.mightylib.main.GameTime;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;

public class Animation {
    private final Vector2f referencePoint;

    AnimationData animationData;

    private float elapsedTime;
    private float speed;
    private float scale;

    private int currentFrame;
    private boolean looping;
    private boolean animationFinished;

    public Animation(Vector2f referencePoint){
        this.referencePoint = referencePoint;

        this.looping = false;
        this.speed = 1f;
        this.scale = 1f;

        restart();
    }

    public void init(final AnimationData animationData, final boolean isLooping){
        this.animationData = animationData;
        this.looping = isLooping;
    }


    public void update(){
        if (!animationFinished)
            elapsedTime += GameTime.DeltaTime() * speed;

        while(elapsedTime >= animationData.getFrame(currentFrame).getFrameTime() &&
                (currentFrame <= animationData.frameNumber() || looping)){

            elapsedTime -= animationData.getFrame(currentFrame).getFrameTime();

            if (currentFrame + 1 < animationData.frameNumber())
                ++currentFrame;
            else {
                if (looping)
                    currentFrame = 0;
                else
                    animationFinished = true;
            }
        }
    }


    public void restart(){
        elapsedTime = 0;
        animationFinished = false;
        currentFrame = 0;
    }


    public boolean isAnimationFinished(){
        return animationFinished;
    }


    public void setAnimationSpeed(float newSpeed){
        speed = newSpeed;
    }
    public float getAnimationSpeed(){
        return speed;
    }


    public void setAnimationScale(float newScale){
        scale = newScale;
    }
    public float getAnimationScale(){
        return scale;
    }
}
