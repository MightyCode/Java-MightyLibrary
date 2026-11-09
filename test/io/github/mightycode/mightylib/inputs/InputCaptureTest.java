package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadButton;
import io.github.mightycode.mightylib.registry.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

// While the console captures the inputs, the game sees no action but those of the engine (no keyboard nor mouse here)
class InputCaptureTest {
    private static final InputAction GAME = InputRegistry.action("test_input_capture:game_action");
    private static final InputAction ENGINE = InputRegistry.action(Identifier.of(Identifier.ENGINE_NAMESPACE, "test_input_capture"));

    private FakeGamepadSource source;
    private GamepadManager gamepads;
    private InputManager inputManager;

    @BeforeEach
    void setUp() {
        source = new FakeGamepadSource();
        gamepads = new GamepadManager(source);
        gamepads.init();
        source.plug(0, true);
        gamepads.postUpdate();

        inputManager = new InputManager(null, null, gamepads);
        inputManager.init(new ActionInput[]{
                new ActionInput(GAME, new InputGamepadButton(GLFW_GAMEPAD_BUTTON_A)),
                new ActionInput(ENGINE, new InputGamepadButton(GLFW_GAMEPAD_BUTTON_A)),
        });

        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = true;
        gamepads.postUpdate();
    }

    @Test
    void captureHidesGameActionsButNotEngineOnes() {
        assertTrue(inputManager.getState(GAME));

        inputManager.setCaptured(true);
        assertTrue(inputManager.isCaptured());
        assertFalse(inputManager.getState(GAME));
        assertEquals(0, inputManager.getValue(GAME));
        assertFalse(inputManager.inputPressed(GAME));
        assertTrue(inputManager.getState(ENGINE));
        assertEquals(1, inputManager.getValue(ENGINE));

        inputManager.setCaptured(false);
        assertTrue(inputManager.getState(GAME));
    }

    @Test
    void blockingHidesEngineActionsToo() {
        inputManager.setBlocked(true);
        assertFalse(inputManager.getState(ENGINE));
        assertFalse(inputManager.getState(GAME));
    }

    @Test
    void simulatedGameActionsAreHiddenWhileCaptured() {
        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = false;
        gamepads.postUpdate();
        inputManager.simulatePress(GAME, 2);
        inputManager.updateActionSimulation();

        inputManager.setCaptured(true);
        assertFalse(inputManager.getState(GAME));
        inputManager.setCaptured(false);
        assertTrue(inputManager.getState(GAME));
    }
}
