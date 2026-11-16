package io.github.mightycode.mightylib.physics.tweenings;

import java.util.Locale;

public enum ETweeningBehaviour {
    In,
    Out,
    InOut;

    public ETweeningBehaviour fromString(String txt){
        String lowerCase = txt.trim().toLowerCase(Locale.ROOT);

        switch (lowerCase) {
            case "in": return In;
            case "out": return Out;
            case "inout": return InOut;
            default: return null;
        }
    }
}
