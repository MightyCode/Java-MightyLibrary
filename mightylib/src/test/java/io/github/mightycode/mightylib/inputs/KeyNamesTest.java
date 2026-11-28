package io.github.mightycode.mightylib.inputs;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The fixed names of the keys, without GL: every GLFW key of the keyboard has one, a key out of bounds is refused
class KeyNamesTest {
    // Every GLFW_KEY_... constant the keyboard manager reads (the aliases LAST and UNKNOWN left out)
    private static Map<String, Integer> glfwKeys() throws IllegalAccessException {
        Map<String, Integer> keys = new HashMap<>();
        for (Field field : GLFW.class.getFields()) {
            if (!field.getName().startsWith("GLFW_KEY_") || field.getType() != int.class
                    || !Modifier.isStatic(field.getModifiers()))
                continue;
            int key = field.getInt(null);
            if (key >= KeyboardManager.BEGIN_KEYS && key < KeyboardManager.KEYS)
                keys.put(field.getName(), key);
        }
        return keys;
    }

    @Test
    void everyKeyOfGlfwHasAName() throws IllegalAccessException {
        Map<String, Integer> keys = glfwKeys();
        assertTrue(keys.size() > 100, keys.toString());

        List<String> unnamed = new ArrayList<>();
        for (Map.Entry<String, Integer> key : keys.entrySet()) {
            String name = KeyNames.Of(key.getValue());
            if (name.isEmpty() || name.startsWith("Key "))
                unnamed.add(key.getKey());
        }
        assertTrue(unnamed.isEmpty(), "Keys without a name: " + unnamed);
    }

    @Test
    void namesOfTheKeysTheLayoutDoesNotName() {
        assertEquals("Escape", KeyNames.Of(GLFW.GLFW_KEY_ESCAPE));
        assertEquals("Enter", KeyNames.Of(GLFW.GLFW_KEY_ENTER));
        assertEquals("F1", KeyNames.Of(GLFW.GLFW_KEY_F1));
        assertEquals("F25", KeyNames.Of(GLFW.GLFW_KEY_F25));
        assertEquals("Left", KeyNames.Of(GLFW.GLFW_KEY_LEFT));
        assertEquals("Left Shift", KeyNames.Of(GLFW.GLFW_KEY_LEFT_SHIFT));
        assertEquals("Right Super", KeyNames.Of(GLFW.GLFW_KEY_RIGHT_SUPER));
        assertEquals("Keypad 1", KeyNames.Of(GLFW.GLFW_KEY_KP_1));
        assertEquals("Keypad Enter", KeyNames.Of(GLFW.GLFW_KEY_KP_ENTER));
        assertEquals("Space", KeyNames.Of(GLFW.GLFW_KEY_SPACE));
        assertEquals("W", KeyNames.Of(GLFW.GLFW_KEY_W));
    }

    @Test
    void theKeypadKeepsItsFixedNames() {
        assertTrue(KeyNames.IsKeypad(GLFW.GLFW_KEY_KP_0));
        assertTrue(KeyNames.IsKeypad(GLFW.GLFW_KEY_KP_EQUAL));
        assertFalse(KeyNames.IsKeypad(GLFW.GLFW_KEY_1));
        assertFalse(KeyNames.IsKeypad(GLFW.GLFW_KEY_LEFT_SHIFT));
    }

    @Test
    void aCodeNoKeyHasIsNamedByItsCode() {
        assertEquals("Key 33", KeyNames.Of(33));
    }

    @Test
    void aKeyOutOfBoundsIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> KeyNames.Of(KeyboardManager.BEGIN_KEYS - 1));
        assertThrows(IllegalArgumentException.class, () -> KeyNames.Of(KeyboardManager.KEYS));
        assertThrows(IllegalArgumentException.class, () -> KeyNames.Of(GLFW.GLFW_KEY_UNKNOWN));
    }
}
