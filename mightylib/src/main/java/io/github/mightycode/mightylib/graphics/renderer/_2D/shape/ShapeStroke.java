package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.utils.math.color.Color4f;

/*
 * The stroke of a vector shape and the margin of its quad, without GL: the vertex shaders compute the same
 * (utils/shape2DVertex.glsl, shapeQuadMargin), this copy is what the tests check.
 */
final class ShapeStroke {
    private float width;
    private Color4f color;
    private EStrokeAlignment alignment;
    private EStrokeUnit unit;

    ShapeStroke() {
        width = 0f;
        color = new Color4f(0f, 0f, 0f, 1f);
        alignment = EStrokeAlignment.CENTERED;
        unit = EStrokeUnit.WORLD;
    }

    float width() { return width; }

    // Negative widths are 0: no stroke
    void setWidth(float width) {
        this.width = Math.max(width, 0f);
    }

    // The color kept here: sent to the shaders, never handed out
    Color4f color() { return color; }

    // Copied: the color given stays the caller's
    void setColor(Color4f color) {
        if (color == null)
            throw new IllegalArgumentException("The stroke color can't be null");
        this.color = color.copy();
    }

    EStrokeAlignment alignment() { return alignment; }

    void setAlignment(EStrokeAlignment alignment) {
        if (alignment == null)
            throw new IllegalArgumentException("The stroke alignment can't be null");
        this.alignment = alignment;
    }

    EStrokeUnit unit() { return unit; }

    void setUnit(EStrokeUnit unit) {
        if (unit == null)
            throw new IllegalArgumentException("The stroke unit can't be null");
        this.unit = unit;
    }

    // The width in world units, worldPerPixel being the size of a pixel in the world
    float worldWidth(float worldPerPixel) {
        return unit == EStrokeUnit.SCREEN_PIXELS ? width * worldPerPixel : width;
    }

    // Distance in world units from the edge of the shape to the outer side of the stroke
    float outerOffset(float worldPerPixel) {
        return worldWidth(worldPerPixel) * alignment.outsideFraction();
    }

    // How far the quad goes beyond the rectangle of the shape: outer part of the stroke, one pixel of
    // anti-aliasing, the extra margin of the shape
    float quadMargin(float worldPerPixel, boolean antialiasing, float extraMargin) {
        return outerOffset(worldPerPixel) + (antialiasing ? worldPerPixel : 0f) + Math.max(extraMargin, 0f);
    }
}
