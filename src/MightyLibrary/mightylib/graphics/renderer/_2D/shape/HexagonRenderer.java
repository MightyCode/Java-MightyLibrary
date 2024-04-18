package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.RectangularFace;
import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.utils.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class HexagonRenderer extends Shape2DRenderer {
    public static final float STRETCH_RATIO = (float)Math.sqrt(3.f) / 2;

    protected EDirection reference;
    protected Vector4f texturePosition;
    public HexagonRenderer(String shaderName) {
        this(shaderName, true);
    }
    public HexagonRenderer(String shaderName, boolean useStretchRatio){
        super(shaderName, true);

        if (useStretchRatio)
            setScale(new Vector3f(1f, STRETCH_RATIO, 1f));
    }

    @Override
    protected void prepare(){
        reference = EDirection.None;
        texturePosition = RectangularFace.BasicTexturePosition();
    }

    protected int[] getIndices(){
        return new int[]{ 0, 1, 2, 2, 3, 4, 4, 5, 0, 0, 2, 4 };
    }

    @Override
    protected float[] calculatePosition(){
        return RendererUtils.calculatePositionForHexagon(new Vector2f(1, 1), this.reference);
    }

    @Override
    protected float[] calculateTexturePosition(){
        return RendererUtils.calculatePositionForHexagon(new Vector2f(1, 1), EDirection.LeftUp);
    }
}
