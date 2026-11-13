package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisButtons;
import io.github.mightycode.mightylib.inputs.inputType.AxisGamepad;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisSum;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadAxis;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

// Axes bound to a gamepad (the mouse and the keyboard need a window ; perSecond needs the game time)
class AxisTest {
    // The registry is shared by the whole JVM : identifiers unique to this class
    private static final InputAxis STICK = InputRegistry.axis("test_axis:stick");
    private static final InputAxis CURVED = InputRegistry.axis("test_axis:curved");
    private static final InputAxis INVERTED = InputRegistry.axis("test_axis:inverted");
    private static final InputAxis BUTTONS = InputRegistry.axis("test_axis:buttons");
    private static final InputAxis SUM = InputRegistry.axis("test_axis:sum");
    private static final InputAxis NOT_BOUND = InputRegistry.axis("test_axis:not_bound");

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
        inputManager.init(new ActionInput[0], new AxisInput[]{
                new AxisInput(STICK, new AxisGamepad(GLFW_GAMEPAD_AXIS_RIGHT_X)),
                new AxisInput(CURVED, new AxisGamepad(GLFW_GAMEPAD_AXIS_RIGHT_X).curve(2).scale(10)),
                new AxisInput(INVERTED, new AxisGamepad(GLFW_GAMEPAD_AXIS_RIGHT_X).scale(-1)),
                new AxisInput(BUTTONS, new AxisButtons(
                        new InputGamepadButton(GLFW_GAMEPAD_BUTTON_DPAD_LEFT),
                        new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.POSITIVE))),
                new AxisInput(SUM, new AxisSum(
                        new AxisGamepad(GLFW_GAMEPAD_AXIS_RIGHT_X),
                        new AxisGamepad(GLFW_GAMEPAD_AXIS_LEFT_X)).scale(2)),
        });
    }

    // Raw values, 0.2 dead zone : 0.6 gives 0.5, -0.6 gives -0.5
    private void axes(float rightX, float leftX) {
        source.axes.get(0)[GLFW_GAMEPAD_AXIS_RIGHT_X] = rightX;
        source.axes.get(0)[GLFW_GAMEPAD_AXIS_LEFT_X] = leftX;
        gamepads.postUpdate();
    }

    @Test
    void axisIsSigned() {
        axes(-0.6f, 0);
        assertEquals(-0.5f, inputManager.getAxis(STICK), 1e-6);
        assertEquals(0.5f, inputManager.getAxis(INVERTED), 1e-6);
    }

    @Test
    void curveKeepsTheSignThenScale() {
        axes(-0.6f, 0);
        assertEquals(-2.5f, inputManager.getAxis(CURVED), 1e-6);
        axes(1, 0);
        assertEquals(10, inputManager.getAxis(CURVED), 1e-6);
    }

    @Test
    void buttonsMakeAnAxisFromTwoActions() {
        axes(0, 0.6f);
        assertEquals(0.5f, inputManager.getAxis(BUTTONS), 1e-6, "Positive : the stick value");

        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_DPAD_LEFT] = true;
        gamepads.postUpdate();
        assertEquals(-0.5f, inputManager.getAxis(BUTTONS), 1e-6, "Negative held : 0.5 - 1");
    }

    @Test
    void sumAddsTheSourcesThenScales() {
        axes(0.6f, 1);
        assertEquals((0.5f + 1) * 2, inputManager.getAxis(SUM), 1e-6);
        axes(0.6f, -0.6f);
        assertEquals(0, inputManager.getAxis(SUM), 1e-6);
    }

    @Test
    void simulatedAxisReplacesItsBindings() {
        axes(1, 0);
        inputManager.simulateAxis(STICK, 42, InputSimulation.UNTIL_RELEASE);
        assertEquals(1, inputManager.getAxis(STICK), 1e-6, "Applied at the next update");

        inputManager.updateAxisSimulation();
        assertEquals(42, inputManager.getAxis(STICK), 1e-6);

        inputManager.simulateAxisRelease(STICK);
        inputManager.updateAxisSimulation();
        assertEquals(1, inputManager.getAxis(STICK), 1e-6);
    }

    @Test
    void axesAreFoundByNameOrId() {
        assertEquals(SUM, inputManager.findAxis("SUM").axis());
        assertEquals(STICK, inputManager.findAxis("test_axis:stick").axis());
        assertNull(inputManager.findAxis("unknown"));
        // Registered but not bound in this manager
        assertNull(inputManager.findAxis("not_bound"));
        assertEquals(0, inputManager.getAxis(NOT_BOUND));
        assertThrows(IllegalArgumentException.class, () -> inputManager.simulateAxis(NOT_BOUND, 1, 1));
    }

    @Test
    void anActionCannotTakeTheIdentifierOfAnAxis() {
        assertThrows(IllegalArgumentException.class, () -> InputRegistry.action("test_axis:stick"));
    }

    @Test
    void invalidCurveIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new AxisGamepad(GLFW_GAMEPAD_AXIS_LEFT_X).curve(0));
    }
}
