package MightyLibrary.mightylib.graphics.renderer._2D;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.graphics.texture.Animator;
import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Animation2DRenderer extends Renderer {
    private final Vector2f referencePosition;
    private Animator animator;
    private final int positionIndex;
    private final int textureIndex;
    private final Vector4f texturePosition;

    private final Vector2f animationScale;

    private final Vector2f offsetRotation;

    private boolean horizontalFlip, verticalFlip;

    public Animation2DRenderer(String shaderName){
        super(shaderName, true, true);

        animationScale = new Vector2f(1);

        offsetRotation = new Vector2f(0, 0);

        texturePosition = new Vector4f(0f, 1f, 0f,1f);

        int[] indices = { 0, 1, 2, 2, 0, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(calculatePosition(), 2, Shape.DYNAMIC_STORE);
        textureIndex = shape.addVbo(texturePos(), 2, Shape.DYNAMIC_STORE);

        referencePosition = new Vector2f();

        horizontalFlip = false;
        verticalFlip = false;
    }


    public void init(Animator animator){
        this.animator = animator;

        updateInfoForFrame();
        switchToTextureMode(animator.getCurrentAnimation().getData().getTextureName());
    }


    public void update(){
        animator.update();

        if (animator.animationChanged()) {
            switchToTextureMode(animator.getCurrentAnimation().getData().getTextureName());
            updateInfoForFrame();
        } else if (animator.isFrameChanged()){
            updateInfoForFrame();
        }

        animator.lateUpdate();
    }

    public void updateInfoForFrame(){
        this.texturePosition.set(animator.getCurrentAnimation().currentTexturePosition());
        shape.updateVbo(texturePos(), textureIndex);

        updatePosition();
        this.setScale(this.animationScale);
    }

    public void setPosition(Vector2f position){
        referencePosition.x = position.x;
        referencePosition.y = position.y;

        updatePosition();
    }

    private void updatePosition(){
        Vector3f temp = new Vector3f(referencePosition.x, referencePosition.y, 0.0f);

        // Take into account hot point of each frame of animation
        if (animator != null) {
            Vector2i hotPoint = animator.getCurrentAnimation().currentHotPoint(horizontalFlip, verticalFlip);

            temp.x -= hotPoint.x * animationScale.x;
            temp.y -= hotPoint.y * animationScale.y;

            temp.x += animator.getCurrentAnimation().getFrameSize().x * animationScale.x * offsetRotation.x;
            temp.y += animator.getCurrentAnimation().getFrameSize().y * animationScale.y * offsetRotation.y;
        }

        setPosition(temp);
    }

    public void setShiftRotation(EDirection direction){
        switch (direction){
            default:
            case None:
                setShiftRotation(new Vector2f(0.5f, 0.5f));
                break;
            case RightUp:
                setShiftRotation(new Vector2f(0, 0));
                break;
            case Up:
                setShiftRotation(new Vector2f(0.5f, 0));
                break;
            case LeftUp:
                setShiftRotation(new Vector2f(1, 0));
                break;
            case Left:
                setShiftRotation(new Vector2f(1, 0.5f));
                break;
            case LeftDown:
                setShiftRotation(new Vector2f(1, 1));
                break;
            case Down:
                setShiftRotation(new Vector2f(0.5f, 1));
                break;
            case RightDown:
                setShiftRotation(new Vector2f(0, 1));
                break;
            case Right:
                setShiftRotation(new Vector2f(0, 0.5f));
                break;
        }
    }

    public void setShiftRotation(Vector2f offsetRotation){
        this.offsetRotation.x = offsetRotation.x;
        this.offsetRotation.y = offsetRotation.y;
    }

    private float[] calculatePosition(){
        return new float[]{
                0 - offsetRotation.x, 1 - offsetRotation.y,
                0 - offsetRotation.x, 0 - offsetRotation.y,
                1 - offsetRotation.x, 0 - offsetRotation.y,
                1 - offsetRotation.x, 1 - offsetRotation.y
        };
    }

    private float[] texturePos(){
        Vector4f bounds = new Vector4f(texturePosition);

        if (horizontalFlip){
            float temp = bounds.x;
            bounds.x = bounds.y;
            bounds.y = temp;
        }

        if (verticalFlip){
            float temp = bounds.z;
            bounds.z = bounds.w;
            bounds.w = temp;
        }


        return new float[]{
                bounds.x, bounds.w,
                bounds.x, bounds.z,
                bounds.y, bounds.z,
                bounds.y, bounds.w
        };
    }

    public Animation2DRenderer setHorizontalFlip(boolean state){
        horizontalFlip = state;

        shape.updateVbo(texturePos(), textureIndex);

        return this;
    }

    public Animation2DRenderer setVerticalFlip(boolean state){
        verticalFlip = state;

        shape.updateVbo(texturePos(), textureIndex);

        return this;
    }


    public Vector2f getScale(){
        return animationScale;
    }

    public Animator getAnimator() {
        return  animator;
    }

    @Override
    public Vector3f position(){
        return new Vector3f(referencePosition.x, referencePosition.y, position.z);
    }

    public void setScale(Vector2f scale){
        this.animationScale.x = scale.x;
        this.animationScale.y = scale.y;

        Vector2i info = animator.getCurrentAnimation().getFrameSize();

        super.setScale(new Vector3f(info.x * animationScale.x, info.y * animationScale.y, 1f));
    }
}
