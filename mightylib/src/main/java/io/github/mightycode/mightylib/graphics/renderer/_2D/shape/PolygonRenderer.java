package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylib.utils.math.geometry.Polygon;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A polygon (convex or not, either winding) filling the size of the shape (its bounding box stretched onto it), with
 * smooth edges and a stroke centered on its edges (joins of setJoin). Filled with the color, or the texture over its
 * bounding box. Reference point: a point of the bounding box (0..1), at the center by default, or a vertex
 * (setReferenceVertex).
 */
public final class PolygonRenderer extends GeometricShape2DRenderer {
    // Miter limit of the fringe of the fill: it follows the sharp corners
    private static final float FRINGE_MITER_LIMIT = 20f;

    private final Polygon polygon;
    private final int[] fillIndices;
    private final Vector2f builtSize = new Vector2f(Float.NaN);
    private final Vector2f builtReference = new Vector2f(Float.NaN);

    public PolygonRenderer(Polygon polygon) {
        this(EngineResources.STROKE_2D, polygon);
    }

    // A shader of the project, with the attributes and parameters of stroke2DV.glsl
    public PolygonRenderer(String shaderName, Polygon polygon) {
        super(shaderName);
        this.polygon = polygon;
        fillIndices = polygon.isConvex() ? fanIndices(polygon.size()) : polygon.earClipIndices();
        setReferenceDirection(EDirection.None);
    }

    private static int[] fanIndices(int count) {
        int[] indices = new int[Math.max(count - 2, 0) * 3];
        for (int i = 0; i < count - 2; ++i) {
            indices[i * 3] = 0;
            indices[i * 3 + 1] = i + 1;
            indices[i * 3 + 2] = i + 2;
        }
        return indices;
    }

    public int numberVertex() {
        return polygon.size();
    }

    // The vertex of this index at the position of the renderer
    public void setReferenceVertex(int index) {
        Vector4f box = polygon.getBoundingBox();
        Vector2f vertex = polygon.get(index);
        setReferencePoint(new Vector2f((vertex.x - box.x) / (box.z - box.x), (vertex.y - box.y) / (box.w - box.y)));
    }

    // The side (1 left, -1 right of the direction of the edges) of the outside of a closed outline
    static float outerSide(List<Vector2f> outline) {
        float area = 0f;
        for (int i = 0; i < outline.size(); ++i) {
            Vector2f a = outline.get(i);
            Vector2f b = outline.get((i + 1) % outline.size());
            area += a.x * b.y - b.x * a.y;
        }
        // Positive area: the inside is on the left of the edges
        return area > 0f ? -1f : 1f;
    }

    @Override
    boolean geometryOutdated() {
        return builtSize.x != scale().x || builtSize.y != scale().y || !builtReference.equals(getReferencePoint());
    }

    @Override
    void buildGeometry(StrokeMesh mesh, boolean withStroke) {
        builtSize.set(scale().x, scale().y);
        builtReference.set(getReferencePoint());

        // The vertices in the frame of the shape: world units from the reference point
        Vector4f box = polygon.getBoundingBox();
        float width = Math.max(box.z - box.x, 1e-6f);
        float height = Math.max(box.w - box.y, 1e-6f);
        Vector2f reference = getReferencePoint();
        List<Vector2f> outline = new ArrayList<>();
        float[] positions = new float[polygon.size() * 2];
        for (int i = 0; i < polygon.size(); ++i) {
            Vector2f vertex = polygon.get(i);
            float x = ((vertex.x - box.x) / width - reference.x) * scale().x;
            float y = ((vertex.y - box.y) / height - reference.y) * scale().y;
            positions[i * 2] = x;
            positions[i * 2 + 1] = y;
            outline.add(new Vector2f(x, y));
        }

        mesh.addFill(positions, fillIndices, StrokeMesh.FILL_COLOR);
        mesh.addFillFringe(Stroker.stroke(outline, true, EStrokeJoin.MITER, EStrokeCap.BUTT, FRINGE_MITER_LIMIT),
                outerSide(outline), StrokeMesh.FILL_COLOR);

        if (withStroke)
            mesh.addStroke(Stroker.stroke(outline, true, getJoin(), EStrokeCap.BUTT, getMiterLimit()),
                    StrokeMesh.STROKE_COLOR);
    }
}
