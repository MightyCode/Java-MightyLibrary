package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.GamepadManager;

/**
 * A gamepad axis (GLFW_GAMEPAD_AXIS_...) used as a button : held while it is pushed beyond a threshold in a
 * direction (stick up : LEFT_Y with NEGATIVE, y is positive downward ; trigger : POSITIVE).
 * The analog value is read with GamepadManager.getAxis or Gamepad.getAxis.
 */
public class InputGamepadAxis implements IAction {
    public static final int POSITIVE = 1;
    public static final int NEGATIVE = -1;
    public static final float DEFAULT_THRESHOLD = 0.5f;

    private final int axis;
    private final int direction;
    private final float threshold;
    private final int player;

    public InputGamepadAxis(int axis, int direction) {
        this(axis, direction, DEFAULT_THRESHOLD, GamepadManager.ANY_PLAYER);
    }

    public InputGamepadAxis(int axis, int direction, float threshold, int player) {
        if (direction != POSITIVE && direction != NEGATIVE)
            throw new IllegalArgumentException("Direction must be POSITIVE or NEGATIVE : " + direction);
        if (threshold <= 0 || threshold > 1)
            throw new IllegalArgumentException("Threshold must be in ]0, 1] : " + threshold);

        this.axis = axis;
        this.direction = direction;
        this.threshold = threshold;
        this.player = player;
    }

    public int axis() { return axis; }
    public int direction() { return direction; }
    public float threshold() { return threshold; }
    public int player() { return player; }

    private boolean beyond(float value) {
        return value * direction >= threshold;
    }

    // For ANY_PLAYER, the axis farthest from 0 among the gamepads decides
    @Override
    public boolean getState(InputManager inputManager) {
        return beyond(inputManager.getGamepadManager().getAxis(axis, player));
    }

    // How far the axis is pushed in the direction, from 0 to 1, without threshold (the dead zone already applies)
    @Override
    public float getValue(InputManager inputManager) {
        return Math.max(0, inputManager.getGamepadManager().getAxis(axis, player) * direction);
    }

    @Override
    public boolean actionPressed(InputManager inputManager) {
        GamepadManager gamepads = inputManager.getGamepadManager();
        return beyond(gamepads.getAxis(axis, player)) && !beyond(gamepads.getOldAxis(axis, player));
    }

    @Override
    public boolean actionReleased(InputManager inputManager) {
        GamepadManager gamepads = inputManager.getGamepadManager();
        return !beyond(gamepads.getAxis(axis, player)) && beyond(gamepads.getOldAxis(axis, player));
    }
}
