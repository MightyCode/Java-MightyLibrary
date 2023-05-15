package MightyLibrary.mightylib.physics.tweenings;

public enum ETweeningBehaviour {
    In,
    Out,
    InOut;

    public ETweeningBehaviour fromString(String txt){
        String lowerCase = txt.trim().toLowerCase();

        if (lowerCase.equals("in")) return In;
        if (lowerCase.equals("out")) return In;
        if (lowerCase.equals("inout")) return In;

        return null;
    }
}
