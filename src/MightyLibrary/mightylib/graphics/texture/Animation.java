package MightyLibrary.mightylib.graphics.texture;

import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.animation.AnimationData;
import MightyLibrary.mightylib.resources.animation.FrameData;
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

    private Texture texture;

    public Animation(){
        this.looping = false;
        this.speed = 1f;

        restart();
    }

    public void init(AnimationData animationData, boolean isLooping){
        this.animationData = animationData;
        this.looping = isLooping;

        this.texture = Resources.getInstance().getResource(Texture.class, animationData.getTextureName());

        restart();
    }


    public void update(){
        if (!animationFinished)
            elapsedTime += GameTime.DeltaTime() * speed;

        while(elapsedTime >= animationData.getFrame(currentFrame).getFrameTime() &&
                (currentFrame <= animationData.frameNumber() || looping)){

            elapsedTime -= animationData.getFrame(currentFrame).getFrameTime();

            if (currentFrame + 1 < animationData.frameNumber()) {
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


    public AnimationData getData(){ return animationData; }


    public Vector4f currentTexturePosition() {
        Vector4f computedPosition = new Vector4f(0, 0, 0, 0);

        FrameData data = animationData.getFrame(currentFrame);
        Vector4i texturePositions = data.getFramePositionCopy();

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
        FrameData data = animationData.getFrame(currentFrame);

        Vector2i result = data.getHotPointCopy();

        if (horizontalFlip)
            result.x = data.getSize().x - result.x;

        if (verticalFlip)
            result.y = data.getSize().y - result.y;

        return result;
    }


    public Vector2i getFrameSize(){
        return animationData.getFrame(currentFrame).getSize();
    }
}
