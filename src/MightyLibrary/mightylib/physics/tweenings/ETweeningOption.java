package MightyLibrary.mightylib.physics.tweenings;

public enum ETweeningOption {
    Direct,
    DirectMirrored,
    DirectReversed,
    Loop,
    LoopMirrored,
    LoopReversed;

    public ETweeningOption fromString(String txt){
        String lowerCase = txt.trim().toLowerCase();

        if (lowerCase.equals("direct")) return Direct;
        if (lowerCase.equals("directmirrored")) return DirectMirrored;
        if (lowerCase.equals("directreversed")) return DirectReversed;
        if (lowerCase.equals("loop")) return DirectReversed;
        if (lowerCase.equals("loopmirrored")) return DirectReversed;
        if (lowerCase.equals("loopreversed")) return DirectReversed;

        return null;
    }
}
