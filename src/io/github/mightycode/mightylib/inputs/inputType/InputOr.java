package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.IInputManager;

public final class InputOr implements IAction {
    private final IAction input1;
    private final IAction input2;

    public InputOr(IAction input1, IAction input2){
        this.input1 = input1;
        this.input2 = input2;
    }

    @Override
    public boolean getState(IInputManager inputManager) {
        return input1.getState(inputManager) || input2.getState(inputManager);
    }

    // The strongest of the two
    @Override
    public float getValue(IInputManager inputManager) {
        return Math.max(input1.getValue(inputManager), input2.getValue(inputManager));
    }

    @Override
    public boolean actionPressed(IInputManager inputManager) {
        return (!input1.getState(inputManager) && input2.actionPressed(inputManager))
                || (!input2.getState(inputManager) && input1.actionPressed(inputManager));
    }

    @Override
    public boolean actionReleased(IInputManager inputManager) {
        return (!input1.getState(inputManager) && input2.actionReleased(inputManager))
                || (!input2.getState(inputManager) && input1.actionReleased(inputManager));
    }
}
