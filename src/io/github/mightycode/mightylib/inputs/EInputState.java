package io.github.mightycode.mightylib.inputs;

/** Behaviour asked to {@link InputManager#isInput(int, EInputState)}. */
public enum EInputState {
    /** Held down during this tick. */
    Press,
    /** Not held during this tick. */
    Release,
    /** Went down at this tick (one tick only). */
    Pressed,
    /** Went up at this tick (one tick only). */
    Released
}
