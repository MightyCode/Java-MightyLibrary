package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.GamepadManager;
import io.github.mightycode.mightylib.inputs.InputManager;

/**
 * A gamepad axis (GLFW_GAMEPAD_AXIS_...) : sticks from -1 to 1 (y positive downward), triggers from 0 to 1,
 * dead zone applied. Of a player, or of any gamepad (the value farthest from 0).
 */
public class AxisGamepad extends AxisBinding {
    private final int axis;
    private final int player;

    public AxisGamepad(int axis) {
        this(axis, GamepadManager.ANY_PLAYER);
    }

    public AxisGamepad(int axis, int player) {
        this.axis = axis;
        this.player = player;
    }

    public int axis() { return axis; }
    public int player() { return player; }

    @Override
    protected float raw(InputManager inputManager) {
        return inputManager.getGamepadManager().getAxis(axis, player);
    }
}
