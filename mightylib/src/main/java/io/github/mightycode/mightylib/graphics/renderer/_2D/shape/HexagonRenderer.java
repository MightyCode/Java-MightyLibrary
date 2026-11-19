package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * A hexagon with a flat top and bottom filling the size of the shape, smooth edges and a stroke: a regular polygon
 * of 6 sides, regular when height = width * STRETCH_RATIO (the size given at the construction by default).
 * Reference point at the center by default.
 */
public final class HexagonRenderer extends VectorShape2DRenderer {
    public static final float STRETCH_RATIO = (float) Math.sqrt(3.f) / 2;
    private static final int SIDES = 6;

    public HexagonRenderer() {
        this(true);
    }

    public HexagonRenderer(boolean useStretchRatio) {
        this(EngineResources.REGULAR_POLYGON_2D, useStretchRatio);
    }

    // A shader of the project, with the parameters of regularPolygon2DF.glsl
    public HexagonRenderer(String shaderName, boolean useStretchRatio) {
        super(shaderName);

        if (useStretchRatio)
            setScale(new Vector3f(1f, STRETCH_RATIO, 1f));

        setReferenceDirection(EDirection.None);
    }

    @Override
    protected void writeShapeParameters(Vector4f first, Vector4f second) {
        RegularPolygonShape.write(SIDES, 0f, first, second);
    }
}
