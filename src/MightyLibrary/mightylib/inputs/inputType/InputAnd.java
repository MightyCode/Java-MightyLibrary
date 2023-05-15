package MightyLibrary.mightylib.inputs.inputType;

import MightyLibrary.mightylib.inputs.InputManager;

public class InputAnd implements IInput{
    private final IInput input1;
    private final IInput input2;

    public InputAnd(IInput input1, IInput input2){
        this.input1 = input1;
        this.input2 = input2;
    }

    @Override
    public boolean getState(InputManager inputManager) {
        return input1.getState(inputManager) && input2.getState(inputManager);
    }

    @Override
    public boolean inputPressed(InputManager inputManager) {
        return (input1.getState(inputManager) && input2.inputPressed(inputManager))
                || (input2.getState(inputManager) && input1.inputPressed(inputManager));
    }

    @Override
    public boolean inputReleased(InputManager inputManager) {
        return (input1.getState(inputManager) && input2.inputReleased(inputManager))
                || (input2.getState(inputManager) && input1.inputReleased(inputManager));
    }
}
