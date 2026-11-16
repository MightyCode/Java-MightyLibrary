package io.github.mightycode.mightylib.inputs;

/** Behavior asked to {@link IInputManager#isInput(InputAction, EInputState)}. */
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
