package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputManager;

// Binding of a registered action in an InputManager
public class ActionInput implements IAction {
    private final InputAction action;
    private final IAction input;

    public ActionInput(InputAction action, IAction input){
        this.action = action;
        this.input = input;
    }

    public InputAction action() { return action; }
    public IAction actionInput() { return input; }

    @Override
    public boolean getState(InputManager inputManager) {
        return actionInput().getState(inputManager);
    }

    @Override
    public float getValue(InputManager inputManager) {
        return actionInput().getValue(inputManager);
    }

    @Override
    public boolean actionPressed(InputManager inputManager) {
        return actionInput().actionPressed(inputManager);
    }

    @Override
    public boolean actionReleased(InputManager inputManager) {
        return actionInput().actionReleased(inputManager);
    }
}
