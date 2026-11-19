package io.github.mightycode.mightylib.scenes.camera;

import io.github.mightycode.mightylib.main.WindowInfo;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The zoom of a 2D camera is its own: neither shared with a copy nor with the vectors given or read
class Camera2DZoomTest {
    // A window that was never created (virtual size 0): enough for the zoom, no GL
    private static WindowInfo windowInfo() throws ReflectiveOperationException {
        Constructor<WindowInfo> constructor = WindowInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @Test
    void aCopyHasItsOwnZoom() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f());
        camera.setZoomLevel(2f);
        Camera2D copy = camera.copy();
        assertEquals(new Vector2f(2f), copy.getZoomLevel());

        copy.setZoomLevel(3f);
        assertEquals(new Vector2f(2f), camera.getZoomLevel());
        camera.setZoomLevel(4f);
        assertEquals(new Vector2f(3f), copy.getZoomLevel());
    }

    @Test
    void theVectorsGivenAndReadAreNotKept() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f(), 1, 10);

        Vector2f given = new Vector2f(2f, 3f);
        camera.setZoomLevel(given);
        given.set(7f);
        assertEquals(new Vector2f(2f, 3f), camera.getZoomLevel());

        camera.getZoomLevel().set(9f);
        assertEquals(new Vector2f(2f, 3f), camera.getZoomLevel());

        // Kept between the levels, without changing the vector given
        Vector2f tooFar = new Vector2f(50f, 0.5f);
        camera.setZoomLevel(tooFar);
        assertEquals(new Vector2f(10f, 1f), camera.getZoomLevel());
        assertEquals(new Vector2f(50f, 0.5f), tooFar);
    }
}
