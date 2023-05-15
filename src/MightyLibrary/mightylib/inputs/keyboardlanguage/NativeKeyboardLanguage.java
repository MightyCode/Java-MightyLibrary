package MightyLibrary.mightylib.inputs.keyboardlanguage;

import MightyLibrary.mightylib.inputs.KeyboardLanguage;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;

public class NativeKeyboardLanguage extends KeyboardLanguage {
    private static final KeyboardLanguage instance = new NativeKeyboardLanguage();

    private HashMap<Character, Character> capslock;

    public static KeyboardLanguage getInstance() { return instance; }
    private NativeKeyboardLanguage(){
        capslock = new HashMap<>();
        capslock.put('1', '!');
        capslock.put('2', '@');
        capslock.put('3', '#');
        capslock.put('4', '$');
        capslock.put('5', '%');
        capslock.put('6', '^');
        capslock.put('7', '&');
        capslock.put('8', '*');
        capslock.put('9', '(');
        capslock.put('0', ')');
        capslock.put('-', '_');
        capslock.put('=', '+');
        capslock.put('[', '{');
        capslock.put(']', '}');
        capslock.put('\'', '\"');
        capslock.put('\\', '|');
        capslock.put('m', '/');
        capslock.put(',', '<');
        capslock.put('.', '>');
        capslock.put('/', '?');
        capslock.put('`', '~');
    }

    @Override
    public char translateKeyTo(int keyId) {
        if (keyId >= GLFW.GLFW_KEY_A && keyId <= GLFW.GLFW_KEY_Z)
            return (char) ('a' + (keyId - GLFW.GLFW_KEY_A));

        if (keyId >= GLFW.GLFW_KEY_0 && keyId <= GLFW.GLFW_KEY_9)
            return (char)('0' + (keyId - GLFW.GLFW_KEY_0));

        if (keyId >= GLFW.GLFW_KEY_KP_0 && keyId <= GLFW.GLFW_KEY_KP_9)
            return (char)('0' + (keyId - GLFW.GLFW_KEY_KP_0));

        if (keyId == GLFW.GLFW_KEY_SPACE)
            return ' ';

        return (char)-1;
    }

    @Override
    public String keyboardConfigurationName() {
        return "qwerty";
    }
}
