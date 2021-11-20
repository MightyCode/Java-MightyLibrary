package MightyLibrary.mightylib.graphics.texture;

import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.resources.Resources;
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


    public Vector4f currentTexturePosition(){
        Vector4f computedPosition = new Vector4f(0, 0, 0, 0);

        FrameData data = animationData.getFrame(currentFrame);
        Vector4i texturePositions = data.getFramePositionCopy();

        computedPosition.x = (texturePositions.x) / (texture.getWidth() * 1.0f);
        computedPosition.y = (texturePositions.z + texturePositions.x) / (texture.getWidth() * 1.0f);
        computedPosition.z = (texturePositions.y) / (texture.getHeight() * 1.0f);
        computedPosition.w = (texturePositions.w + texturePositions.y) / (texture.getHeight() * 1.0f);

        //System.out.println(computedPosition.x +  " " + computedPosition.y + " " + computedPosition.z + " ");

        return computedPosition;
    }


    public Vector2i currentHotPoint(){
        FrameData data = animationData.getFrame(currentFrame);
        return data.getHotPointReference();
    }


    public Vector2i getFrameSize(){
        return animationData.getFrame(currentFrame).getSize();
    }
}
