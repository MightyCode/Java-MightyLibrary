package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;

public interface IAction {
    boolean getState(InputManager inputManager);

    boolean actionPressed(InputManager inputManager);

    boolean actionReleased(InputManager inputManager);
}
