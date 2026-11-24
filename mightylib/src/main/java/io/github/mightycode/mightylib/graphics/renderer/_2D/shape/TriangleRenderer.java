package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.internal.ShapeTypes;
import io.github.mightycode.mightylib.resources.EngineResources;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * A triangle with smooth edges and a stroke (rounded outside the corners). Its vertices are given in the rectangle
 * of the shape (0..1 of its size, y down): by default pointing up, (0.5, 0), (1, 1), (0, 1). Reference point at the
 * top left by default.
 */
public final class TriangleRenderer extends VectorShape2DRenderer {
    private final Vector2f first = new Vector2f(0.5f, 0f);
    private final Vector2f second = new Vector2f(1f, 1f);
    private final Vector2f third = new Vector2f(0f, 1f);

    public TriangleRenderer() {
        this(EngineResources.TRIANGLE_2D);
    }

    // A shader of the project, with the parameters of triangle2DF.glsl
    public TriangleRenderer(String shaderName) {
        super(shaderName);
    }

    // In the rectangle of the shape (0..1 of its size); a vertex out of it widens the quad
    public TriangleRenderer setVertices(Vector2f first, Vector2f second, Vector2f third) {
        this.first.set(first);
        this.second.set(second);
        this.third.set(third);
        return this;
    }

    // A copy of the vertex 0, 1 or 2
    public Vector2f getVertex(int index) {
        switch (index) {
            case 0:
                return new Vector2f(first);
            case 1:
                return new Vector2f(second);
            case 2:
                return new Vector2f(third);
            default:
                throw new IllegalArgumentException("A triangle has the vertices 0, 1 and 2, not " + index);
        }
    }

    @Override
    protected void writeShapeParameters(Vector4f firstParameters, Vector4f secondParameters) {
        float width = scale().x;
        float height = scale().y;
        firstParameters.set((first.x - 0.5f) * width, (first.y - 0.5f) * height,
                (second.x - 0.5f) * width, (second.y - 0.5f) * height);
        secondParameters.set((third.x - 0.5f) * width, (third.y - 0.5f) * height, 0f, 0f);
    }

    @Override
    protected float extraMargin() {
        float width = Math.abs(scale().x);
        float height = Math.abs(scale().y);
        return Math.max(Math.max(beyond(first, width, height), beyond(second, width, height)),
                beyond(third, width, height));
    }

    // How far the vertex is out of the rectangle, world units
    private static float beyond(Vector2f vertex, float width, float height) {
        float x = Math.max(Math.max(-vertex.x, vertex.x - 1f), 0f) * width;
        float y = Math.max(Math.max(-vertex.y, vertex.y - 1f), 0f) * height;
        return Math.max(x, y);
    }

    @Override
    int shapeType() {
        return ShapeTypes.TRIANGLE;
    }
}
