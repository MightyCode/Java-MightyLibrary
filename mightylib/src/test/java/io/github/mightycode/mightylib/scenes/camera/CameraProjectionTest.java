package io.github.mightycode.mightylib.scenes.camera;

import io.github.mightycode.mightylib.main.WindowInfo;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

// A projection computed again replaces the previous one: never composed with it (JOML ortho/perspective multiply)
class CameraProjectionTest {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final float EPSILON = 1e-6f;

    // A window that was never created, of a virtual size of 800 x 600: no GL
    private static WindowInfo windowInfo() throws ReflectiveOperationException {
        Constructor<WindowInfo> constructor = WindowInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        WindowInfo info = constructor.newInstance();
        info.getVirtualSizeRef().set(WIDTH, HEIGHT);
        Field ratio = WindowInfo.class.getDeclaredField("virtualRatio");
        ratio.setAccessible(true);
        ratio.setFloat(info, (float) WIDTH / HEIGHT);
        return info;
    }

    private static void assertMatrix(Matrix4f expected, Matrix4f actual) {
        assertTrue(expected.equals(actual, EPSILON), "expected\n" + expected + "got\n" + actual);
    }

    private static Matrix4f ortho2D(boolean inverted) {
        return inverted ? new Matrix4f().setOrtho(0, WIDTH, 0, HEIGHT, -10, 10)
                : new Matrix4f().setOrtho(0, WIDTH, HEIGHT, 0, -10, 10);
    }

    private static Matrix4f perspective(float fovDegrees) {
        return new Matrix4f().setPerspective((float) Math.toRadians(fovDegrees), (float) WIDTH / HEIGHT, 0.01f, 10000f);
    }

    private static Matrix4f ortho3D() {
        return new Matrix4f().setOrtho(-WIDTH / 2f, WIDTH / 2f, -HEIGHT / 2f, HEIGHT / 2f, -10000f, 10000f);
    }

    @Test
    void a2DProjectionComputedTwiceIsThatOfOneComputation() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f());
        assertMatrix(ortho2D(false), camera.getProjectionMatrix());

        camera.updateProjection();
        camera.updateProjection();
        assertMatrix(ortho2D(false), camera.getProjectionMatrix());
    }

    @Test
    void invertingThe2DViewTwiceGivesTheFirstProjection() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f());

        camera.invertView();
        assertMatrix(ortho2D(true), camera.getProjectionMatrix());
        camera.invertView();
        assertMatrix(ortho2D(false), camera.getProjectionMatrix());
    }

    @Test
    void anInverted2DCameraHasTheInvertedProjection() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f(), true);
        assertMatrix(ortho2D(true), camera.getProjectionMatrix());
    }

    @Test
    void a3DPerspectiveComputedTwiceIsThatOfOneComputation() throws ReflectiveOperationException {
        Camera3D camera = new Camera3D(windowInfo(), 70f, new Vector3f());
        assertMatrix(perspective(70f), camera.getProjectionMatrix());

        camera.updateProjection();
        camera.updateProjection();
        assertMatrix(perspective(70f), camera.getProjectionMatrix());
    }

    @Test
    void anotherAngleGivesItsPerspectiveOnly() throws ReflectiveOperationException {
        Camera3D camera = new Camera3D(windowInfo(), 70f, new Vector3f());

        camera.setViewAngleDegrees(60);
        assertMatrix(perspective(60), camera.getProjectionMatrix());
        assertEquals(60f, camera.getViewAngleDegrees());
    }

    // Bug: the angle in degrees went to JOML as radians (120 gave a view of about 35 degrees)
    @Test
    void theViewAngleIsInDegrees() throws ReflectiveOperationException {
        Camera3D camera = new Camera3D(windowInfo(), 90f, new Vector3f());
        // A vertical view of 90 degrees: tan(45) = 1, so the y scale of the projection is 1
        assertEquals(1f, camera.getProjectionMatrix().m11(), 1e-5f);
    }

    @Test
    void theViewModeGoesToTheOrthographicProjectionAndBack() throws ReflectiveOperationException {
        Camera3D camera = new Camera3D(windowInfo(), 1.2f, new Vector3f());

        camera.setViewMode(Camera3D.EViewMode.Orthographic);
        assertMatrix(ortho3D(), camera.getProjectionMatrix());
        camera.updateProjection();
        assertMatrix(ortho3D(), camera.getProjectionMatrix());

        camera.setViewMode(Camera3D.EViewMode.Perspective);
        assertMatrix(perspective(1.2f), camera.getProjectionMatrix());
    }
}
