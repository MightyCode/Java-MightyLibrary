package MightyLibrary.mightylib.physics.tweenings;

public enum ETweeningType {
    Linear,
    Quadratic,
    Cubic,
    Quartic,
    Quintic,
    Sinusoidal,
    Exponential,
    Circular,
    Elastic,
    Back,
    Bounce;

    public ETweeningType fromString(String txt){
        String lowerCase = txt.trim().toLowerCase();

        if (lowerCase.equals("linear")) return Linear;
        if (lowerCase.equals("quadratic")) return Quadratic;
        if (lowerCase.equals("cubic")) return Cubic;
        if (lowerCase.equals("quartic")) return Quartic;
        if (lowerCase.equals("quintic")) return Quintic;
        if (lowerCase.equals("sinusoidal")) return Sinusoidal;
        if (lowerCase.equals("exponential")) return Exponential;
        if (lowerCase.equals("circular")) return Circular;
        if (lowerCase.equals("elastic")) return Elastic;
        if (lowerCase.equals("back")) return Back;
        if (lowerCase.equals("bounce")) return Bounce;

        return null;
    }
}
