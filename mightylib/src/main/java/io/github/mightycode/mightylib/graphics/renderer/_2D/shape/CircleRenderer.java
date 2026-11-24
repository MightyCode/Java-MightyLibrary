package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.internal.ShapeTypes;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;

/**
 * A circle with smooth edges and a stroke: the largest circle in the size of the shape (setRadius gives a square
 * size). Reference point at the center by default.
 */
public final class CircleRenderer extends VectorShape2DRenderer {
    public CircleRenderer() {
        this(EngineResources.CIRCLE_2D);
    }

    // A shader of the project, with the parameters of circle2DF.glsl
    public CircleRenderer(String shaderName) {
        super(shaderName);
        setReferenceDirection(EDirection.None);
    }

    // World units: the size becomes a square of side 2 radius
    public CircleRenderer setRadius(float radius) {
        setSizePix(2 * radius, 2 * radius);
        return this;
    }

    public float getRadius() {
        return Math.min(Math.abs(scale().x), Math.abs(scale().y)) / 2;
    }

    @Override
    int shapeType() {
        return ShapeTypes.CIRCLE;
    }
}
