package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The regular polygons of the shaders: their bounding box, stretched onto the size of the shape
class RegularPolygonShapeTest {
    private static final float EPSILON = 1e-5f;
    private static final float HALF_SQRT_3 = (float) Math.sqrt(3) / 2;

    @Test
    void theHexagonFromAngleZeroIsRegularWithTheStretchRatio() {
        Vector4f box = RegularPolygonShape.unitBoundingBox(6, 0f);
        assertEquals(-1f, box.x, EPSILON);
        assertEquals(1f, box.y, EPSILON);
        assertEquals(-HALF_SQRT_3, box.z, EPSILON);
        assertEquals(HALF_SQRT_3, box.w, EPSILON);
        // Height over width of the box
        assertEquals(HexagonRenderer.STRETCH_RATIO, (box.w - box.z) / (box.y - box.x), EPSILON);
    }

    @Test
    void aTrianglePointingUpHasItsBoxBelowTheCenterOfTheCircle() {
        Vector4f box = RegularPolygonShape.unitBoundingBox(3, (float) (-Math.PI / 2));
        assertEquals(-HALF_SQRT_3, box.x, EPSILON);
        assertEquals(HALF_SQRT_3, box.y, EPSILON);
        // The apex at the top (y down), the base at half the radius
        assertEquals(-1f, box.z, EPSILON);
        assertEquals(0.5f, box.w, EPSILON);
    }

    @Test
    void theSidesAreKeptInTheRangeOfTheShader() {
        assertEquals(3, RegularPolygonShape.clampSides(1));
        assertEquals(64, RegularPolygonShape.clampSides(1000));

        Vector4f first = new Vector4f();
        Vector4f second = new Vector4f();
        RegularPolygonShape.write(2, 0.5f, first, second);
        assertEquals(3f, first.x);
        assertEquals(0.5f, first.y);
        assertEquals(RegularPolygonShape.unitBoundingBox(3, 0.5f), second);
    }
}
