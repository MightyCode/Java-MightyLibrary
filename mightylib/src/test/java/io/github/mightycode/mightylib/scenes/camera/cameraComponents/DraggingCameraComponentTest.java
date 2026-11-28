package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The world follows the mouse from the press to the release, on both axes, by the move of the mouse over the zoom
class DraggingCameraComponentTest {
    private static final InputAction DRAG = InputAction.of("test:drag");

    private final TestCameraInputs inputs = new TestCameraInputs();
    private Camera2D camera;
    private DraggingCameraComponent component;

    @BeforeEach
    void create() throws ReflectiveOperationException {
        camera = new Camera2D(TestCameraInputs.windowInfo(1000, 500), new Vector2f());
        component = new DraggingCameraComponent();
        component.init(inputs.inputs, inputs.mouse, camera);
        component.initActionId(DRAG);
        inputs.mouse.x = 500;
        inputs.mouse.y = 300;
        inputs.tick(component::update);
    }

    private void moveMouse(double x, double y) {
        inputs.mouse.x = x;
        inputs.mouse.y = y;
        inputs.tick(component::update);
    }

    @Test
    void theMouseMovesTheCameraOnlyBetweenThePressAndTheRelease() {
        moveMouse(450, 280);
        assertEquals(new Vector2f(), camera.getCamPosCopy());
        assertFalse(component.isDragging());

        inputs.pressed.add(DRAG);
        inputs.tick(component::update);
        assertTrue(component.isDragging());
        assertEquals(new Vector2f(), camera.getCamPosCopy());

        moveMouse(350, 330);
        assertEquals(new Vector2f(100f, -50f), camera.getCamPosCopy());

        // The release stops the drag before the move of its tick
        inputs.released.add(DRAG);
        moveMouse(0, 0);
        assertFalse(component.isDragging());
        assertEquals(new Vector2f(100f, -50f), camera.getCamPosCopy());
    }

    @Test
    void theMoveOfTheWorldIsTheMoveOfTheMouseOverTheZoom() {
        camera.setZoomReference(EDirection.LeftUp);
        camera.setZoomLevel(new Vector2f(2f, 4f));
        inputs.pressed.add(DRAG);
        inputs.tick(component::update);

        moveMouse(300, 100);
        assertEquals(new Vector2f(100f, 50f), camera.getCamPosCopy());
    }
}
