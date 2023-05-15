package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.mightylib.util.math.EFlip;
import MightyLibrary.mightylib.util.math.ERotation;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RectangleRenderer extends Renderer {
    protected EDirection reference;
    protected final int positionIndex, textureIndex;
    protected Vector4f texturePosition;

    protected EFlip textureFlip;
    protected ERotation textureRotation;

    public RectangleRenderer(String shaderName) {
        super(shaderName, true, true);

        reference = EDirection.LeftUp;
        textureFlip = EFlip.None;
        textureRotation = ERotation.None;

        texturePosition = new Vector4f(0f, 1f, 0f,1f);

        int[] indices = RendererUtils.indicesForSquare();
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVboFloat(calculatePosition(), 2, Shape.STATIC_STORE);
        textureIndex = shape.addVboFloat(texturePos(), 2, Shape.STATIC_STORE);
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
        return RendererUtils.texturePosition(texturePosition, textureFlip, textureRotation);
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

    public Vector2f get2DPosition(){
        return new Vector2f(position.x, position.y);
    }


    public EFlip getTextureFlip() {
        return textureFlip;
    }

    public RectangleRenderer setTextureFlip(EFlip textureFlip) {
        this.textureFlip = textureFlip;

        return updateShapeTexture();
    }

    public ERotation getTextureRotation() {
        return textureRotation;
    }

    public RectangleRenderer setTextureRotation(ERotation textureRotation) {
        this.textureRotation = textureRotation;

        return updateShapeTexture();
    }


    public RectangleRenderer updateShapeTexture(){
        shape.updateVbo(texturePos(), textureIndex);

        return this;
    }


    public RectangleRenderer updateShapePosition(){
        shape.updateVbo(calculatePosition(), positionIndex);

        return this;
    }
}
