package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.RectangularFace;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import MightyLibrary.mightylib.resources.texture.Texture;
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

    protected RectangularFace face;

    public RectangleRenderer(String shaderName) {
        super(shaderName, true);

        reference = EDirection.LeftUp;
        face = new RectangularFace();

        texturePosition = RectangularFace.BasicTexturePosition();

        int[] indices = RectangularFace.IndicesForSquare();
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
        return face.textureCornerPosition(texturePosition);
    }

    // Set size with size of pixel
    public RectangleRenderer setSizePix(float width, float height){
        setScale(new Vector3f(width, height, 1.0f));

        return this;
    }

    public RectangleRenderer setSizeToTexture() {
        if (textures[0] == null)
            return this;

        setScale(new Vector3f(textures[0].getWidth(), textures[0].getHeight(), 1.0f));

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
        return face.getFlip();
    }

    public RectangleRenderer setTextureFlip(EFlip textureFlip) {
        this.face.setFlip(textureFlip);

        return updateShapeTexture();
    }

    public ERotation getTextureRotation() {
        return face.getRotation();
    }

    public RectangleRenderer setTextureRotation(ERotation textureRotation) {
        this.face.setRotation(textureRotation);

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
