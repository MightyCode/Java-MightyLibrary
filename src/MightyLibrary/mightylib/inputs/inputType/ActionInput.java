package MightyLibrary.mightylib.inputs.inputType;

import MightyLibrary.mightylib.inputs.InputManager;

public class ActionInput implements IAction {
    private final int actionId;
    private final String actionName;
    private final IAction input;

    public ActionInput(int actionId, String actionName, IAction input){
        this.actionId = actionId;
        this.actionName = actionName;
        this.input = input;
    }

    public int actionId() { return actionId; }
    public IAction actionInput() { return input; }

    public String actionName() { return actionName; }

    @Override
    public boolean getState(InputManager inputManager) {
        return actionInput().getState(inputManager);
    }

    @Override
    public boolean actionPressed(InputManager inputManager) {
        return actionInput().getState(inputManager);
    }

    @Override
    public boolean actionReleased(InputManager inputManager) {
        return actionInput().getState(inputManager);
    }
}
