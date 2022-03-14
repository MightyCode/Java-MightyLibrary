package MightyLibrary.mightylib.inputs;

public class ActionConfigurations {
    private final int actionId;
    private final InputConfiguration[] configurations;

    public ActionConfigurations(int actionId, InputConfiguration[] configurations){
        this.configurations = configurations;
        this.actionId = actionId;
    }

    public int actionId() { return actionId; }
    public InputConfiguration[] configurations() { return configurations; }
}
