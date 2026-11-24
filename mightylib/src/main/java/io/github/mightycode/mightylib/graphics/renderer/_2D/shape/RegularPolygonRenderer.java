package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.internal.ShapeTypes;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector4f;

/**
 * A regular polygon (3 to 64 sides) with smooth edges and a stroke, stretched to fill the size of the shape: it is
 * regular when the size has the ratio of its bounding box (HexagonRenderer.STRETCH_RATIO for 6 sides from the angle
 * 0). First vertex at the start angle (radians, from +x toward +y), straight up by default (-pi / 2). Reference point
 * at the center by default.
 */
public final class RegularPolygonRenderer extends VectorShape2DRenderer {
    private int sides;
    private float startAngle;

    public RegularPolygonRenderer(int sides) {
        this(EngineResources.REGULAR_POLYGON_2D, sides);
    }

    // A shader of the project, with the parameters of regularPolygon2DF.glsl
    public RegularPolygonRenderer(String shaderName, int sides) {
        super(shaderName);
        this.sides = RegularPolygonShape.clampSides(sides);
        startAngle = (float) (-Math.PI / 2);
        setReferenceDirection(EDirection.None);
    }

    // Kept in 3..64
    public RegularPolygonRenderer setSides(int sides) {
        this.sides = RegularPolygonShape.clampSides(sides);
        return this;
    }

    public int getSides() {
        return sides;
    }

    public RegularPolygonRenderer setStartAngle(float startAngle) {
        this.startAngle = startAngle;
        return this;
    }

    public float getStartAngle() {
        return startAngle;
    }

    @Override
    protected void writeShapeParameters(Vector4f first, Vector4f second) {
        RegularPolygonShape.write(sides, startAngle, first, second);
    }

    @Override
    int shapeType() {
        return ShapeTypes.REGULAR_POLYGON;
    }
}
