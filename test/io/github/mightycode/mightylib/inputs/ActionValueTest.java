package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.IAction;
import io.github.mightycode.mightylib.inputs.inputType.InputAnd;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadAxis;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadButton;
import io.github.mightycode.mightylib.inputs.inputType.InputOr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

// Values of the actions bound to a gamepad (no keyboard nor mouse : they need a window)
class ActionValueTest {
    private static final int RIGHT = 100;
    private static final int LEFT = 101;
    private static final int RIGHT_OR_A = 102;
    private static final int A_AND_RIGHT = 103;
    private static final int TRIGGER = 104;

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
        IAction right = new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.POSITIVE);
        IAction a = new InputGamepadButton(GLFW_GAMEPAD_BUTTON_A);
        inputManager.init(new ActionInput[]{
                new ActionInput(RIGHT, "RIGHT", right),
                new ActionInput(LEFT, "LEFT", new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.NEGATIVE)),
                new ActionInput(RIGHT_OR_A, "RIGHT_OR_A", new InputOr(right, a)),
                new ActionInput(A_AND_RIGHT, "A_AND_RIGHT", new InputAnd(a, right)),
                new ActionInput(TRIGGER, "TRIGGER", new InputGamepadAxis(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER, InputGamepadAxis.POSITIVE)),
        });
    }

    // Raw stick value, 0.2 dead zone : 0.6 gives 0.5
    private void stick(float rawX) {
        source.axes.get(0)[GLFW_GAMEPAD_AXIS_LEFT_X] = rawX;
        gamepads.postUpdate();
    }

    @Test
    void axisValueIsProgressiveWithoutThreshold() {
        stick(0.4f);
        assertEquals(0.25f, inputManager.getValue(RIGHT), 1e-6);
        assertFalse(inputManager.getState(RIGHT), "Below the threshold of 0.5 for the state");
        assertEquals(0, inputManager.getValue(LEFT), "Opposite direction");

        stick(-1);
        assertEquals(1, inputManager.getValue(LEFT), 1e-6);
        assertTrue(inputManager.getState(LEFT));
        assertEquals(0, inputManager.getValue(RIGHT));
    }

    @Test
    void triggerValueGoesFromZeroToOne() {
        assertEquals(0, inputManager.getValue(TRIGGER));
        source.axes.get(0)[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = 1;
        gamepads.postUpdate();
        assertEquals(1, inputManager.getValue(TRIGGER), 1e-6);
    }

    @Test
    void buttonValueIsZeroOrOne() {
        stick(0.6f);
        assertEquals(0.5f, inputManager.getValue(RIGHT_OR_A), 1e-6, "Or : the strongest");
        assertEquals(0, inputManager.getValue(A_AND_RIGHT), "And : the weakest, A is released");

        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = true;
        gamepads.postUpdate();
        assertEquals(1, inputManager.getValue(RIGHT_OR_A), 1e-6);
        assertEquals(0.5f, inputManager.getValue(A_AND_RIGHT), 1e-6);
    }

    @Test
    void simulatedActionHasFullValue() {
        inputManager.getSimulation().press(RIGHT, 1);
        inputManager.getSimulation().update();
        assertEquals(1, inputManager.getValue(RIGHT));
    }

    @Test
    void unknownActionHasNoValue() {
        assertEquals(0, inputManager.getValue(999));
    }
}
