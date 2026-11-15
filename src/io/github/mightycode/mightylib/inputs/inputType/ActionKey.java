package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.IInputManager;

import java.util.Objects;

// An action read by its key in the IInputManager that reads the binding: its binding in that window
final class ActionKey implements IAction {
    private final InputAction action;

    ActionKey(InputAction action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    @Override
    public boolean getState(IInputManager inputManager) {
        return inputManager.getState(action);
    }

    @Override
    public float getValue(IInputManager inputManager) {
        return inputManager.getValue(action);
    }

    @Override
    public boolean actionPressed(IInputManager inputManager) {
        return inputManager.inputPressed(action);
    }

    @Override
    public boolean actionReleased(IInputManager inputManager) {
        return inputManager.inputReleased(action);
    }

    @Override
    public String toString() {
        return action.toString();
    }
}
