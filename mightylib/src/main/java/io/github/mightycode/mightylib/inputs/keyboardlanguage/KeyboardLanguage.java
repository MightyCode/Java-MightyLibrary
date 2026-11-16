package io.github.mightycode.mightylib.inputs.keyboardlanguage;

import io.github.mightycode.mightylib.inputs.CharInputEntry;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;

/**
 * The characters of the keys for one keyboard layout. A subclass gives its name (hook keyboardConfigurationName)
 * and adds its keys with addInputEntry in its constructor.
 */
public abstract class KeyboardLanguage {
    private KeyboardManager manager;

    private final HashSet<CharInputEntry> inputEntries;

    protected KeyboardLanguage(){
        inputEntries = new HashSet<>();
    }

    // Hook: the name of the layout ("AZERTY"...)
    public abstract String keyboardConfigurationName();

    // A key and its characters (with caps lock, with AltGr)
    protected final void addInputEntry(CharInputEntry entry) {
        inputEntries.add(entry);
    }

    final protected boolean shouldCapsLock(){
        return manager.isCapsLock() || manager.getRawKeyState(GLFW.GLFW_KEY_LEFT_SHIFT) || manager.getRawKeyState(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    final protected boolean shouldAltGr(){
        return manager.getRawKeyState(GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public final char translateKeyTo(int keyId) {
        for (CharInputEntry charInputEntry : inputEntries){
            if (!charInputEntry.isKeyId(keyId))
                continue;

            if (shouldCapsLock())
                return charInputEntry.getModification(CharInputEntry.CAPSLOCK);
            else if (shouldAltGr())
                return charInputEntry.getModification(CharInputEntry.ALT);

            return charInputEntry.getChar();
        }

        return Character.MAX_VALUE;
    }

    public final void setManager(KeyboardManager m) {
        manager = m;
    }
}
