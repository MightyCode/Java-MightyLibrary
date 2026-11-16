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

        switch (lowerCase) {
            case "direct": return Direct;
            case "directmirrored": return DirectMirrored;
            case "directreversed": return DirectReversed;
            case "loop": return Loop;
            case "loopmirrored": return LoopMirrored;
            case "loopreversed": return LoopReversed;
            default: return null;
        }
    }
}
