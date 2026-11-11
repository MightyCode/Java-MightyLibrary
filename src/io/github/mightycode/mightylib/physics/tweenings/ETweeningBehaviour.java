package io.github.mightycode.mightylib.physics.tweenings;

public enum ETweeningBehaviour {
    In,
    Out,
    InOut;

    public ETweeningBehaviour fromString(String txt){
        String lowerCase = txt.trim().toLowerCase();

        if (lowerCase.equals("in")) return In;
        if (lowerCase.equals("out")) return Out;
        if (lowerCase.equals("inout")) return InOut;

        return null;
    }
}
