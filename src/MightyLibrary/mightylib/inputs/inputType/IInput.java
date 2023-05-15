package MightyLibrary.mightylib.inputs.inputType;

import MightyLibrary.mightylib.inputs.InputManager;

public interface IInput {
    boolean getState(InputManager inputManager);

    boolean inputPressed(InputManager inputManager);

    boolean inputReleased(InputManager inputManager);
}
