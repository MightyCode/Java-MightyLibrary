package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.resources.EngineResources;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A polyline ending with a head at its last point, all in the color of the stroke: a filled triangle (the line stops
 * at its base) or an open V drawn with the line. Head length and width in world units. Like PolylineRenderer, the
 * points are relative to the position of the renderer, the stroke gives the width (1 by default), the join and the cap
 * (at the start; the line under a filled head ends flat).
 */
public final class ArrowRenderer extends GeometricShape2DRenderer {
    private final List<Vector2f> points = new ArrayList<>();
    private EStrokeCap cap;
    private float headLength;
    private float headWidth;
    private boolean headFilled;

    public ArrowRenderer() {
        this(EngineResources.STROKE_2D);
    }

    // A shader of the project, with the attributes and parameters of stroke2DV.glsl
    public ArrowRenderer(String shaderName) {
        super(shaderName);
        cap = EStrokeCap.BUTT;
        headLength = 16f;
        headWidth = 12f;
        headFilled = true;
        setStrokeWidth(1f);
    }

    // A straight arrow from start to end (relative to the position)
    public ArrowRenderer setLine(Vector2f start, Vector2f end) {
        points.clear();
        points.add(new Vector2f(start));
        points.add(new Vector2f(end));
        geometryChanged();
        return this;
    }

    // Replaces the points (copied); the head is at the last one
    public ArrowRenderer setPoints(Collection<Vector2f> newPoints) {
        points.clear();
        for (Vector2f point : newPoints)
            points.add(new Vector2f(point));
        geometryChanged();
        return this;
    }

    public int pointCount() {
        return points.size();
    }

    public Vector2f getPoint(int index) {
        return new Vector2f(points.get(index));
    }

    public ArrowRenderer setHead(float length, float width, boolean filled) {
        headLength = Math.max(length, 0f);
        headWidth = Math.max(width, 0f);
        headFilled = filled;
        geometryChanged();
        return this;
    }

    public float getHeadLength() {
        return headLength;
    }

    public float getHeadWidth() {
        return headWidth;
    }

    public boolean isHeadFilled() {
        return headFilled;
    }

    // The cap of the start of the line, BUTT by default
    public ArrowRenderer setCap(EStrokeCap cap) {
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
        List<Vector2f> line = Stroker.cleanPoints(points, false);
        if (line.size() < 2)
            return;

        Vector2f tip = line.get(line.size() - 1);
        Vector2f before = line.get(line.size() - 2);
        float lastLength = tip.distance(before);
        Vector2f direction = new Vector2f(tip).sub(before).div(lastLength);
        Vector2f across = new Vector2f(-direction.y, direction.x).mul(headWidth / 2);
        Vector2f headBase = new Vector2f(direction).mul(-headLength).add(tip);
        Vector2f firstWing = new Vector2f(headBase).add(across);
        Vector2f secondWing = new Vector2f(headBase).sub(across);

        if (headFilled) {
            // The line stops at the base of the head
            List<Vector2f> shaft = new ArrayList<>(line.subList(0, line.size() - 1));
            if (headLength < lastLength)
                shaft.add(headBase);
            if (withStroke)
                mesh.addStroke(Stroker.stroke(shaft, false, getJoin(), cap, EStrokeCap.BUTT, getMiterLimit()),
                        StrokeMesh.STROKE_COLOR);

            if (headLength > 0f && headWidth > 0f) {
                List<Vector2f> head = List.of(tip, firstWing, secondWing);
                mesh.addFill(new float[]{tip.x, tip.y, firstWing.x, firstWing.y, secondWing.x, secondWing.y},
                        new int[]{0, 1, 2}, StrokeMesh.STROKE_COLOR);
                mesh.addFillFringe(Stroker.stroke(head, true, EStrokeJoin.MITER, EStrokeCap.BUTT, 20f),
                        PolygonRenderer.outerSide(head), StrokeMesh.STROKE_COLOR);
            }
        } else if (withStroke) {
            mesh.addStroke(Stroker.stroke(line, false, getJoin(), cap, getMiterLimit()), StrokeMesh.STROKE_COLOR);
            if (headLength > 0f && headWidth > 0f)
                mesh.addStroke(Stroker.stroke(List.of(firstWing, tip, secondWing), false, getJoin(), cap,
                        getMiterLimit()), StrokeMesh.STROKE_COLOR);
        }
    }
}
