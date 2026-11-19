package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The triangles of the strokes, for each join and cap, and the vertices placed for a width
class StrokerTest {
    private static final float EPSILON = 1e-4f;
    // Vertices and triangles of a butt or square cap: the two edge vertices, then the fringe quad
    private static final int FLAT_CAP_VERTICES = 6;
    private static final int FLAT_CAP_TRIANGLES = 2;
    private static final int ROUND_STEPS = 16;

    private static List<Vector2f> points(float... coordinates) {
        List<Vector2f> points = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2)
            points.add(new Vector2f(coordinates[i], coordinates[i + 1]));
        return points;
    }

    private static StrokeGeometry stroke(List<Vector2f> points, boolean closed, EStrokeJoin join, EStrokeCap cap) {
        return Stroker.stroke(points, closed, join, cap, Stroker.DEFAULT_MITER_LIMIT);
    }

    private static int triangles(StrokeGeometry geometry) {
        assertEquals(0, geometry.indexCount() % 3);
        return geometry.indexCount() / 3;
    }

    // Twice the signed area of each triangle once placed, none flat
    private static void assertNoDegenerateTriangle(StrokeGeometry geometry, float halfWidth, float fringe) {
        int[] indices = geometry.indices();
        for (int i = 0; i < indices.length; i += 3) {
            Vector2f a = geometry.position(indices[i], halfWidth, fringe);
            Vector2f b = geometry.position(indices[i + 1], halfWidth, fringe);
            Vector2f c = geometry.position(indices[i + 2], halfWidth, fringe);
            float area = Math.abs((b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x));
            assertTrue(area > 1e-5f, "triangle " + i / 3 + " is flat: " + a + " " + b + " " + c);
        }
        for (int index : indices)
            assertTrue(index >= 0 && index < geometry.vertexCount());
    }

    @Test
    void aSegmentWithButtCapsIsAQuadAndTwoFringes() {
        StrokeGeometry geometry = stroke(points(0, 0, 10, 0), false, EStrokeJoin.MITER, EStrokeCap.BUTT);
        assertEquals(2 * FLAT_CAP_VERTICES, geometry.vertexCount());
        assertEquals(2 + 2 * FLAT_CAP_TRIANGLES, triangles(geometry));
        assertNoDegenerateTriangle(geometry, 2f, 0.5f);
    }

    @Test
    void roundCapsAreHalfDisks() {
        StrokeGeometry geometry = stroke(points(0, 0, 10, 0), false, EStrokeJoin.MITER, EStrokeCap.ROUND);
        // Per cap: the two edge vertices, the center, the rim
        assertEquals(2 * (2 + 1 + ROUND_STEPS + 1), geometry.vertexCount());
        assertEquals(2 + 2 * ROUND_STEPS, triangles(geometry));
        assertNoDegenerateTriangle(geometry, 2f, 0.5f);

        // The rim at half the width plus the fringe from the end point
        for (int i = 0; i < geometry.vertexCount(); ++i) {
            if (geometry.sides()[i] != 0f)
                assertEquals(2.5f, distanceToNearestEnd(geometry.position(i, 2f, 0.5f)), EPSILON);
        }
    }

    private static float distanceToNearestEnd(Vector2f point) {
        return Math.min(point.distance(0, 0), point.distance(10, 0));
    }

    @Test
    void theButtCapEndsHalfAFringeAfterTheEndPointAndTheSquareOneHalfAWidth() {
        StrokeGeometry butt = stroke(points(0, 0, 10, 0), false, EStrokeJoin.MITER, EStrokeCap.BUTT);
        StrokeGeometry square = stroke(points(0, 0, 10, 0), false, EStrokeJoin.MITER, EStrokeCap.SQUARE);
        float halfWidth = 2f;
        float fringe = 0.5f;

        assertEquals(10f + fringe, maxX(butt, halfWidth, fringe), EPSILON);
        assertEquals(10f + halfWidth + fringe, maxX(square, halfWidth, fringe), EPSILON);

        // Along: the distance beyond the end goes from inside to +fringe / 2 at the far vertices
        float[] alongs = square.alongs();
        float farthest = -Float.MAX_VALUE;
        for (int i = 0; i < square.vertexCount(); ++i)
            farthest = Math.max(farthest, alongs[i * 2] * halfWidth + alongs[i * 2 + 1] * fringe);
        assertEquals(fringe / 2, farthest, EPSILON);
    }

    private static float maxX(StrokeGeometry geometry, float halfWidth, float fringe) {
        float max = -Float.MAX_VALUE;
        for (int i = 0; i < geometry.vertexCount(); ++i)
            max = Math.max(max, geometry.position(i, halfWidth, fringe).x);
        return max;
    }

    @Test
    void aRightAngleMiterMeetsTheEdgesInsideAndOutside() {
        StrokeGeometry geometry = stroke(points(0, 0, 10, 0, 10, 10), false, EStrokeJoin.MITER, EStrokeCap.BUTT);
        // Join: inner point, two outer vertices, center, tip
        assertEquals(2 * FLAT_CAP_VERTICES + 5, geometry.vertexCount());
        assertEquals(2 * 2 + 2 * FLAT_CAP_TRIANGLES + 4, triangles(geometry));
        assertNoDegenerateTriangle(geometry, 1f, 0.25f);

        List<Vector2f> placed = placed(geometry, 1f, 0f);
        assertTrue(contains(placed, new Vector2f(9, 1)), "inner meeting point");
        assertTrue(contains(placed, new Vector2f(11, -1)), "miter tip");
    }

    @Test
    void bevelAndRoundJoins() {
        StrokeGeometry bevel = stroke(points(0, 0, 10, 0, 10, 10), false, EStrokeJoin.BEVEL, EStrokeCap.BUTT);
        assertEquals(2 * FLAT_CAP_VERTICES + 4, bevel.vertexCount());
        assertEquals(2 * 2 + 2 * FLAT_CAP_TRIANGLES + 3, triangles(bevel));
        assertNoDegenerateTriangle(bevel, 1f, 0.25f);

        // A quarter turn: 8 steps of the fan, 7 vertices between the outer ones
        StrokeGeometry round = stroke(points(0, 0, 10, 0, 10, 10), false, EStrokeJoin.ROUND, EStrokeCap.BUTT);
        assertEquals(2 * FLAT_CAP_VERTICES + 4 + 7, round.vertexCount());
        assertEquals(2 * 2 + 2 * FLAT_CAP_TRIANGLES + 2 + 8, triangles(round));
        assertNoDegenerateTriangle(round, 1f, 0.25f);
        for (Vector2f point : placed(round, 1f, 0f)) {
            if (point.distance(10, 0) > 0.5f && point.x > 10f && point.y < 0f)
                assertEquals(1f, point.distance(10, 0), EPSILON, "the fan is on the circle of the half width");
        }
    }

    @Test
    void aSharpMiterBecomesABevelBeyondTheLimit() {
        // Turn of 170 degrees: the miter would be 11.5 times the width
        double angle = Math.toRadians(170);
        List<Vector2f> sharp = points(0, 0, 10, 0, 10 + 10 * (float) Math.cos(angle), 10 * (float) Math.sin(angle));

        StrokeGeometry limited = Stroker.stroke(sharp, false, EStrokeJoin.MITER, EStrokeCap.BUTT, 4f);
        StrokeGeometry bevel = Stroker.stroke(sharp, false, EStrokeJoin.BEVEL, EStrokeCap.BUTT, 4f);
        assertEquals(bevel.vertexCount(), limited.vertexCount());
        assertEquals(triangles(bevel), triangles(limited));

        StrokeGeometry generous = Stroker.stroke(sharp, false, EStrokeJoin.MITER, EStrokeCap.BUTT, 20f);
        assertEquals(bevel.vertexCount() + 1, generous.vertexCount());
        assertEquals(triangles(bevel) + 1, triangles(generous));
        assertNoDegenerateTriangle(generous, 0.5f, 0.25f);
    }

    @Test
    void aClosedLineHasAJoinAtEveryPointAndNoCap() {
        List<Vector2f> square = points(0, 0, 10, 0, 10, 10, 0, 10);
        StrokeGeometry closed = stroke(square, true, EStrokeJoin.MITER, EStrokeCap.ROUND);
        assertEquals(4 * 5, closed.vertexCount());
        assertEquals(4 * 2 + 4 * 4, triangles(closed));
        assertNoDegenerateTriangle(closed, 1f, 0.25f);

        // The last point repeating the first one changes nothing
        List<Vector2f> repeated = new ArrayList<>(square);
        repeated.add(new Vector2f(0, 0));
        StrokeGeometry same = stroke(repeated, true, EStrokeJoin.MITER, EStrokeCap.ROUND);
        assertEquals(closed.vertexCount(), same.vertexCount());
        assertArrayEquals(closed.indices(), same.indices());
    }

    @Test
    void repeatedPointsAreIgnored() {
        StrokeGeometry once = stroke(points(0, 0, 10, 0), false, EStrokeJoin.ROUND, EStrokeCap.BUTT);
        StrokeGeometry repeated = stroke(points(0, 0, 0, 0, 10, 0, 10, 0, 10, 0), false, EStrokeJoin.ROUND,
                EStrokeCap.BUTT);
        assertEquals(once.vertexCount(), repeated.vertexCount());
        assertArrayEquals(once.indices(), repeated.indices());

        assertTrue(stroke(points(3, 3, 3, 3), false, EStrokeJoin.ROUND, EStrokeCap.ROUND).isEmpty());
        assertTrue(stroke(points(3, 3), true, EStrokeJoin.ROUND, EStrokeCap.ROUND).isEmpty());
        assertTrue(stroke(new ArrayList<>(), false, EStrokeJoin.ROUND, EStrokeCap.ROUND).isEmpty());
    }

    @Test
    void collinearSegmentsShareTheirVertices() {
        StrokeGeometry geometry = stroke(points(0, 0, 5, 0, 10, 0), false, EStrokeJoin.ROUND, EStrokeCap.BUTT);
        assertEquals(2 * FLAT_CAP_VERTICES + 2, geometry.vertexCount());
        assertEquals(2 * 2 + 2 * FLAT_CAP_TRIANGLES, triangles(geometry));
        assertNoDegenerateTriangle(geometry, 1f, 0.25f);
    }

    @Test
    void aUTurnHasAHalfDiskWithTheRoundJoinAndNothingOtherwise() {
        List<Vector2f> back = points(0, 0, 10, 0, 0, 0);
        StrokeGeometry miter = stroke(back, false, EStrokeJoin.MITER, EStrokeCap.BUTT);
        assertEquals(2 * FLAT_CAP_VERTICES + 4, miter.vertexCount());
        assertEquals(2 * 2 + 2 * FLAT_CAP_TRIANGLES, triangles(miter));
        assertNoDegenerateTriangle(miter, 1f, 0.25f);

        StrokeGeometry round = stroke(back, false, EStrokeJoin.ROUND, EStrokeCap.BUTT);
        assertEquals(miter.vertexCount() + 1 + ROUND_STEPS + 1, round.vertexCount());
        assertEquals(triangles(miter) + ROUND_STEPS, triangles(round));
        assertNoDegenerateTriangle(round, 1f, 0.25f);
        // The half disk is beyond the turning point
        assertEquals(11f + 0.25f, maxX(round, 1f, 0.25f), EPSILON);
    }

    @Test
    void theEdgesAreExtrudedByUnitVectorsAcrossTheLine() {
        StrokeGeometry geometry = stroke(points(0, 0, 3, 4), false, EStrokeJoin.MITER, EStrokeCap.BUTT);
        Vector2f direction = new Vector2f(0.6f, 0.8f);
        Vector2f left = new Vector2f(-0.8f, 0.6f);
        float[] extrusions = geometry.extrusions();
        float[] fringes = geometry.fringeExtrusions();
        float[] sides = geometry.sides();
        for (int i = 0; i < geometry.vertexCount(); ++i) {
            Vector2f extrusion = new Vector2f(extrusions[i * 2], extrusions[i * 2 + 1]);
            assertEquals(1f, extrusion.length(), EPSILON);
            assertEquals(1f, Math.abs(sides[i]), EPSILON);
            // Across the direction (0.6, 0.8), on the side given
            assertEquals(0f, extrusion.dot(direction), EPSILON);
            assertEquals(Math.signum(sides[i]), Math.signum(extrusion.dot(left)));
            // The fringe extrusion of a butt cap is along the line
            Vector2f fringe = new Vector2f(fringes[i * 2], fringes[i * 2 + 1]);
            assertTrue(fringe.length() == 0f || Math.abs(Math.abs(fringe.dot(direction)) - 1f) < EPSILON);
        }
    }

    @Test
    void theInnerPointIsLimitedByTheShortSegments() {
        // A sharp turn on short segments: the inner point stays within half the shorter one
        StrokeGeometry geometry = stroke(points(0, 0, 1, 0, 0, 0.2f), false, EStrokeJoin.BEVEL, EStrokeCap.BUTT);
        float[] limits = geometry.limits();
        float smallest = Float.MAX_VALUE;
        for (float limit : limits)
            smallest = Math.min(smallest, limit);
        assertTrue(smallest < 1f);

        for (Vector2f point : placed(geometry, 5f, 0f))
            assertTrue(point.length() < 20f, "no vertex thrown far away: " + point);
    }

    @Test
    void nullsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> Stroker.stroke(null, false, EStrokeJoin.MITER, EStrokeCap.BUTT, 4f));
        assertThrows(IllegalArgumentException.class,
                () -> Stroker.stroke(points(0, 0, 1, 1), false, null, EStrokeCap.BUTT, 4f));
        assertThrows(IllegalArgumentException.class,
                () -> Stroker.stroke(points(0, 0, 1, 1), false, EStrokeJoin.MITER, null, 4f));
    }

    @Test
    void thePackedAttributesFollowTheVertices() {
        StrokeGeometry geometry = stroke(points(0, 0, 10, 0, 10, 10), false, EStrokeJoin.ROUND, EStrokeCap.SQUARE);
        float[] extrusions = geometry.packedExtrusions();
        float[] coverage = geometry.packedCoverage();
        assertEquals(geometry.vertexCount() * 4, extrusions.length);
        assertEquals(geometry.vertexCount() * 4, coverage.length);
        int last = geometry.vertexCount() - 1;
        assertEquals(geometry.extrusions()[last * 2], extrusions[last * 4]);
        assertEquals(geometry.fringeExtrusions()[last * 2 + 1], extrusions[last * 4 + 3]);
        assertEquals(geometry.sides()[last], coverage[last * 4]);
        assertEquals(geometry.limits()[last], coverage[last * 4 + 3]);
        assertEquals(2 * geometry.vertexCount(), geometry.bases().length);
    }

    private static List<Vector2f> placed(StrokeGeometry geometry, float halfWidth, float fringe) {
        List<Vector2f> placed = new ArrayList<>();
        for (int i = 0; i < geometry.vertexCount(); ++i)
            placed.add(geometry.position(i, halfWidth, fringe));
        return placed;
    }

    private static boolean contains(List<Vector2f> points, Vector2f expected) {
        return points.stream().anyMatch(point -> point.distance(expected) < EPSILON);
    }

    @Test
    void everyJoinAndCapGivesTrianglesWithArea() {
        List<Vector2f> zigzag = points(0, 0, 20, 0, 25, 15, 40, 5, 45, 30, 30, 32);
        for (EStrokeJoin join : EStrokeJoin.values()) {
            for (EStrokeCap cap : EStrokeCap.values()) {
                for (boolean closed : Arrays.asList(false, true))
                    assertNoDegenerateTriangle(stroke(zigzag, closed, join, cap), 1.5f, 0.5f);
            }
        }
    }
}
