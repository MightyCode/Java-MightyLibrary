package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadButton;
import io.github.mightycode.mightylib.registry.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

// While an owner holds the focus, the game sees no action but those of the engine (no keyboard nor mouse here)
class InputFocusTest {
    private static final InputAction GAME = InputRegistry.action("test_input_focus:game_action");
    private static final InputAction ENGINE = InputRegistry.action(Identifier.of(Identifier.ENGINE_NAMESPACE, "test_input_focus"));

    private FakeGamepadSource source;
    private GamepadManager gamepads;
    private InputManager inputManager;
    private InputFocus focus;

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
        focus = inputManager.focus();

        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = true;
        gamepads.postUpdate();
    }

    // The tick after: the button is held, nothing is pressed any more
    private void nextTick() {
        gamepads.postUpdate();
        inputManager.updateActionSimulation();
        inputManager.resetDroppedEdges();
    }

    @Test
    void sceneHoldsTheFocusByDefault() {
        assertNull(focus.current());
        assertFalse(focus.isHeld());
        assertTrue(inputManager.getState(GAME));
    }

    @Test
    void holdingTheFocusHidesGameActionsButNotEngineOnes() {
        focus.request("owner");
        assertTrue(focus.isHeld());
        assertFalse(inputManager.getState(GAME));
        assertEquals(0, inputManager.getValue(GAME));
        assertFalse(inputManager.inputPressed(GAME));
        assertTrue(inputManager.getState(ENGINE));
        assertEquals(1, inputManager.getValue(ENGINE));

        focus.release("owner");
        assertTrue(inputManager.getState(GAME));
    }

    @Test
    void stackKeepsTheOrder() {
        Object a = new Object();
        Object b = new Object();
        focus.request(a);
        focus.request(b);
        assertTrue(focus.isCurrent(b));
        assertFalse(focus.isCurrent(a));
        assertTrue(focus.isRequested(a));

        // The top gives the focus back to the one below
        focus.release(b);
        assertTrue(focus.isCurrent(a));
        assertTrue(focus.isHeld());

        focus.release(a);
        assertNull(focus.current());
        assertFalse(focus.isHeld());
    }

    @Test
    void requestingAgainMovesTheOwnerUp() {
        Object a = new Object();
        Object b = new Object();
        focus.request(a);
        focus.request(b);
        focus.request(a);
        assertTrue(focus.isCurrent(a));

        focus.release(a);
        assertTrue(focus.isCurrent(b));
    }

    @Test
    void releasingInTheMiddleKeepsTheTop() {
        Object a = new Object();
        Object b = new Object();
        focus.request(a);
        focus.request(b);

        focus.release(a);
        assertTrue(focus.isCurrent(b));
        assertFalse(focus.isRequested(a));

        // Releasing an owner that is not there changes nothing
        focus.release(a);
        assertTrue(focus.isCurrent(b));
    }

    @Test
    void ownerCanNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> focus.request(null));
        assertFalse(focus.isCurrent(null));
    }

    @Test
    void releasedFocusIsBackToTheSceneEvenIfItWasRequestedTwice() {
        focus.request("owner");
        focus.request("owner");
        focus.release("owner");
        assertFalse(focus.isHeld());
    }

    @Test
    void pressOfTheTickIsNotPassedToTheNextHolder() {
        // The button was just pressed: the owner that gives the focus back in this tick is the one who saw it
        assertTrue(inputManager.inputPressed(GAME));
        focus.request("owner");
        focus.release("owner");
        assertFalse(inputManager.inputPressed(GAME));
        assertTrue(inputManager.getState(GAME));

        nextTick();
        assertFalse(inputManager.inputPressed(GAME));
        assertTrue(inputManager.getState(GAME));
    }

    @Test
    void engineActionsLoseTheirPressOfTheTickToo() {
        focus.request("owner");
        assertFalse(inputManager.inputPressed(ENGINE));
    }

    @Test
    void newPressAfterTheChangeIsSeen() {
        focus.request("owner");
        focus.release("owner");
        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = false;
        nextTick();
        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = true;
        nextTick();

        assertTrue(inputManager.inputPressed(GAME));
    }

    @Test
    void releaseOfTheTickIsDroppedToo() {
        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = false;
        nextTick();
        assertTrue(inputManager.inputReleased(GAME));

        focus.request("owner");
        focus.release("owner");
        assertFalse(inputManager.inputReleased(GAME));
    }

    @Test
    void simulatedGameActionsAreHiddenWhileTheFocusIsHeld() {
        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = false;
        gamepads.postUpdate();
        inputManager.simulatePress(GAME, 2);
        inputManager.updateActionSimulation();

        focus.request("owner");
        assertFalse(inputManager.getState(GAME));
        focus.release("owner");
        assertTrue(inputManager.getState(GAME));
    }

    @Test
    void blockingHidesEngineActionsToo() {
        inputManager.setBlocked(true);
        assertFalse(inputManager.getState(ENGINE));
        assertFalse(inputManager.getState(GAME));
    }
}
