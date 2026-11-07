package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;

public interface IAction {
    boolean getState(InputManager inputManager);

    boolean actionPressed(InputManager inputManager);

    boolean actionReleased(InputManager inputManager);

    // Strength of the action, from 0 to 1 : 1 when held for a key or a button, progressive for a gamepad axis
    default float getValue(InputManager inputManager) {
        return getState(inputManager) ? 1 : 0;
    }
}
