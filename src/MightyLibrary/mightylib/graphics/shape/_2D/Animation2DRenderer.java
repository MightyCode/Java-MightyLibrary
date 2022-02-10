package MightyLibrary.mightylib.graphics.shape._2D;

import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import MightyLibrary.mightylib.graphics.texture.Animator;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.util.math.MightyMath;
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

    public Animation2DRenderer(String shaderName){
        super(shaderName, true, true);

        animationScale = new Vector2f(1);

        texturePosition = new Vector4f(0f, 1f, 0f,1f);

        int[] indices = { 0, 1, 2, 2, 0, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(calculatePosition(), 2, Shape.DYNAMIC_STORE);
        textureIndex = shape.addVbo(texturePos(), 2, Shape.DYNAMIC_STORE);

        referencePosition = new Vector2f();
    }


    public void init(Animator animator){
        this.animator = animator;

        updatePosition();
    }


    public void update(){
        animator.update();

        if (animator.animationChanged())
            switchToTextureMode(animator.getCurrentAnimation().getData().getTextureName());

        if (animator.getCurrentAnimation().isCurrentFrameChanged()){
            this.texturePosition.set(animator.getCurrentAnimation().currentTexturePosition());
            shape.updateVbo(texturePos(), textureIndex);

            updatePosition();

            this.setScale(this.animationScale);
        }
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
            Vector2i hotPoint = animator.getCurrentAnimation().currentHotPoint();

            temp.x -= hotPoint.x * animationScale.x;
            temp.y -= hotPoint.y * animationScale.y;
        }

        setPosition(temp);
    }

    private float[] calculatePosition(){
        return new float[]{
                0, 1,
                0, 0,
                1, 0,
                1, 1
        };
    }

    private float[] texturePos(){
        return new float[]{
                texturePosition.x, texturePosition.w,
                texturePosition.x, texturePosition.z,
                texturePosition.y, texturePosition.z,
                texturePosition.y, texturePosition.w
        };
    }


    public Vector2f getScale(){
        return animationScale;
    }

    public void setScale(Vector2f scale){
        this.animationScale.x = scale.x;
        this.animationScale.y = scale.y;

        Vector2i info = animator.getCurrentAnimation().getFrameSize();
        super.setScale(new Vector3f(info.x * animationScale.x, info.y * animationScale.y, 1f));
    }
}
