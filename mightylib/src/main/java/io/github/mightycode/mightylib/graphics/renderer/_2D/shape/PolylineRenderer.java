package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.resources.EngineResources;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A thick line through points, open or closed, with smooth edges: drawn with the stroke (width, color, unit: world
 * units or screen pixels; 1 by default), joins (miter, round, bevel) and caps (butt, round, square). The points are
 * given relative to the position of the renderer (its rotation applies to them; its size is not used). A width or a
 * zoom that changes costs nothing: the vertex shader extrudes the line.
 */
public final class PolylineRenderer extends GeometricShape2DRenderer {
    private final List<Vector2f> points = new ArrayList<>();
    private boolean closed;
    private EStrokeCap cap;

    public PolylineRenderer() {
        this(EngineResources.STROKE_2D);
    }

    // A shader of the project, with the attributes and parameters of stroke2DV.glsl
    public PolylineRenderer(String shaderName) {
        super(shaderName);
        cap = EStrokeCap.BUTT;
        setStrokeWidth(1f);
    }

    // Replaces the points (copied)
    public PolylineRenderer setPoints(Collection<Vector2f> newPoints) {
        points.clear();
        for (Vector2f point : newPoints)
            points.add(new Vector2f(point));
        geometryChanged();
        return this;
    }

    public PolylineRenderer addPoint(Vector2f point) {
        points.add(new Vector2f(point));
        geometryChanged();
        return this;
    }

    public PolylineRenderer setPoint(int index, Vector2f point) {
        points.get(index).set(point);
        geometryChanged();
        return this;
    }

    public PolylineRenderer clearPoints() {
        points.clear();
        geometryChanged();
        return this;
    }

    public int pointCount() {
        return points.size();
    }

    // A copy of the point of this index
    public Vector2f getPoint(int index) {
        return new Vector2f(points.get(index));
    }

    // Closed: the last point is joined to the first one, no cap
    public PolylineRenderer setClosed(boolean closed) {
        this.closed = closed;
        geometryChanged();
        return this;
    }

    public boolean isClosed() {
        return closed;
    }

    // BUTT by default
    public PolylineRenderer setCap(EStrokeCap cap) {
        if (cap == null)
            throw new IllegalArgumentException("The cap can't be null");
        this.cap = cap;
        geometryChanged();
        return this;
    }

    public EStrokeCap getCap() {
        return cap;
    }

    @Override
    void buildGeometry(StrokeMesh mesh, boolean withStroke) {
        if (withStroke)
            mesh.addStroke(Stroker.stroke(points, closed, getJoin(), cap, getMiterLimit()), StrokeMesh.STROKE_COLOR);
    }
}
