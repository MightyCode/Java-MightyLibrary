package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;

public class InputAnd implements IAction {
    private final IAction input1;
    private final IAction input2;

    public InputAnd(IAction input1, IAction input2){
        this.input1 = input1;
        this.input2 = input2;
    }

    @Override
    public boolean getState(InputManager inputManager) {
        return input1.getState(inputManager) && input2.getState(inputManager);
    }

    // The weakest of the two : 0 if one of them is not used
    @Override
    public float getValue(InputManager inputManager) {
        return Math.min(input1.getValue(inputManager), input2.getValue(inputManager));
    }

    @Override
    public boolean actionPressed(InputManager inputManager) {
        return (input1.getState(inputManager) && input2.actionPressed(inputManager))
                || (input2.getState(inputManager) && input1.actionPressed(inputManager));
    }

    @Override
    public boolean actionReleased(InputManager inputManager) {
        return (input1.getState(inputManager) && input2.actionReleased(inputManager))
                || (input2.getState(inputManager) && input1.actionReleased(inputManager));
    }
}
