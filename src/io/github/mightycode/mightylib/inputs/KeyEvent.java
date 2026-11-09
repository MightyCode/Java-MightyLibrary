package io.github.mightycode.mightylib.inputs;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A key pressed, or repeated by the system while held, in the order typed: for text editing (cursor moves, deletions),
 * where the state of the keys at each tick loses fast presses and the repetition.
 */
public final class KeyEvent {
    private final int key;
    private final int mods;
    private final boolean repeat;

    public KeyEvent(int key, int mods, boolean repeat) {
        this.key = key;
        this.mods = mods;
        this.repeat = repeat;
    }

    // GLFW_KEY_...
    public int key() { return key; }

    // GLFW_MOD_... flags
    public int mods() { return mods; }

    public boolean isRepeat() { return repeat; }

    public boolean isControl() { return (mods & GLFW_MOD_CONTROL) != 0; }

    public boolean isShift() { return (mods & GLFW_MOD_SHIFT) != 0; }

    public boolean isAlt() { return (mods & GLFW_MOD_ALT) != 0; }

    @Override
    public String toString() {
        return "KeyEvent{" + key + ", mods=" + mods + (repeat ? ", repeat" : "") + "}";
    }
}
