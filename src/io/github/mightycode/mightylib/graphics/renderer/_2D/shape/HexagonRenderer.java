package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.RendererUtils;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class HexagonRenderer extends Shape2DRenderer {
    public static final float STRETCH_RATIO = (float)Math.sqrt(3.f) / 2;

    public HexagonRenderer(String shaderName) {
        this(shaderName, true);
    }
    public HexagonRenderer(String shaderName, boolean useStretchRatio){
        super(shaderName, true);

        if (useStretchRatio)
            setScale(new Vector3f(1f, STRETCH_RATIO, 1f));

        setReferenceDirection(EDirection.None);
    }

    protected int[] getIndices(){
        return new int[]{ 0, 1, 2, 2, 3, 4, 4, 5, 0, 0, 2, 4 };
    }

    @Override
    protected float[] calculatePosition(){
        return RendererUtils.calculatePositionForHexagon(new Vector2f(1, 1), getReferencePoint());
    }

    @Override
    protected float[] calculateTexturePosition(){
        return RendererUtils.calculatePositionForHexagon(new Vector2f(1, 1), new Vector2f(0, 0));
    }
}
