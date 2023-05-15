package MightyLibrary.mightylib.inputs;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

public abstract class KeyboardLanguage {
    protected KeyboardManager manager;
    public abstract char translateKeyTo(int keyId);

    public abstract String keyboardConfigurationName();

    final protected boolean shouldCapsLock(){
        return manager.isCapsLock() || manager.getKeyState(GLFW.GLFW_KEY_LEFT_SHIFT) || manager.getKeyState(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    final protected boolean shouldAltGr(){
        return manager.getKeyState(GLFW.GLFW_KEY_RIGHT_ALT);
    }

    void setManager(KeyboardManager m) {
        manager = m;
    }
}
