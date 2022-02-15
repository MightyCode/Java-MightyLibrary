package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RectangleRenderer extends Renderer {
    protected EDirection reference;
    protected final int positionIndex, textureIndex;
    protected Vector4f texturePosition;

    public RectangleRenderer(String shaderName) {
        super(shaderName, true, true);

        reference = EDirection.LeftUp;

        texturePosition = new Vector4f(0f, 1f, 0f,1f);

        int[] indices = { 0, 1, 2, 2, 0, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(calculatePosition(), 2, Shape.STATIC_STORE);
        textureIndex = shape.addVbo(texturePos(), 2, Shape.STATIC_STORE);
    }


    private float[] calculatePosition(){
        return RendererUtils.calculatePositionForSquare(new Vector2f(1, 1), this.reference);
    }

    public RectangleRenderer setReference(EDirection reference){
        this.reference = reference;

        shape.updateVbo(calculatePosition(), positionIndex);

        return this;
    }


    public void setTexturePosition(Vector4f newTexturePosition){
        texturePosition = newTexturePosition;
    }


    private float[] texturePos(){
        return new float[]{
                texturePosition.x, texturePosition.w,
                texturePosition.x, texturePosition.z,
                texturePosition.y, texturePosition.z,
                texturePosition.y, texturePosition.w
        };
    }

    // Set size with size of pixel
    public RectangleRenderer setSizePix(float width, float height){
        setScale(new Vector3f(width, height, 1.0f));

        return this;
    }

    public RectangleRenderer setSizeToTexture(){
        setScale(new Vector3f(texture.getWidth(), texture.getHeight(), 1.0f));

        return this;
    }


    public RectangleRenderer setPosition(Vector2f position){
        super.setPosition(new Vector3f(position.x, position.y, 0.0f));

        return this;
    }

    public void updateShape(){
        //shape.updateVbo(calculatePosition(), positionIndex);
        shape.updateVbo(texturePos(), textureIndex);
    }
}