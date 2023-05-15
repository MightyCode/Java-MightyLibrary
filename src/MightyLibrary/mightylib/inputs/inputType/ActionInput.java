package MightyLibrary.mightylib.inputs.inputType;

public class ActionInput {
    private final int actionId;
    private final String actionName;
    private final IInput input;

    public ActionInput(int actionId, String actionName, IInput input){
        this.actionId = actionId;
        this.actionName = actionName;
        this.input = input;
    }

    public int actionId() { return actionId; }
    public IInput actionInput() { return input; }

    public String actionName() { return actionName; }
}
