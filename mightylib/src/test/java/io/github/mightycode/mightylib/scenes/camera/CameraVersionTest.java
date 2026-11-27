package io.github.mightycode.mightylib.scenes.camera;

import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.main.WindowInfo;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

// The values of the matrices of a camera are tracked: each change of a matrix makes a new version of its value
class CameraVersionTest {
    // A window that was never created (virtual size 0): enough for the matrices, no GL
    private static WindowInfo windowInfo() throws ReflectiveOperationException {
        Constructor<WindowInfo> constructor = WindowInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    // The version of the value changes with the call
    private static void assertChanges(ShaderValue value, Runnable change) {
        int before = value.version();
        change.run();
        assertNotEquals(before, value.version());
    }

    @Test
    void eachSetterOfA2DCameraMarksItsView() throws ReflectiveOperationException {
        Camera2D camera = new Camera2D(windowInfo(), new Vector2f(), 1, 10);
        ShaderValue view = camera.getView();
        ShaderValue projection = camera.getProjection();
        assertTrue(view.isTracked());
        assertTrue(projection.isTracked());

        assertChanges(view, () -> camera.setPos(new Vector2f(3, 4)));
        assertChanges(view, () -> camera.setX(5));
        assertChanges(view, () -> camera.setY(6));
        assertChanges(view, () -> camera.moveX(1));
        assertChanges(view, () -> camera.moveY(1));
        assertChanges(view, () -> camera.moveXinZoom(1));
        assertChanges(view, () -> camera.moveYinZoom(1));
        assertChanges(view, () -> camera.setZoomLevel(2));
        assertChanges(view, () -> camera.setZoomLevelX(3));
        assertChanges(view, () -> camera.setZoomLevelY(3));
        assertChanges(view, () -> camera.setRotation(0.5f));
        assertChanges(view, camera::updateView);
        assertChanges(projection, camera::invertView);
        assertChanges(projection, camera::updateProjection);

        // Reading the values is not a change
        int version = view.version();
        assertSame(view, camera.getView());
        assertEquals(version, view.version());
    }

    @Test
    void eachSetterOfA3DCameraMarksItsMatrices() throws ReflectiveOperationException {
        Camera3D camera = new Camera3D(windowInfo(), 70, new Vector3f());
        ShaderValue view = camera.getView();
        ShaderValue projection = camera.getProjection();
        assertTrue(view.isTracked());

        assertChanges(view, () -> camera.setPos(new Vector3f(1, 2, 3)));
        assertChanges(view, () -> camera.setX(1));
        assertChanges(view, () -> camera.setY(1));
        assertChanges(view, () -> camera.setZ(1));
        assertChanges(view, () -> camera.moveX(1));
        assertChanges(view, () -> camera.moveY(1));
        assertChanges(view, () -> camera.moveZ(1));
        assertChanges(view, () -> camera.lookAt(new Vector3f(10, 0, 0)));
        assertChanges(view, camera::updateView);
        assertChanges(projection, () -> camera.setViewAngleDegrees(60));
        assertChanges(projection, camera::updateProjection);
    }
}
