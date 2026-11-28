package io.github.mightycode.mightylib.inputs;

import static org.lwjgl.glfw.GLFW.*;

// Fixed English names of the keys (GLFW_KEY_...): the label of a key the layout of the system does not name
final class KeyNames {
    private static final String[] NAMES = new String[KeyboardManager.KEYS];

    static {
        put(GLFW_KEY_SPACE, "Space");
        put(GLFW_KEY_APOSTROPHE, "'");
        put(GLFW_KEY_COMMA, ",");
        put(GLFW_KEY_MINUS, "-");
        put(GLFW_KEY_PERIOD, ".");
        put(GLFW_KEY_SLASH, "/");
        for (int digit = 0; digit <= 9; ++digit)
            put(GLFW_KEY_0 + digit, String.valueOf(digit));
        put(GLFW_KEY_SEMICOLON, ";");
        put(GLFW_KEY_EQUAL, "=");
        for (char letter = 'A'; letter <= 'Z'; ++letter)
            put(GLFW_KEY_A + (letter - 'A'), String.valueOf(letter));
        put(GLFW_KEY_LEFT_BRACKET, "[");
        put(GLFW_KEY_BACKSLASH, "\\");
        put(GLFW_KEY_RIGHT_BRACKET, "]");
        put(GLFW_KEY_GRAVE_ACCENT, "`");
        put(GLFW_KEY_WORLD_1, "World 1");
        put(GLFW_KEY_WORLD_2, "World 2");

        put(GLFW_KEY_ESCAPE, "Escape");
        put(GLFW_KEY_ENTER, "Enter");
        put(GLFW_KEY_TAB, "Tab");
        put(GLFW_KEY_BACKSPACE, "Backspace");
        put(GLFW_KEY_INSERT, "Insert");
        put(GLFW_KEY_DELETE, "Delete");
        put(GLFW_KEY_RIGHT, "Right");
        put(GLFW_KEY_LEFT, "Left");
        put(GLFW_KEY_DOWN, "Down");
        put(GLFW_KEY_UP, "Up");
        put(GLFW_KEY_PAGE_UP, "Page Up");
        put(GLFW_KEY_PAGE_DOWN, "Page Down");
        put(GLFW_KEY_HOME, "Home");
        put(GLFW_KEY_END, "End");
        put(GLFW_KEY_CAPS_LOCK, "Caps Lock");
        put(GLFW_KEY_SCROLL_LOCK, "Scroll Lock");
        put(GLFW_KEY_NUM_LOCK, "Num Lock");
        put(GLFW_KEY_PRINT_SCREEN, "Print Screen");
        put(GLFW_KEY_PAUSE, "Pause");
        for (int f = 1; f <= 25; ++f)
            put(GLFW_KEY_F1 + f - 1, "F" + f);

        for (int digit = 0; digit <= 9; ++digit)
            put(GLFW_KEY_KP_0 + digit, "Keypad " + digit);
        put(GLFW_KEY_KP_DECIMAL, "Keypad .");
        put(GLFW_KEY_KP_DIVIDE, "Keypad /");
        put(GLFW_KEY_KP_MULTIPLY, "Keypad *");
        put(GLFW_KEY_KP_SUBTRACT, "Keypad -");
        put(GLFW_KEY_KP_ADD, "Keypad +");
        put(GLFW_KEY_KP_ENTER, "Keypad Enter");
        put(GLFW_KEY_KP_EQUAL, "Keypad =");

        put(GLFW_KEY_LEFT_SHIFT, "Left Shift");
        put(GLFW_KEY_LEFT_CONTROL, "Left Control");
        put(GLFW_KEY_LEFT_ALT, "Left Alt");
        put(GLFW_KEY_LEFT_SUPER, "Left Super");
        put(GLFW_KEY_RIGHT_SHIFT, "Right Shift");
        put(GLFW_KEY_RIGHT_CONTROL, "Right Control");
        put(GLFW_KEY_RIGHT_ALT, "Right Alt");
        put(GLFW_KEY_RIGHT_SUPER, "Right Super");
    }

    private KeyNames() {}

    private static void put(int key, String name) {
        NAMES[key] = name;
    }

    // The fixed name of a key between BEGIN_KEYS and KEYS - 1; "Key <code>" for a code no GLFW key has
    static String Of(int key) {
        KeyboardManager.CheckKey(key);
        String name = NAMES[key];
        return name != null ? name : "Key " + key;
    }

    // The keypad keeps its fixed names: the layout names its keys like the digits of the main block
    static boolean IsKeypad(int key) {
        return key >= GLFW_KEY_KP_0 && key <= GLFW_KEY_KP_EQUAL;
    }
}
