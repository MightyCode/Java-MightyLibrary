package io.github.mightycode.mightylib.utils.math.geometry;

import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeometryMathTest {
    private static final Vector2f ORIGIN = new Vector2f(0, 0);

    @Test
    void crossProductOfTheTwoSidesAroundB() {
        // B = (1,0) is the corner: BA = (-1,0), BC = (0,1) -> -1
        assertEquals(-1f, GeometryMath.CrossProductLength(0, 0, 1, 0, 1, 1));
        assertEquals(1f, GeometryMath.CrossProductLength(1, 1, 1, 0, 0, 0), "swapping A and C changes the sign");
        assertEquals(0f, GeometryMath.CrossProductLength(0, 0, 1, 1, 2, 2), "aligned points");
        assertEquals(-1f, GeometryMath.CrossProductLength(new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1)));
    }

    @Test
    void crossProductGrowsWithTheSides() {
        assertEquals(-6f, GeometryMath.CrossProductLength(0, 0, 2, 0, 2, 3));
    }

    @Test
    void convexCornerIsANegativeCrossProduct() {
        assertTrue(GeometryMath.IsConvex(0, 0, 1, 0, 1, 1));
        assertFalse(GeometryMath.IsConvex(1, 1, 1, 0, 0, 0));
        assertFalse(GeometryMath.IsConvex(0, 0, 1, 1, 2, 2), "aligned points are not a corner");
        assertTrue(GeometryMath.IsConvex(new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1)));
    }

    @Test
    void everyCornerOfATriangleIsAnEarWhenTheOrderIsCounterClockwise() {
        List<Vector2f> triangle = Arrays.asList(ORIGIN, new Vector2f(1, 0), new Vector2f(0, 1));
        for (int i = 0; i < 3; ++i)
            assertTrue(GeometryMath.isEar(i, triangle));
    }

    @Test
    void noCornerIsAnEarWhenTheOrderIsClockwise() {
        List<Vector2f> square = Arrays.asList(ORIGIN, new Vector2f(0, 1), new Vector2f(1, 1), new Vector2f(1, 0));
        for (int i = 0; i < 4; ++i)
            assertFalse(GeometryMath.isEar(i, square));
    }

    @Test
    void theReflexCornerOfAnLShapeIsNotAnEar() {
        List<Vector2f> shape = Arrays.asList(ORIGIN, new Vector2f(2, 0), new Vector2f(2, 1),
                new Vector2f(1, 1), new Vector2f(1, 2), new Vector2f(0, 2));
        assertFalse(GeometryMath.isEar(3, shape));
        assertTrue(GeometryMath.isEar(1, shape));
    }

    @Test
    void theFirstVertexUsesTheLastOneAsPredecessor() {
        List<Vector2f> square = Arrays.asList(ORIGIN, new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1));
        assertTrue(GeometryMath.isEar(0, square));
        assertTrue(GeometryMath.isEar(3, square));
    }
}
