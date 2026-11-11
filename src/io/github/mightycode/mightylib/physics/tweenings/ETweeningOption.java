package io.github.mightycode.mightylib.physics.tweenings;

import java.util.Locale;

public enum ETweeningOption {
    Direct,
    DirectMirrored,
    DirectReversed,
    Loop,
    LoopMirrored,
    LoopReversed;

    public ETweeningOption fromString(String txt){
        String lowerCase = txt.trim().toLowerCase(Locale.ROOT);

        if (lowerCase.equals("direct")) return Direct;
        if (lowerCase.equals("directmirrored")) return DirectMirrored;
        if (lowerCase.equals("directreversed")) return DirectReversed;
        if (lowerCase.equals("loop")) return Loop;
        if (lowerCase.equals("loopmirrored")) return LoopMirrored;
        if (lowerCase.equals("loopreversed")) return LoopReversed;

        return null;
    }
}
