package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.joml.Vector4i;

public class Animation {
    private AnimationData animationData;

    private float elapsedTime;
    private float speed;

    private int lastFrame;
    private int currentFrame;
    private boolean looping;
    private boolean animationFinished;

    // Data given by the last data() call
    private AnimationData usedData;

    public Animation(){
        this.looping = false;
        this.speed = 1f;

        restart();
    }

    // Null data (unknown name): the fallback animation
    public void init(AnimationData animationData, boolean isLooping){
        this.animationData = animationData;
        this.looping = isLooping;

        restart();
    }

    // The animation if loaded, otherwise the fallback one (failed or still loading); restarted when it changes
    private AnimationData data() {
        AnimationData usable = Resources.getInstance().usable(AnimationData.class, animationData);
        if (usable != usedData) {
            usedData = usable;
            restart();
        }

        return usable;
    }

    private TextureData texture() {
        TextureData texture = Resources.getInstance().getResourceOrFallback(TextureData.class, data().getTextureName());
        return Resources.getInstance().usable(TextureData.class, texture);
    }


    public void update(){
        if (!animationFinished)
            elapsedTime += GameTime.DeltaTime() * speed;

        while(elapsedTime >= data().getFrame(currentFrame).getFrameTime() &&
                (currentFrame <= data().frameNumber() || looping)){

            elapsedTime -= data().getFrame(currentFrame).getFrameTime();

            if (currentFrame + 1 < data().frameNumber()) {
                ++currentFrame;
            } else {
                if (looping)
                    restart();
                else
                    animationFinished = true;
            }
        }
    }

    public void lateUpdate(){
        lastFrame = currentFrame;
    }

    public void restart(){
        elapsedTime = 0;
        animationFinished = false;
        currentFrame = 0;
        lastFrame = -1;
    }


    public boolean isCurrentFrameChanged(){ return lastFrame != currentFrame; }


    public boolean isAnimationFinished(){
        return animationFinished;
    }


    public void setAnimationSpeed(float newSpeed){
        speed = newSpeed;
    }
    public float getAnimationSpeed(){
        return speed;
    }


    // The usable data: the fallback one while the animation is not loaded
    public AnimationData getData(){ return data(); }


    public Vector4f currentTexturePosition() {
        Vector4f computedPosition = new Vector4f(0, 0, 0, 0);

        FrameData data = data().getFrame(currentFrame);
        Vector4i texturePositions = data.getFramePositionCopy();
        TextureData texture = texture();

        computedPosition.x = (texturePositions.x) / (texture.getWidth() * 1.0f);
        computedPosition.y = (texturePositions.z + texturePositions.x) / (texture.getWidth() * 1.0f);
        computedPosition.z = (texturePositions.y) / (texture.getHeight() * 1.0f);
        computedPosition.w = (texturePositions.w + texturePositions.y) / (texture.getHeight() * 1.0f);

        return computedPosition;
    }


    public Vector2i currentHotPoint() {
        return currentHotPoint(false, false);
    }

    public Vector2i currentHotPoint(boolean horizontalFlip, boolean verticalFlip){
        FrameData data = data().getFrame(currentFrame);

        Vector2i result = data.getHotPointCopy();

        if (horizontalFlip)
            result.x = data.getSize().x - result.x;

        if (verticalFlip)
            result.y = data.getSize().y - result.y;

        return result;
    }


    public Vector2i getFrameSize(){
        return data().getFrame(currentFrame).getSize();
    }
}
