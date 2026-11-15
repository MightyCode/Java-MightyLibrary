package io.github.mightycode.mightylib.utils.math.geometry;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PolygonTest {
    // Counter-clockwise with y up: the order the ear clipping wants (see GeometryMathTest)
    private static final float[] SQUARE = {0, 0, 1, 0, 1, 1, 0, 1};
    // L shape of area 3 with a reflex vertex at (1,1)
    private static final float[] L_SHAPE = {0, 0, 2, 0, 2, 1, 1, 1, 1, 2, 0, 2};

    private static float area(float[] vertices, int[] indices) {
        float total = 0;
        for (int i = 0; i < indices.length; i += 3) {
            Vector2f a = new Vector2f(vertices[indices[i] * 2], vertices[indices[i] * 2 + 1]);
            Vector2f b = new Vector2f(vertices[indices[i + 1] * 2], vertices[indices[i + 1] * 2 + 1]);
            Vector2f c = new Vector2f(vertices[indices[i + 2] * 2], vertices[indices[i + 2] * 2 + 1]);
            total += Math.abs((b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y)) / 2;
        }
        return total;
    }

    @Test
    void builderCollectsTheVertices() {
        Polygon.PolygonBuilder builder = Polygon.CreateBuilder()
                .addVertex(new Vector2f(0, 0)).addVertex(new Vector2f(4, 0)).addVertex(new Vector2f(4, 3)).addVertex(new Vector2f(9, 9));
        assertEquals(4, builder.size());
        assertEquals(new Vector2f(4, 3), builder.get(2));

        assertEquals(new Vector2f(9, 9), builder.pop());
        builder.removeVertex(0);
        assertEquals(2, builder.size());

        Polygon polygon = builder.build();
        assertEquals(2, polygon.size());
        assertEquals(new Vector2f(4, 0), polygon.get(0));
        assertEquals(new Vector2f(4, 3), polygon.get(1));
    }

    @Test
    void boundingBoxCoversAllVertices() {
        Polygon polygon = new Polygon(new float[]{1, 5, -2, 3, 4, -1, 0, 8});
        assertEquals(new Vector4f(-2, -1, 4, 8), polygon.getBoundingBox());
    }

    @Test
    void boundingBoxOfASinglePoint() {
        assertEquals(new Vector4f(3, 4, 3, 4), new Polygon(new float[]{3, 4}).getBoundingBox());
    }

    @Test
    void boundingBoxIsACopy() {
        Polygon polygon = new Polygon(SQUARE);
        polygon.getBoundingBox().set(9, 9, 9, 9);
        assertEquals(new Vector4f(0, 0, 1, 1), polygon.getBoundingBox());
    }

    @Test
    void getReturnsACopyOfTheVertex() {
        Polygon polygon = new Polygon(SQUARE);
        polygon.get(1).set(7, 7);
        assertEquals(new Vector2f(1, 0), polygon.get(1));
    }

    @Test
    void indexPastTheEndGivesNull() {
        assertNull(new Polygon(SQUARE).get(4));
    }

    @Test
    void convexAndConcavePolygons() {
        assertTrue(new Polygon(SQUARE).isConvex());
        assertTrue(new Polygon(new float[]{0, 0, 0, 1, 1, 1, 1, 0}).isConvex(), "either winding order");
        assertTrue(new Polygon(new float[]{0, 0, 4, 0, 2, 3}).isConvex());
        assertFalse(new Polygon(L_SHAPE).isConvex());
    }

    @Test
    void earClippingATriangleGivesItself() {
        int[] indices = new Polygon(new float[]{0, 0, 1, 0, 0, 1}).earClipIndices();
        assertEquals(3, indices.length);
        assertEquals(0.5f, area(new float[]{0, 0, 1, 0, 0, 1}, indices), 1e-5);
    }

    @Test
    void earClippingASquareGivesTwoTrianglesOfTheSameArea() {
        int[] indices = new Polygon(SQUARE).earClipIndices();
        assertEquals(6, indices.length);
        assertEquals(1f, area(SQUARE, indices), 1e-5);
        for (int index : indices)
            assertTrue(index >= 0 && index < 4);
    }

    @Test
    void earClippingAConvexPolygonCoversItsArea() {
        // regular-ish hexagon, counter-clockwise: area computed by the shoelace formula
        float[] hexagon = {2, 0, 1, 1.5f, -1, 1.5f, -2, 0, -1, -1.5f, 1, -1.5f};
        int[] indices = new Polygon(hexagon).earClipIndices();
        assertEquals(12, indices.length);
        assertEquals(9f, area(hexagon, indices), 1e-4);
    }

    @Test
    void earClippingAClockwisePolygonGivesTheSameArea() {
        float[] clockwise = {0, 2, 1, 2, 1, 1, 2, 1, 2, 0, 0, 0};
        int[] indices = new Polygon(clockwise).earClipIndices();
        assertEquals(12, indices.length);
        assertEquals(3f, area(clockwise, indices), 1e-4);
    }

    @Test
    void earClippingAConcavePolygonCoversExactlyItsArea() {
        int[] indices = new Polygon(L_SHAPE).earClipIndices();
        assertEquals(12, indices.length);
        assertEquals(3f, area(L_SHAPE, indices), 1e-4);
    }
}
