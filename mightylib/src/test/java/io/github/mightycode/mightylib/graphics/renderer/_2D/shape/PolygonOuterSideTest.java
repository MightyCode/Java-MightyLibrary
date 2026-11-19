package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The side of the outside of a closed outline, where the fringe of a fill goes, for either winding
class PolygonOuterSideTest {
    private static List<Vector2f> square() {
        List<Vector2f> square = new ArrayList<>();
        square.add(new Vector2f(0, 0));
        square.add(new Vector2f(10, 0));
        square.add(new Vector2f(10, 10));
        square.add(new Vector2f(0, 10));
        return square;
    }

    @Test
    void theOutsideIsOppositeToTheTurnOfTheOutline() {
        // (0,0) -> (10,0) -> (10,10): the edges turn toward their left normal (-dy, dx), the inside
        assertEquals(-1f, PolygonRenderer.outerSide(square()));

        List<Vector2f> reversed = square();
        Collections.reverse(reversed);
        assertEquals(1f, PolygonRenderer.outerSide(reversed));
    }

    @Test
    void theOuterVerticesOfTheOutlineAreOutsideTheSquare() {
        List<Vector2f> square = square();
        float outer = PolygonRenderer.outerSide(square);
        StrokeGeometry outline = Stroker.stroke(square, true, EStrokeJoin.MITER, EStrokeCap.BUTT, 20f);
        float[] sides = outline.sides();
        for (int i = 0; i < outline.vertexCount(); ++i) {
            if (sides[i] * outer > 0f) {
                Vector2f point = outline.position(i, 0f, 1f);
                boolean outside = point.x < 0f || point.x > 10f || point.y < 0f || point.y > 10f;
                assertTrue(outside, "vertex " + i + " at " + point);
            }
        }
    }
}
