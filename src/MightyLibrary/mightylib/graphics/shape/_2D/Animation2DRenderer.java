package MightyLibrary.mightylib.graphics.shape._2D;

import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import MightyLibrary.mightylib.graphics.texture.Animator;
import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.main.Window;
import MightyLibrary.mightylib.util.valueDebug.TableDebug;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;

public class Animation2DRenderer extends Renderer {

    private float computedW, computedH;
    private Window window;

    private Vector2f referencePoint;
    private Vector2f lastRefPoint;

    private Animator animator;
    private int positionIndex, textureIndex;
    private Vector4f texturePosition;

    private float scale;

    public Animation2DRenderer(String shaderName){
        super(shaderName, true, true);

        scale = 1.0f;

        computedW = 1.0f;
        computedH = 1.0f;

        window = ManagerContainer.getInstance().window;
        texturePosition = new Vector4f(0f, 1f, 0f,1f);
        referencePoint = new Vector2f(0.0f, 0.0f);
        lastRefPoint = new Vector2f(referencePoint);

        int[] indices = { 0, 1, 2, 2, 0, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(calculatePosition(), 2, Shape.DYNAMIC_STORE);
        textureIndex = shape.addVbo(texturePos(), 2, Shape.DYNAMIC_STORE);
    }


    public void init(Animator animator, Vector2f referencePoint){
        this.animator = animator;
        this.referencePoint = referencePoint;
    }


    public void update(){
        animator.update();

        if (animator.animationChanged())
            setTexture(animator.getCurrentAnimation().getData().getTextureName());

        if (animator.getCurrentAnimation().isCurrentFrameChanged()){
            this.texturePosition.set(animator.getCurrentAnimation().currentTexturePosition());
            shape.updateVbo(texturePos(), textureIndex);

            Vector2i info = animator.getCurrentAnimation().getFrameSize();
            setSizePix(info.x * scale, info.y * scale);

            shape.updateVbo(calculatePosition(), positionIndex);
        }
    }


    public void draw(){
        if (!lastRefPoint.equals(referencePoint)){
            shape.updateVbo(calculatePosition(), positionIndex);
        }

        //TableDebug.printf(calculatePosition());
        //System.out.println();

        lastRefPoint = new Vector2f(referencePoint);

        super.draw();
    }


    private void setSizePix(float width, float height){
        setSizeProp(width / window.virtualSize.x, height / window.virtualSize.y);
    }


    // Set size with a proportion of the window
    public void setSizeProp(float width, float height){
        computedW = width * 2 - 1.0f;
        computedH = height * 2 - 1.0f;
        shape.updateVbo(calculatePosition(), positionIndex);
    }


    private float[] calculatePosition(){
        float posX = referencePoint.x * 2.0f / window.virtualSize.x;
        float posY = referencePoint.y * 2.0f / window.virtualSize.y;

        // Take into account hot point of each frame of animation
        if (animator != null) {
            Vector2i vector2i = animator.getCurrentAnimation().currentHotPoint();

            //System.out.println(referencePoint.x + " " + posX + " " + (vector2i.x * 2.0f * scale / window.virtualSize.x));

            posX -= vector2i.x * 2.0f * scale / window.virtualSize.x;
            posY -= vector2i.y * 2.0f * scale / window.virtualSize.y;
        }

        return new float[]{
                -1.0f + posX, 1.0f - posY,
                -1.0f + posX, - computedH - posY,
                computedW + posX, - computedH - posY,
                computedW + posX, 1.0f - posY

        };
    }

    private float[] texturePos(){
        return new float[]{
                texturePosition.x, texturePosition.z,
                texturePosition.x, texturePosition.w,
                texturePosition.y, texturePosition.w,
                texturePosition.y, texturePosition.z
        };
    }


    public float getScale(){
        return scale;
    }

    public void setScale(float scale){
        this.scale = scale;
    }
}