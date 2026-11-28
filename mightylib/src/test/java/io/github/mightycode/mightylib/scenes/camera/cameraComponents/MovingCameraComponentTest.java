package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.main.utils.GameTimeControl;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovingCameraComponentTest {
    private static final InputAction LEFT = InputAction.of("test:left");
    private static final InputAction RIGHT = InputAction.of("test:right");
    private static final InputAction UP = InputAction.of("test:up");
    private static final InputAction DOWN = InputAction.of("test:down");
    private static final InputAction QUICK = InputAction.of("test:quick");

    private final TestCameraInputs inputs = new TestCameraInputs();

    @AfterEach
    void reset() {
        GameTimeControl.reset();
    }

    private MovingCameraComponent component(MovingCameraComponent component, Camera2D camera) {
        component.init(inputs.inputs, camera);
        component.initActionIds(new MovingCameraComponent.Inputs().setMoveLeft(LEFT).setMoveRight(RIGHT)
                .setMoveUp(UP).setMoveDown(DOWN).setQuickSpeed(QUICK));
        return component;
    }

    @Test
    void diagonalIsNotFaster() {
        Vector2f diagonal = MovingCameraComponent.ClampLength(new Vector2f(1, -1));
        assertEquals(1, diagonal.length(), 1e-6);
        assertEquals(-diagonal.x, diagonal.y, 1e-6);
    }

    @Test
    void partialStickIsKept() {
        Vector2f partial = MovingCameraComponent.ClampLength(new Vector2f(0.3f, 0.4f));
        assertEquals(0.3f, partial.x, 1e-6);
        assertEquals(0.4f, partial.y, 1e-6);
    }

    // A stick pushed half way moves at half the speed, on each axis
    @Test
    void aStickMovesInProportionToItsValue() throws ReflectiveOperationException {
        GameTimeControl.setDeltaTime(0.5f);
        Camera2D camera = new Camera2D(TestCameraInputs.windowInfo(1000, 500), new Vector2f());
        MovingCameraComponent moving = component(new MovingCameraComponent(100f, 400f), camera);

        inputs.values.put(RIGHT, 0.5f);
        inputs.values.put(UP, 0.25f);
        moving.update();
        assertEquals(25f, camera.getCamPosCopy().x, 1e-4f);
        assertEquals(-12.5f, camera.getCamPosCopy().y, 1e-4f);

        // Two opposite directions cancel each other
        camera.setPos(new Vector2f());
        inputs.values.put(LEFT, 0.5f);
        inputs.values.put(DOWN, 0.25f);
        moving.update();
        assertEquals(new Vector2f(), camera.getCamPosCopy());
    }

    @Test
    void theSpeedsAreThoseGivenAndTheMoveIsOnTheScreen() throws ReflectiveOperationException {
        GameTimeControl.setDeltaTime(0.1f);
        Camera2D camera = new Camera2D(TestCameraInputs.windowInfo(1000, 500), new Vector2f());
        camera.setZoomReference(EDirection.LeftUp);
        camera.setZoomLevel(2f);
        MovingCameraComponent moving = component(new MovingCameraComponent(), camera);
        assertEquals(MovingCameraComponent.MOVE_SPEED, moving.getMoveSpeed());
        assertEquals(MovingCameraComponent.QUICK_SPEED, moving.getQuickSpeed());

        moving.setMoveSpeed(200f);
        moving.setQuickSpeed(1000f);
        inputs.hold(DOWN);
        moving.update();
        // 200 pixels per second for a tenth of a second, at zoom 2
        assertEquals(10f, camera.getCamPosCopy().y, 1e-4f);

        inputs.hold(QUICK);
        moving.update();
        assertEquals(60f, camera.getCamPosCopy().y, 1e-4f);
        assertEquals(0f, camera.getCamPosCopy().x, 1e-4f);
    }
}
