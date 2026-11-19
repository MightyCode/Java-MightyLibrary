package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import org.apiguardian.api.API;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * Turns a polyline into the triangles of its stroke, independent of the width (StrokeGeometry): one quad per segment,
 * the joins on the outer side of each turn (miter, bevel or round fan) without covering the quads, the inner side
 * closed at the meeting point of the inner edges, the caps at the ends of an open line. Pure Java, without GL: used by
 * PolylineRenderer, ArrowRenderer and PolygonRenderer, and by any renderer of paths.
 * <p>
 * Repeated points are ignored; a turn back on the same line (U-turn) gets a round fan with the round join, nothing
 * with the others; the inner side of a join is limited to half the shorter of its segments (a line wider than its
 * segments shows it). A miter longer than miterLimit times the width becomes a bevel (like SVG).
 */
@API(status = EXPERIMENTAL)
public final class Stroker {
    public static final float DEFAULT_MITER_LIMIT = 4f;
    // Angle of a step of the round joins and caps: the arc is off by 0.5 % of the half width at most
    static final double ROUND_STEP = Math.PI / 16;
    // Points closer than this are the same one
    private static final float SAME_POINT = 1e-5f;
    // Below this sine, two segments are on the same line (straight on or U-turn)
    private static final float STRAIGHT = 1e-4f;

    private static final float[] INTERIOR = {0f, -1f};

    private Stroker() {}

    /**
     * The stroke of the polyline (at least two different points, else an empty geometry); closed: the last point is
     * joined to the first one, no cap.
     */
    public static StrokeGeometry stroke(List<Vector2f> points, boolean closed, EStrokeJoin join, EStrokeCap cap,
                                        float miterLimit) {
        if (points == null || join == null || cap == null)
            throw new IllegalArgumentException("The points, the join and the cap of a stroke can't be null");

        List<Vector2f> clean = cleanPoints(points, closed);
        if (clean.size() < 2)
            return new Builder(clean, false, join, cap, miterLimit).geometry();

        Builder builder = new Builder(clean, closed && clean.size() >= 3, join, cap, miterLimit);
        builder.build();
        return builder.geometry();
    }

    // Without the repeated points (consecutive ones, and the last one equal to the first of a closed line)
    static List<Vector2f> cleanPoints(List<Vector2f> points, boolean closed) {
        List<Vector2f> clean = new ArrayList<>();
        for (Vector2f point : points) {
            if (clean.isEmpty() || clean.get(clean.size() - 1).distance(point) > SAME_POINT)
                clean.add(new Vector2f(point));
        }

        if (closed) {
            while (clean.size() > 1 && clean.get(clean.size() - 1).distance(clean.get(0)) <= SAME_POINT)
                clean.remove(clean.size() - 1);
        }

        return clean;
    }

    private static final class Builder {
        private final List<Vector2f> points;
        private final boolean loop;
        private final EStrokeJoin join;
        private final EStrokeCap cap;
        private final float miterLimit;

        private final int segmentCount;
        private final Vector2f[] directions;
        private final Vector2f[] normals;
        private final float[] lengths;
        // Left (side 1) and right (side -1) vertices at the start and at the end of each segment
        private final int[][] starts;
        private final int[][] ends;

        private final FloatList bases = new FloatList();
        private final FloatList extrusions = new FloatList();
        private final FloatList fringes = new FloatList();
        private final FloatList sides = new FloatList();
        private final FloatList alongs = new FloatList();
        private final FloatList limits = new FloatList();
        private final List<Integer> indices = new ArrayList<>();

        Builder(List<Vector2f> points, boolean loop, EStrokeJoin join, EStrokeCap cap, float miterLimit) {
            this.points = points;
            this.loop = loop;
            this.join = join;
            this.cap = cap;
            this.miterLimit = miterLimit;

            segmentCount = points.size() < 2 ? 0 : (loop ? points.size() : points.size() - 1);
            directions = new Vector2f[segmentCount];
            normals = new Vector2f[segmentCount];
            lengths = new float[segmentCount];
            starts = new int[segmentCount][];
            ends = new int[segmentCount][];

            for (int i = 0; i < segmentCount; ++i) {
                Vector2f edge = new Vector2f(points.get((i + 1) % points.size())).sub(points.get(i));
                lengths[i] = edge.length();
                directions[i] = edge.div(lengths[i]);
                normals[i] = new Vector2f(-directions[i].y, directions[i].x);
            }
        }

        void build() {
            int count = points.size();
            for (int j = 0; j < count; ++j) {
                if (!loop && j == 0)
                    cap(0, true);
                else if (!loop && j == count - 1)
                    cap(segmentCount - 1, false);
                else
                    join(j, (j - 1 + segmentCount) % segmentCount, j % segmentCount);
            }

            for (int i = 0; i < segmentCount; ++i) {
                int startLeft = starts[i][0];
                int startRight = starts[i][1];
                int endLeft = ends[i][0];
                int endRight = ends[i][1];
                triangle(startLeft, startRight, endRight);
                triangle(startLeft, endRight, endLeft);
            }
        }

        private void cap(int segment, boolean atStart) {
            Vector2f base = atStart ? points.get(0) : points.get(points.size() - 1);
            Vector2f normal = normals[segment];
            Vector2f outward = new Vector2f(directions[segment]);
            if (atStart)
                outward.negate();

            int left = vertex(base, normal.x, normal.y, 1f, StrokeGeometry.NO_LIMIT);
            int right = vertex(base, -normal.x, -normal.y, -1f, StrokeGeometry.NO_LIMIT);
            if (atStart)
                starts[segment] = new int[]{left, right};
            else
                ends[segment] = new int[]{left, right};

            switch (cap) {
                case ROUND:
                    // Half disk from the left edge through the outward direction to the right edge
                    fan(base, normal, outward, Math.PI, 1f);
                    break;
                case BUTT:
                case SQUARE:
                    flatCap(base, normal, outward, cap == EStrokeCap.SQUARE);
                    break;
                default:
                    throw new IllegalStateException("Unknown cap " + cap);
            }
        }

        // The end piece of a butt (one fringe long) or square (half width and one fringe long) cap, faded along
        private void flatCap(Vector2f base, Vector2f normal, Vector2f outward, boolean square) {
            float extension = square ? 1f : 0f;
            float[] inner = {-extension, -0.5f};
            float[] outer = {0f, 0.5f};

            int innerLeft = vertex(base, normal.x, normal.y, 0f, 0f, 1f, inner, StrokeGeometry.NO_LIMIT);
            int innerRight = vertex(base, -normal.x, -normal.y, 0f, 0f, -1f, inner, StrokeGeometry.NO_LIMIT);
            int outerLeft;
            int outerRight;
            if (square) {
                outerLeft = vertex(base, normal.x + outward.x, normal.y + outward.y, 0f, 0f, 1f, outer,
                        StrokeGeometry.NO_LIMIT);
                outerRight = vertex(base, -normal.x + outward.x, -normal.y + outward.y, 0f, 0f, -1f, outer,
                        StrokeGeometry.NO_LIMIT);
            } else {
                outerLeft = vertex(base, normal.x, normal.y, outward.x, outward.y, 1f, outer, StrokeGeometry.NO_LIMIT);
                outerRight = vertex(base, -normal.x, -normal.y, outward.x, outward.y, -1f, outer,
                        StrokeGeometry.NO_LIMIT);
            }

            triangle(innerLeft, innerRight, outerRight);
            triangle(innerLeft, outerRight, outerLeft);
        }

        private void join(int pointIndex, int previous, int next) {
            Vector2f base = points.get(pointIndex);
            Vector2f d1 = directions[previous];
            Vector2f d2 = directions[next];
            Vector2f n1 = normals[previous];
            Vector2f n2 = normals[next];
            float cross = d1.x * d2.y - d1.y * d2.x;
            float dot = d1.dot(d2);

            if (Math.abs(cross) < STRAIGHT && dot > 0) {
                // Straight on: the two segments share their vertices
                Vector2f normal = new Vector2f(n1).add(n2).normalize();
                int left = vertex(base, normal.x, normal.y, 1f, StrokeGeometry.NO_LIMIT);
                int right = vertex(base, -normal.x, -normal.y, -1f, StrokeGeometry.NO_LIMIT);
                ends[previous] = new int[]{left, right};
                starts[next] = new int[]{left, right};
                return;
            }

            if (Math.abs(cross) < STRAIGHT) {
                // U-turn: each segment ends square on the point, a half disk beyond it with the round join
                ends[previous] = new int[]{vertex(base, n1.x, n1.y, 1f, StrokeGeometry.NO_LIMIT),
                        vertex(base, -n1.x, -n1.y, -1f, StrokeGeometry.NO_LIMIT)};
                starts[next] = new int[]{vertex(base, n2.x, n2.y, 1f, StrokeGeometry.NO_LIMIT),
                        vertex(base, -n2.x, -n2.y, -1f, StrokeGeometry.NO_LIMIT)};
                if (join == EStrokeJoin.ROUND)
                    fan(base, n1, d1, Math.PI, 1f);
                return;
            }

            // The inner side is the one the line turns to
            float innerSide = cross > 0 ? 1f : -1f;
            float outerSide = -innerSide;

            Vector2f bisector = new Vector2f(n1).add(n2).normalize();
            float cosHalf = Math.max(bisector.dot(n1), 1e-6f);
            float tanHalf = (float) Math.sqrt(Math.max(1 - cosHalf * cosHalf, 0f)) / cosHalf;
            Vector2f miter = new Vector2f(bisector).div(cosHalf);

            // Meeting point of the inner edges, kept within half of the shorter segment
            float innerLimit = 0.5f * Math.min(lengths[previous], lengths[next]) / Math.max(tanHalf, 1e-6f);
            int inner = vertex(base, innerSide * miter.x, innerSide * miter.y, innerSide, innerLimit);
            int outerPrevious = vertex(base, outerSide * n1.x, outerSide * n1.y, outerSide, StrokeGeometry.NO_LIMIT);
            int outerNext = vertex(base, outerSide * n2.x, outerSide * n2.y, outerSide, StrokeGeometry.NO_LIMIT);
            int center = vertex(base, 0f, 0f, 0f, StrokeGeometry.NO_LIMIT);

            ends[previous] = innerSide > 0 ? new int[]{inner, outerPrevious} : new int[]{outerPrevious, inner};
            starts[next] = innerSide > 0 ? new int[]{inner, outerNext} : new int[]{outerNext, inner};

            // Between the ends of the two quads, down to the inner point
            triangle(inner, outerPrevious, center);
            triangle(inner, center, outerNext);

            EStrokeJoin used = join;
            if (used == EStrokeJoin.MITER && 1f / cosHalf > miterLimit)
                used = EStrokeJoin.BEVEL;

            switch (used) {
                case MITER:
                    int tip = vertex(base, outerSide * miter.x, outerSide * miter.y, outerSide,
                            StrokeGeometry.NO_LIMIT);
                    triangle(center, outerPrevious, tip);
                    triangle(center, tip, outerNext);
                    break;
                case BEVEL:
                    triangle(center, outerPrevious, outerNext);
                    break;
                case ROUND:
                    roundJoin(base, center, outerPrevious, outerNext, n1, n2, outerSide, cross);
                    break;
                default:
                    throw new IllegalStateException("Unknown join " + used);
            }
        }

        // Fan around the point from the outer edge of the previous segment to the one of the next, the short way
        private void roundJoin(Vector2f base, int center, int first, int last, Vector2f n1, Vector2f n2,
                               float outerSide, float cross) {
            double angle = Math.acos(Math.max(-1f, Math.min(1f, n1.dot(n2))));
            int steps = Math.max(1, (int) Math.ceil(angle / ROUND_STEP));
            double turn = Math.signum(cross);
            float startX = outerSide * n1.x;
            float startY = outerSide * n1.y;

            int previous = first;
            for (int step = 1; step < steps; ++step) {
                double t = turn * angle * step / steps;
                float x = (float) (startX * Math.cos(t) - startY * Math.sin(t));
                float y = (float) (startX * Math.sin(t) + startY * Math.cos(t));
                int current = vertex(base, x, y, outerSide, StrokeGeometry.NO_LIMIT);
                triangle(center, previous, current);
                previous = current;
            }
            triangle(center, previous, last);
        }

        // Half disk (sweep pi) or part of a disk around base: from the direction from through toward, its own vertices
        private void fan(Vector2f base, Vector2f from, Vector2f toward, double sweep, float side) {
            int steps = Math.max(1, (int) Math.ceil(sweep / ROUND_STEP));
            int center = vertex(base, 0f, 0f, 0f, StrokeGeometry.NO_LIMIT);
            int previous = -1;
            for (int step = 0; step <= steps; ++step) {
                double t = sweep * step / steps;
                float x = (float) (from.x * Math.cos(t) + toward.x * Math.sin(t));
                float y = (float) (from.y * Math.cos(t) + toward.y * Math.sin(t));
                int current = vertex(base, x, y, side, StrokeGeometry.NO_LIMIT);
                if (previous >= 0)
                    triangle(center, previous, current);
                previous = current;
            }
        }

        private int vertex(Vector2f base, float extrusionX, float extrusionY, float side, float limit) {
            return vertex(base, extrusionX, extrusionY, 0f, 0f, side, INTERIOR, limit);
        }

        private int vertex(Vector2f base, float extrusionX, float extrusionY, float fringeX, float fringeY,
                           float side, float[] along, float limit) {
            bases.add(base.x, base.y);
            extrusions.add(extrusionX, extrusionY);
            fringes.add(fringeX, fringeY);
            sides.add(side);
            alongs.add(along[0], along[1]);
            limits.add(limit);
            return sides.size() - 1;
        }

        private void triangle(int a, int b, int c) {
            indices.add(a);
            indices.add(b);
            indices.add(c);
        }

        StrokeGeometry geometry() {
            int[] indexArray = new int[indices.size()];
            for (int i = 0; i < indexArray.length; ++i)
                indexArray[i] = indices.get(i);

            return new StrokeGeometry(bases.toArray(), extrusions.toArray(), fringes.toArray(), sides.toArray(),
                    alongs.toArray(), limits.toArray(), indexArray);
        }
    }

    // A growing array of floats without boxing
    private static final class FloatList {
        private float[] values = new float[64];
        private int size;

        void add(float value) {
            if (size == values.length) {
                float[] grown = new float[values.length * 2];
                System.arraycopy(values, 0, grown, 0, size);
                values = grown;
            }
            values[size++] = value;
        }

        void add(float first, float second) {
            add(first);
            add(second);
        }

        int size() {
            return size;
        }

        float[] toArray() {
            float[] copy = new float[size];
            System.arraycopy(values, 0, copy, 0, size);
            return copy;
        }
    }
}
