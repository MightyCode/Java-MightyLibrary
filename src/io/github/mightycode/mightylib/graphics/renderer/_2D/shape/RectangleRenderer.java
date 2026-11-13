package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.RectangularFace;
import io.github.mightycode.mightylib.graphics.renderer.RendererUtils;
import io.github.mightycode.mightylib.utils.math.geometry.EFlip;
import io.github.mightycode.mightylib.utils.math.geometry.ERotation;
import org.joml.Vector2f;

public class RectangleRenderer extends Shape2DRenderer {
    protected RectangularFace face;

    public RectangleRenderer(String shaderName) {
        super(shaderName, true);

        face = new RectangularFace();
        setTexturePosition(RectangularFace.BasicTexturePosition());
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
        return face.textureCornerPosition(getTexturePosition());
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
