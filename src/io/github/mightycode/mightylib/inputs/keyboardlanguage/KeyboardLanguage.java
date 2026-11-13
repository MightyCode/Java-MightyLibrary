package io.github.mightycode.mightylib.inputs.keyboardlanguage;

import io.github.mightycode.mightylib.inputs.CharInputEntry;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;

public abstract class KeyboardLanguage {
    protected KeyboardManager manager;

    protected final HashSet<CharInputEntry> inputEntries;

    protected KeyboardLanguage(){
        inputEntries = new HashSet<>();
    }

    public abstract String keyboardConfigurationName();

    final protected boolean shouldCapsLock(){
        return manager.isCapsLock() || manager.getRawKeyState(GLFW.GLFW_KEY_LEFT_SHIFT) || manager.getRawKeyState(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    final protected boolean shouldAltGr(){
        return manager.getRawKeyState(GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public char translateKeyTo(int keyId) {
        for (CharInputEntry charInputEntry : inputEntries){
            if (!charInputEntry.isKeyId(keyId))
                continue;

            if (shouldCapsLock())
                return charInputEntry.getModification(CharInputEntry.CAPSLOCK);
            else if (shouldAltGr())
                return charInputEntry.getModification(CharInputEntry.ALT);

            return charInputEntry.getChar();
        }

        return (char)-1;
    }

    public void setManager(KeyboardManager m) {
        manager = m;
    }
}
