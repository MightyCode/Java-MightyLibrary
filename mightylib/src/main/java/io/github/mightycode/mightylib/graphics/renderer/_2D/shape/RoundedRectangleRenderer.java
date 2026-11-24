package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.internal.ShapeTypes;
import io.github.mightycode.mightylib.resources.EngineResources;
import org.joml.Vector4f;

/**
 * A rectangle with rounded corners (world units, each clamped to half the smaller side), smooth edges and a stroke;
 * radius 0 (the default) gives a sharp rectangle. Reference point at the top left by default, like RectangleRenderer
 * (which stays the renderer of the sprites).
 */
public final class RoundedRectangleRenderer extends VectorShape2DRenderer {
    // Top left, top right, bottom right, bottom left
    private final Vector4f radii = new Vector4f();

    public RoundedRectangleRenderer() {
        this(EngineResources.ROUNDED_RECTANGLE_2D);
    }

    // A shader of the project, with the parameters of roundedRectangle2DF.glsl
    public RoundedRectangleRenderer(String shaderName) {
        super(shaderName);
    }

    // The same radius for the four corners
    public RoundedRectangleRenderer setCornerRadius(float radius) {
        return setCornerRadii(radius, radius, radius, radius);
    }

    public RoundedRectangleRenderer setCornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        radii.set(Math.max(topLeft, 0f), Math.max(topRight, 0f), Math.max(bottomRight, 0f), Math.max(bottomLeft, 0f));
        return this;
    }

    // Top left, top right, bottom right, bottom left (a copy)
    public Vector4f getCornerRadii() {
        return new Vector4f(radii);
    }

    @Override
    protected void writeShapeParameters(Vector4f first, Vector4f second) {
        first.set(radii);
    }

    @Override
    int shapeType() {
        return ShapeTypes.ROUNDED_RECTANGLE;
    }
}
