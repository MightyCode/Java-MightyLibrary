package io.github.mightycode.mightylib.physics.tweenings;

import java.util.Locale;

public enum ETweeningBehaviour {
    In,
    Out,
    InOut;

    public ETweeningBehaviour fromString(String txt){
        String lowerCase = txt.trim().toLowerCase(Locale.ROOT);

        if (lowerCase.equals("in")) return In;
        if (lowerCase.equals("out")) return Out;
        if (lowerCase.equals("inout")) return InOut;

        return null;
    }
}
