package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.GamepadManager;

/**
 * A gamepad button (GLFW_GAMEPAD_BUTTON_...) of a player, or of any gamepad (GamepadManager.ANY_PLAYER).
 */
public final class InputGamepadButton implements IAction {
    private final int button;
    private final int player;

    public InputGamepadButton(int button) {
        this(button, GamepadManager.ANY_PLAYER);
    }

    public InputGamepadButton(int button, int player) {
        this.button = button;
        this.player = player;
    }

    public int button() { return button; }
    public int player() { return player; }

    @Override
    public boolean getState(IInputManager inputManager) {
        return inputManager.getGamepadManager().getButton(button, player);
    }

    @Override
    public boolean actionPressed(IInputManager inputManager) {
        return inputManager.getGamepadManager().buttonPressed(button, player);
    }

    @Override
    public boolean actionReleased(IInputManager inputManager) {
        return inputManager.getGamepadManager().buttonReleased(button, player);
    }
}
