package io.github.mightycode.mightylib.algorithms;

/**
 * How much an algorithm writes on the standard output. Algorithms are silent by default (NONE).
 */
public enum EVerbosity {
    /** Nothing is printed. */
    NONE,
    /** One summary line per run. */
    ESSENTIAL,
    /** Every step, for debugging. */
    ALL;

    public boolean allows(EVerbosity required) {
        return ordinal() >= required.ordinal();
    }

    /** Prints the message (with the engine prefix) when this level is at least the required one. */
    public void print(EVerbosity required, String message) {
        if (allows(required) && required != NONE)
            System.out.println("-- " + message);
    }
}
