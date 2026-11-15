package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.IInputManager;

public interface IAction {
    boolean getState(IInputManager inputManager);

    boolean actionPressed(IInputManager inputManager);

    boolean actionReleased(IInputManager inputManager);

    // Strength of the action, from 0 to 1 : 1 when held for a key or a button, progressive for a gamepad axis
    default float getValue(IInputManager inputManager) {
        return getState(inputManager) ? 1 : 0;
    }
}
