package io.github.mightycode.mightylib.inputs;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A key pressed, or repeated by the system while held, in the order typed: for text editing (cursor moves, deletions),
 * where the state of the keys at each tick loses fast presses and the repetition.
 * The key code is the physical position (GLFW_KEY_Q is the key at the left of W, 'a' on an AZERTY keyboard); the
 * letter is the one the layout of the system gives to it: shortcuts such as Ctrl+A compare letters.
 */
public final class KeyEvent {
    private final int key;
    private final int scancode;
    private final int mods;
    private final boolean repeat;
    private final char letter;
    private final int textBefore;

    /** A key that comes after all the text typed during its tick (simulated keys). */
    public static final int AFTER_TEXT = Integer.MAX_VALUE;

    /**
     * @param letter the character the layout of the system gives to the key, lower case (glfwGetKeyName),
     *               0 when the key has no name (arrows, Home, F1...)
     */
    public KeyEvent(int key, int scancode, int mods, boolean repeat, char letter) {
        this(key, scancode, mods, repeat, letter, AFTER_TEXT);
    }

    /**
     * @param textBefore the characters of the text typed during the tick that came before this key (the typed text
     *                   and the keys are two lists: this puts them back in order), AFTER_TEXT when after all of it
     */
    public KeyEvent(int key, int scancode, int mods, boolean repeat, char letter, int textBefore) {
        this.key = key;
        this.scancode = scancode;
        this.mods = mods;
        this.repeat = repeat;
        this.letter = Character.toLowerCase(letter);
        this.textBefore = Math.max(0, textBefore);
    }

    /**
     * The characters of the text typed in the tick before this key, at most the length of that text: an editor
     * inserts them, then applies the key (typing "a", Backspace, "b" in one tick gives "b").
     */
    public int textBefore(int typedLength) {
        return Math.min(textBefore, typedLength);
    }

    // GLFW_KEY_... : the physical position, named after the US layout
    public int key() { return key; }

    // Specific to the platform, 0 for a simulated key
    public int scancode() { return scancode; }

    // GLFW_MOD_... flags
    public int mods() { return mods; }

    public boolean isRepeat() { return repeat; }

    // Lower case letter of the key in the layout of the system, 0 if the key has none
    public char letter() { return letter; }

    public boolean isControl() { return (mods & GLFW_MOD_CONTROL) != 0; }

    public boolean isShift() { return (mods & GLFW_MOD_SHIFT) != 0; }

    public boolean isAlt() { return (mods & GLFW_MOD_ALT) != 0; }

    // The letter of a key name given by glfwGetKeyName (null: none)
    static char LetterOf(String keyName) {
        if (keyName == null || keyName.length() != 1)
            return 0;

        return Character.toLowerCase(keyName.charAt(0));
    }

    @Override
    public String toString() {
        return "KeyEvent{" + key + (letter != 0 ? " '" + letter + "'" : "") + ", mods=" + mods
                + (repeat ? ", repeat" : "") + "}";
    }
}
