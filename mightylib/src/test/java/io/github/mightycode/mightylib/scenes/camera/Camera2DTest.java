package io.github.mightycode.mightylib.scenes.camera;

import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// A 2D camera without window: screen to world under translation, zoom and rotation, the point kept by the zoom
class Camera2DTest {
    private static final float EPSILON = 1e-3f;

    // A window that was never created, with a virtual size of 1000 x 500: no GL
    private static WindowInfo windowInfo() throws ReflectiveOperationException {
        Constructor<WindowInfo> constructor = WindowInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        WindowInfo info = constructor.newInstance();
        info.getVirtualSizeRef().set(1000, 500);
        return info;
    }

    private static void assertVector(Vector2f expected, Vector2f actual) {
        assertEquals(expected.x, actual.x, EPSILON, "x of " + actual);
        assertEquals(expected.y, actual.y, EPSILON, "y of " + actual);
    }

    // Where the view puts a point of the world on the screen (virtual pixels)
    private static Vector2f onScreen(Camera2D camera, Vector2f world) {
        Vector4f screen = camera.getViewMatrix().transform(new Vector4f(world, 0f, 1f));
        return new Vector2f(screen.x, screen.y);
    }

    @Test
    void aScreenPositionIsTheWorldPositionUnderAnIdentityCamera() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f());
        assertVector(new Vector2f(120f, 80f), camera.transformPosition(new Vector2f(120f, 80f)));
    }

    @Test
    void theScreenIsTheWorldMovedByThePositionThenDividedByTheZoom() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f(100f, 50f));
        assertVector(new Vector2f(100f, 50f), camera.transformPosition(new Vector2f()));
        assertVector(new Vector2f(110f, 70f), camera.transformPosition(new Vector2f(10f, 20f)));

        camera.setZoomReference(EDirection.LeftUp);
        camera.setZoomLevel(2f);
        // The corner kept by the zoom: the position does not move
        assertVector(new Vector2f(100f, 50f), camera.getCamPosCopy());
        assertVector(new Vector2f(105f, 60f), camera.transformPosition(new Vector2f(10f, 20f)));

        camera.setZoomLevel(new Vector2f(4f, 0.5f));
        assertVector(new Vector2f(102.5f, 90f), camera.transformPosition(new Vector2f(10f, 20f)));
    }

    // The rotation turns the world around its origin, before the move and the zoom
    @Test
    void theRotationTurnsTheWorldAroundItsOrigin() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f());
        camera.setRotation((float) Math.PI / 2f);
        assertVector(new Vector2f(0f, -10f), camera.transformPosition(new Vector2f(10f, 0f)));
        assertEquals((float) Math.PI / 2f, camera.getRotation(), EPSILON);
    }

    // transformPosition is the inverse of the view, whatever the position, zoom and rotation
    @Test
    void screenToWorldIsTheInverseOfTheView() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f(-340f, 75f));
        camera.setZoomLevel(new Vector2f(1.7f, 0.6f));
        camera.setRotation(0.4f);

        for (Vector2f screen : new Vector2f[]{new Vector2f(), new Vector2f(1000f, 500f), new Vector2f(313f, -42f)})
            assertVector(screen, onScreen(camera, camera.transformPosition(screen)));
    }

    // The point of the screen at the zoom reference (parts of the virtual size) shows the same point of the world
    @Test
    void theZoomKeepsTheWorldPointUnderItsReference() throws ReflectiveOperationException {
        Object[][] cases = {
                {EDirection.None, new Vector2f(500f, 250f)},
                {EDirection.LeftUp, new Vector2f(0f, 0f)},
                {EDirection.Up, new Vector2f(500f, 0f)},
                {EDirection.RightUp, new Vector2f(1000f, 0f)},
                {EDirection.Left, new Vector2f(0f, 250f)},
                {EDirection.Right, new Vector2f(1000f, 250f)},
                {EDirection.LeftDown, new Vector2f(0f, 500f)},
                {EDirection.Down, new Vector2f(500f, 500f)},
                {EDirection.RightDown, new Vector2f(1000f, 500f)},
        };
        for (Object[] testCase : cases) {
            Camera2D camera = new Camera2D(windowInfo(), new Vector2f(30f, -20f));
            camera.setZoomReference((EDirection) testCase[0]);
            Vector2f screen = (Vector2f) testCase[1];
            Vector2f before = camera.transformPosition(screen);

            camera.setZoomLevel(2f);
            assertVector(before, camera.transformPosition(screen));
            camera.setZoomLevel(new Vector2f(0.5f, 3f));
            assertVector(before, camera.transformPosition(screen));
        }
    }

    @Test
    void aReferenceGivenInPartsOfTheScreenIsKeptToo() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f());
        camera.setZoomReference(new Vector2f(0.25f, 0.75f));
        Vector2f screen = new Vector2f(250f, 375f);
        Vector2f before = camera.transformPosition(screen);

        camera.setZoomLevelX(3f);
        camera.setZoomLevelY(1.5f);
        assertVector(before, camera.transformPosition(screen));
        assertEquals(new Vector2f(3f, 1.5f), camera.getZoomLevel());
    }

    @Test
    void theZoomIsKeptBetweenItsLevelsOnBothAxes() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f(), 1, 10);
        camera.setZoomLevel(new Vector2f(0.5f, 20f));
        assertEquals(new Vector2f(1f, 10f), camera.getZoomLevel());
    }

    // A move in zoom is a move on the screen: the world moves by the value over the zoom
    @Test
    void aMoveInZoomIsDividedByTheZoom() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f());
        camera.setZoomReference(EDirection.LeftUp);
        camera.setZoomLevel(new Vector2f(2f, 4f));
        camera.moveXinZoom(100f);
        camera.moveYinZoom(100f);
        assertVector(new Vector2f(50f, 25f), camera.getCamPosCopy());

        camera.moveX(10f);
        camera.moveY(-5f);
        assertVector(new Vector2f(60f, 20f), camera.getCamPosCopy());
    }

    // A copy shows the same: same view, same projection, even zoomed around the center (the copy of a zoomed camera
    // was moved by its zoom from 1), then zooms the same way (reference and levels)
    @Test
    void aCopyShowsTheSameAsTheCamera() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f(40f, 60f), 1, 3);
        camera.invertView();
        camera.setZoomLevel(2f);
        camera.setRotation(0.3f);
        camera.setZoomReference(new Vector2f(0.2f, 0.9f));

        Camera2D copy = camera.copy();
        assertVector(camera.getCamPosCopy(), copy.getCamPosCopy());
        assertEquals(camera.getRotation(), copy.getRotation());
        assertMatrix(camera.getViewMatrix(), copy.getViewMatrix());
        assertMatrix(camera.getProjectionMatrix(), copy.getProjectionMatrix());

        camera.setZoomLevel(5f);
        copy.setZoomLevel(5f);
        assertEquals(new Vector2f(3f), copy.getZoomLevel());
        assertMatrix(camera.getViewMatrix(), copy.getViewMatrix());
    }

    private static void assertMatrix(Matrix4f expected, Matrix4f actual) {
        assertTrue(expected.equals(actual, EPSILON), "expected\n" + expected + "actual\n" + actual);
    }
}
