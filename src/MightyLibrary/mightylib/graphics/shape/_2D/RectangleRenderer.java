package MightyLibrary.mightylib.graphics.shape._2D;

import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.main.Window;
import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class RectangleRenderer extends Renderer {
    protected Window window;
    protected float windowW, windowH, posX, posY;
    protected int positionIndex, textureIndex;
    protected Vector4f texturePosition;

    public RectangleRenderer(String shaderName) {
        super(shaderName, true, true);
        window = ManagerContainer.getInstance().window;
        posX = 0.0f;
        posY = 0.0f;

        windowW = 1.0f;
        windowH = 1.0f;

        texturePosition = new Vector4f(0, 1, 0,1 );

        int[] indices = { 0, 1, 2, 2, 0, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(calculatePosition(), 2, Shape.STATIC_STORE);
        textureIndex = shape.addVbo(texturePos(), 2, Shape.STATIC_STORE);
    }


    private float[] calculatePosition(){
        return new float[]{
                -1.0f + posX, 1.0f - posY,
                -1.0f + posX, -windowH - posY,
                windowW + posX, -windowH - posY,
                windowW + posX, 1.0f - posY

        };
    }


    public void setTexturePosition(Vector4f newTexturePosition){
        texturePosition = newTexturePosition;
    }


    private float[] texturePos(){
        return new float[]{
                texturePosition.x, texturePosition.z,
                texturePosition.x, texturePosition.w,
                texturePosition.y, texturePosition.w,
                texturePosition.y, texturePosition.z
        };
    }

    // Set size with size of pixel
    public RectangleRenderer setSizePix(float width, float height){
        windowW = width * 2.0f / window.size.x - 1.0f;
        windowH = height * 2.0f / window.size.y - 1.0f;
        return setSizeProp(width * 1.0f / window.size.x, height * 1.0f / window.size.y);
    }


    // Set size with a proportion of the window
    public RectangleRenderer setSizeProp(float width, float height){
        windowW = width * 2.0f - 1.0f;
        windowH = height * 2.0f - 1.0f;
        updateShape();
        return this;
    }


    public void setPosition(Vector2f position){
        setPosition(position.x, position.y);
    }


    public void setPosition(float posX, float posY){
        this.posX = posX * 2.0f / window.size.x;
        this.posY = posY * 2.0f / window.size.y;;
        updateShape();
    }


    public void updateShape(){
        shape.resetVbo(calculatePosition(), positionIndex);
        shape.resetVbo(texturePos(), textureIndex);
    }
}
