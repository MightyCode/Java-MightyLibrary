package MightyLibrary.mightylib.inputs;

public class InputConfiguration {
    private final int inputId;
    private final EInputType inputType;

    public InputConfiguration(){
        inputId = -1;
        inputType = EInputType.None;
    }

    public InputConfiguration(int inputId, EInputType type){
        this.inputId = inputId;
        this.inputType = type;
    }

    public int id() { return inputId; }
    public EInputType type() { return inputType; }
}
