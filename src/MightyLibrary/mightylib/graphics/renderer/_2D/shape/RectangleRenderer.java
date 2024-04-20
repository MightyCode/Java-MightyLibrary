package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.RectangularFace;
import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import MightyLibrary.mightylib.utils.math.geometry.EFlip;
import MightyLibrary.mightylib.utils.math.geometry.ERotation;
import org.joml.Vector2f;

public class RectangleRenderer extends Shape2DRenderer {
    protected RectangularFace face;

    public RectangleRenderer(String shaderName) {
        super(shaderName, true);

        face = new RectangularFace();
        texturePosition = RectangularFace.BasicTexturePosition();
    }

    @Override
    public Shape2DRenderer init() {
        super.init();

        setReferenceDirection(EDirection.LeftUp);

        return this;
    }

    @Override
    protected int[] getIndices(){
        return RectangularFace.IndicesForSquare();
    }

    @Override
    protected float[] calculatePosition(){
        return RendererUtils.calculatePositionForSquare(new Vector2f(1, 1), getReferencePoint());
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
