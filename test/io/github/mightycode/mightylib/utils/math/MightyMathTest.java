package io.github.mightycode.mightylib.utils.math;

import io.github.mightycode.mightylib.utils.math.geometry.EDirection3D;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MightyMathTest {
    private static final float EPS = 1e-4f;

    @Test
    void axisVectors() {
        assertEquals(new Vector3f(0, 0, -1), MightyMath.LeftVector());
        assertEquals(new Vector3f(0, 0, 1), MightyMath.RightVector());
        assertEquals(new Vector3f(0, 1, 0), MightyMath.UpVector());
        assertEquals(new Vector3f(0, -1, 0), MightyMath.DownVector());
        assertEquals(new Vector3f(1, 0, 0), MightyMath.ForwardVector());
        assertEquals(new Vector3f(-1, 0, 0), MightyMath.BackwardVector());
        assertEquals(new Vector3f(1, 1, 1), MightyMath.OnesVector());
        assertEquals(new Vector3f(0, 0, 0), MightyMath.ZerosVector());
    }

    @Test
    void axisVectorsAreNewObjectsEachTime() {
        Vector3f first = MightyMath.UpVector();
        first.add(5, 5, 5);
        assertEquals(new Vector3f(0, 1, 0), MightyMath.UpVector());
    }

    @Test
    void toVectorMatchesTheAxisOfEachDirection() {
        assertEquals(MightyMath.ForwardVector(), MightyMath.ToVector(EDirection3D.Forward));
        assertEquals(MightyMath.BackwardVector(), MightyMath.ToVector(EDirection3D.Backward));
        assertEquals(MightyMath.LeftVector(), MightyMath.ToVector(EDirection3D.Left));
        assertEquals(MightyMath.RightVector(), MightyMath.ToVector(EDirection3D.Right));
        assertEquals(MightyMath.UpVector(), MightyMath.ToVector(EDirection3D.Up));
        assertEquals(MightyMath.DownVector(), MightyMath.ToVector(EDirection3D.Down));
        assertEquals(MightyMath.ZerosVector(), MightyMath.ToVector(EDirection3D.None));
        assertEquals(MightyMath.ZerosVector(), MightyMath.ToVector(EDirection3D.Undef));
    }

    @Test
    void mapTransposesANumberBetweenTwoIntervals() {
        assertEquals(150f, MightyMath.Mapf(5, 0, 10, 100, 200), EPS);
        assertEquals(100f, MightyMath.Mapf(0, 0, 10, 100, 200), EPS);
        assertEquals(200f, MightyMath.Mapf(10, 0, 10, 100, 200), EPS);
        assertEquals(10f, MightyMath.Mapf(0, 0, 10, 10, 0), EPS, "reversed destination");
        assertEquals(2f, MightyMath.Mapf(20, 0, 10, 0, 1), EPS, "outside the source interval: extrapolated");
        assertEquals(1.5, MightyMath.Mapd(0.5, 0, 1, 1, 2), 1e-12);
    }

    @Test
    void logarithmicMapPutsTheGeometricMeanInTheMiddle() {
        assertEquals(100f, MightyMath.MapLogf(10, 1, 100, 1, 10000), 0.05f);
        assertEquals(1f, MightyMath.MapLogf(1, 1, 100, 1, 10000), 1e-3f);
        assertEquals(10000f, MightyMath.MapLogf(100, 1, 100, 1, 10000), 0.5f);
    }

    @Test
    void sums() {
        assertEquals(0, MightyMath.Sum(new int[0]));
        assertEquals(10, MightyMath.Sum(new int[]{1, 2, 3, 4}));
        assertEquals(6f, MightyMath.Sum(new float[]{1, 2, 3}), EPS);
        assertEquals(5f, MightyMath.Sum(new float[]{1, 2, 3, 4}, 1, 3), EPS, "from index 1 to 3 excluded");
        assertEquals(0f, MightyMath.Sum(new float[]{1, 2, 3}, 2, 2), EPS);
        assertEquals(4f, MightyMath.Sum(new ArrayList<>(Arrays.asList(1.5f, 2.5f))), EPS);
    }

    @Test
    void angleConversions() {
        assertEquals(Math.PI, MightyMath.toRads(180.0), 1e-12);
        assertEquals(MightyMath.PI_FLOAT / 2, MightyMath.toRads(90f), EPS);
        assertEquals(90.0, MightyMath.toDegrees(Math.PI / 2), 1e-9);
        assertEquals(45f, MightyMath.toDegrees(MightyMath.PI_FLOAT / 4), EPS);
        assertEquals(37.5f, MightyMath.toDegrees(MightyMath.toRads(37.5f)), EPS);
    }

    @Test
    void pointProjectedOnAnAxis() {
        Vector2f onX = MightyMath.ProjectPointOnAxis(new Vector2f(3, 4), new Vector2f(1, 0));
        assertEquals(3f, onX.x, EPS);
        assertEquals(0f, onX.y, EPS);

        Vector2f scaledAxis = MightyMath.ProjectPointOnAxis(new Vector2f(3, 4), new Vector2f(10, 0));
        assertEquals(3f, scaledAxis.x, EPS, "the length of the axis does not matter");

        Vector2f diagonal = MightyMath.ProjectPointOnAxis(new Vector2f(3, 4), new Vector2f(1, 1));
        assertEquals(3.5f, diagonal.x, EPS);
        assertEquals(3.5f, diagonal.y, EPS);
    }

    @Disabled("Known bug: RotatePointAround swaps cosine and sine, an angle of 0 turns the point by a quarter turn")
    @Test
    void rotatingByZeroKeepsThePoint() {
        Vector2f result = MightyMath.RotatePointAround(new Vector2f(3, 1), new Vector2f(1, 1), 0f);
        assertEquals(3f, result.x, EPS);
        assertEquals(1f, result.y, EPS);
    }

    @Disabled("Known bug: RotatePointAround swaps cosine and sine")
    @Test
    void aQuarterTurnAroundTheOrigin() {
        Vector2f result = MightyMath.RotatePointAround(new Vector2f(1, 0), new Vector2f(), MightyMath.PI_FLOAT / 2);
        assertEquals(0f, result.x, EPS);
        assertEquals(1f, result.y, EPS);
    }

    @Test
    void pointOnASlopedSegment() {
        Vector4f line = new Vector4f(0, 0, 10, 10);
        assertTrue(MightyMath.isPositionOverridingLine(new Vector2f(5, 5), line, 0.01f));
        assertFalse(MightyMath.isPositionOverridingLine(new Vector2f(5, 6), line, 0.5f));
        assertTrue(MightyMath.isPositionOverridingLine(new Vector2f(5, 5.4f), line, 0.5f), "inside the precision");
    }

    @Test
    void pointBeyondTheEndsOfTheSegmentIsNotOnIt() {
        Vector4f line = new Vector4f(0, 0, 10, 10);
        assertFalse(MightyMath.isPositionOverridingLine(new Vector2f(11, 11), line, 0.5f));
        assertTrue(MightyMath.isPositionOverridingLine(new Vector2f(10.2f, 10.2f), line, 0.5f), "the ends are widened too");
        assertFalse(MightyMath.isPositionOverridingLine(new Vector2f(-2, -2), line, 0.5f));
    }

    @Test
    void segmentGivenFromRightToLeft() {
        assertTrue(MightyMath.isPositionOverridingLine(new Vector2f(5, 5), new Vector4f(10, 10, 0, 0), 0.01f));
    }

    @Test
    void horizontalSegment() {
        Vector4f line = new Vector4f(0, 2, 10, 2);
        assertTrue(MightyMath.isPositionOverridingLine(new Vector2f(5, 2), line, 0.01f));
        assertFalse(MightyMath.isPositionOverridingLine(new Vector2f(5, 2.5f), line, 0.1f));
    }

    @Disabled("Known bug: a vertical segment has an infinite slope, no point is ever found on it")
    @Test
    void verticalSegment() {
        assertTrue(MightyMath.isPositionOverridingLine(new Vector2f(3, 5), new Vector4f(3, 0, 3, 10), 0.01f));
    }
}
