package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.internal.ShapeTypes;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Lines at a regular spacing over the rectangle of the shape, in a single quad: a background grid, a graph paper.
 * The lines are drawn with the stroke (width, unit, color; one gray pixel of the screen by default; the alignment
 * doesn't apply) over the fill (transparent by default). A line goes through the origin of the grid, given relative
 * to the center of the shape: with the reference point at the center (the default) and the origin 0, the lines are
 * at the multiples of the spacing around the position.
 */
public final class GridRenderer extends VectorShape2DRenderer {
    private final Vector2f spacing = new Vector2f(50f, 50f);
    private final Vector2f origin = new Vector2f();

    public GridRenderer() {
        this(EngineResources.GRID_2D);
    }

    // A shader of the project, with the parameters of grid2DF.glsl
    public GridRenderer(String shaderName) {
        super(shaderName);

        setColorMode(new Color4f(0f, 0f, 0f, 0f));
        setStrokeColor(new Color4f(0.5f, 0.5f, 0.5f, 1f));
        setStrokeUnit(EStrokeUnit.SCREEN_PIXELS);
        setStrokeWidth(1f);
        setReferenceDirection(EDirection.None);
    }

    // World units between two lines, on each axis (kept positive)
    public GridRenderer setSpacing(float x, float y) {
        spacing.set(Math.max(Math.abs(x), 1e-3f), Math.max(Math.abs(y), 1e-3f));
        return this;
    }

    public Vector2f getSpacing() {
        return new Vector2f(spacing);
    }

    // A point where two lines cross, relative to the center of the shape
    public GridRenderer setGridOrigin(Vector2f origin) {
        this.origin.set(origin);
        return this;
    }

    public Vector2f getGridOrigin() {
        return new Vector2f(origin);
    }

    @Override
    protected void writeShapeParameters(Vector4f first, Vector4f second) {
        first.set(spacing.x, spacing.y, origin.x, origin.y);
    }

    @Override
    int shapeType() {
        return ShapeTypes.GRID;
    }
}
