package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The wheel zooms around the mouse: the point of the world under the cursor stays under it
class ZoomingCameraComponentTest {
    private static final InputAction QUICK = InputAction.of("test:quick");

    private final TestCameraInputs inputs = new TestCameraInputs();
    private Camera2D camera;
    private ZoomingCameraComponent component;

    @BeforeEach
    void create() throws ReflectiveOperationException {
        // A window of 1000 x 500 pixels showing a virtual size of 2000 x 1000
        camera = new Camera2D(TestCameraInputs.windowInfo(2000, 1000), new Vector2f(30f, -40f));
        component = new ZoomingCameraComponent();
        component.init(inputs.inputs, inputs.mouse, camera, new Vector2i(1000, 500));
        component.initActionId(QUICK);
        inputs.mouse.x = 250;
        inputs.mouse.y = 400;
        inputs.tick(component::update);
    }

    // The cursor at a quarter of the width and four fifths of the height: the same parts of the virtual size
    private Vector2f underTheMouse() {
        return camera.transformPosition(new Vector2f(500f, 800f));
    }

    private void scroll(float y) {
        inputs.mouse.simulateScroll(0, y);
        inputs.tick(component::update);
    }

    @Test
    void aWheelStepZoomsByATenthAroundTheMouse() {
        Vector2f before = underTheMouse();

        scroll(1);
        assertEquals(1.1f, camera.getZoomLevel().x, 1e-5f);
        assertEquals(1.1f, camera.getZoomLevel().y, 1e-5f);
        assertEquals(before.x, underTheMouse().x, 1e-2f);
        assertEquals(before.y, underTheMouse().y, 1e-2f);

        scroll(-2);
        assertEquals(1.1f * 0.8f, camera.getZoomLevel().x, 1e-5f);
        assertEquals(before.x, underTheMouse().x, 1e-2f);
        assertEquals(before.y, underTheMouse().y, 1e-2f);
    }

    @Test
    void withoutWheelNothingChanges() {
        Vector2f position = camera.getCamPosCopy();
        inputs.tick(component::update);
        assertEquals(new Vector2f(1f), camera.getZoomLevel());
        assertEquals(position, camera.getCamPosCopy());
    }

    @Test
    void theQuickActionZoomsTheWidthOnly() {
        inputs.hold(QUICK);
        scroll(1);
        assertEquals(1.1f, camera.getZoomLevel().x, 1e-5f);
        assertEquals(1f, camera.getZoomLevel().y, 1e-5f);
    }

    @Test
    void zoomMultipliesTheCurrentZoom() {
        component.zoom(new Vector2f(2f, 3f));
        component.zoom(new Vector2f(2f, 0.5f));
        assertEquals(new Vector2f(4f, 1.5f), camera.getZoomLevel());
    }
}
