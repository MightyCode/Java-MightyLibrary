package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.RectangularFace;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.utils.math.EDirection;
import MightyLibrary.mightylib.utils.math.EFlip;
import MightyLibrary.mightylib.utils.math.ERotation;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RectangleRenderer extends ShapeRenderer {
    protected RectangularFace face;

    public RectangleRenderer(String shaderName) {
        super(shaderName, true);
    }

    @Override
    protected void prepare(){
        reference = EDirection.LeftUp;
        face = new RectangularFace();
        texturePosition = RectangularFace.BasicTexturePosition();
    }

    @Override
    protected int[] getIndices(){
        return RectangularFace.IndicesForSquare();
    }

    @Override
    protected float[] calculatePosition(){
        return RendererUtils.calculatePositionForSquare(new Vector2f(1, 1), this.reference);
    }

    @Override
    protected float[] calculateTexturePosition(){
        return face.textureCornerPosition(texturePosition);
    }

    public EFlip getTextureFlip() {
        return face.getFlip();
    }

    public RectangleRenderer setTextureFlip(EFlip textureFlip) {
        this.face.setFlip(textureFlip);

        return (RectangleRenderer) updateShapeTexture();
    }

    public ERotation getTextureRotation() {
        return face.getRotation();
    }

    public RectangleRenderer setTextureRotation(ERotation textureRotation) {
        this.face.setRotation(textureRotation);

        return (RectangleRenderer) updateShapeTexture();
    }
}
