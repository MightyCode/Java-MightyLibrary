package MightyLibrary.mightylib.inputs.inputType;

import MightyLibrary.mightylib.inputs.InputManager;

public interface IAction {
    boolean getState(InputManager inputManager);

    boolean actionPressed(InputManager inputManager);

    boolean actionReleased(InputManager inputManager);
}
