package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.joml.Vector4i;

public final class Animation {
    private AnimationData animationData;

    private float elapsedTime;
    private float speed;

    private int lastFrame;
    private int currentFrame;
    private boolean looping;
    private boolean animationFinished;

    // Data given by the last data() call
    private AnimationData usedData;

    // Belongs to the engine of the view that creates it
    private final Resources resources = Views.engine().resources();

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
        AnimationData usable = resources.usable(AnimationData.class, animationData);
        if (usable != usedData) {
            usedData = usable;
            restart();
        }

        return usable;
    }

    private TextureData texture() {
        TextureData texture = resources.getResourceOrFallback(TextureData.class, data().getTextureName());
        return resources.usable(TextureData.class, texture);
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


    /*
     * Texels kept inside each side of a frame. The frames of a sheet touch each other (no margin): sampling at the
     * edge of a quad can read up to half a texel outside its frame, which drew a line of the neighbor frame above the
     * animations. Cost: the outer rows and columns of a frame are drawn half as wide as the others (a margin in the
     * sheets would avoid it, docs/later.md).
     */
    static final float FRAME_INSET = 0.5f;

    public Vector4f currentTexturePosition() {
        return currentTexturePosition(new Vector4f());
    }

    // Texture coordinates of the current frame (left, right, top, bottom) written in out, without allocation
    public Vector4f currentTexturePosition(Vector4f out) {
        FrameData data = data().getFrame(currentFrame);
        TextureData texture = texture();

        return FrameTexturePosition(data.getFramePositionReference(), texture.getWidth(), texture.getHeight(), out);
    }

    /**
     * Texture coordinates of a frame (x, y, width, height in pixels) of a sheet of the given size, FRAME_INSET inside
     * each side: left, right, top, bottom in out.
     */
    static Vector4f FrameTexturePosition(Vector4i frame, int sheetWidth, int sheetHeight, Vector4f out) {
        float width = sheetWidth, height = sheetHeight;
        // A frame thinner than two insets keeps its center
        float insetX = Math.min(FRAME_INSET, frame.z * 0.5f), insetY = Math.min(FRAME_INSET, frame.w * 0.5f);

        return out.set((frame.x + insetX) / width, (frame.x + frame.z - insetX) / width,
                (frame.y + insetY) / height, (frame.y + frame.w - insetY) / height);
    }


    public Vector2i currentHotPoint() {
        return currentHotPoint(false, false);
    }

    public Vector2i currentHotPoint(boolean horizontalFlip, boolean verticalFlip){
        return currentHotPoint(horizontalFlip, verticalFlip, new Vector2i());
    }

    // The hot point of the current frame, mirrored inside the frame by the flips, written in out
    public Vector2i currentHotPoint(boolean horizontalFlip, boolean verticalFlip, Vector2i out){
        FrameData data = data().getFrame(currentFrame);
        Vector2i hotPoint = data.getHotPointReference();
        Vector4i frame = data.getFramePositionReference();

        return out.set(horizontalFlip ? frame.z - hotPoint.x : hotPoint.x,
                verticalFlip ? frame.w - hotPoint.y : hotPoint.y);
    }


    public Vector2i getFrameSize(){
        return getFrameSize(new Vector2i());
    }

    // Width and height in pixels of the current frame, written in out
    public Vector2i getFrameSize(Vector2i out){
        Vector4i frame = data().getFrame(currentFrame).getFramePositionReference();
        return out.set(frame.z, frame.w);
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Makes this frame (from 0) the current one, from its start: seen as a frame change by the next update of the
     * renderer (isCurrentFrameChanged). The frame of the usable data: while the animation is loading, that of its
     * fallback, and the animation restarts once loaded.
     * @throws IndexOutOfBoundsException when the data has no such frame
     */
    public void setFrame(int frame) {
        int count = data().frameNumber();
        if (frame < 0 || frame >= count)
            throw new IndexOutOfBoundsException("Frame " + frame + " of an animation of " + count + " frames");

        currentFrame = frame;
        elapsedTime = 0;
        animationFinished = false;
    }
}
