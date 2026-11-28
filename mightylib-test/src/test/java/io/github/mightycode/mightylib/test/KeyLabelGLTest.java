package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.inputs.KeyboardManager;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The labels of the keys in the layout of the machine: fixed names for the keys it does not name, one character for
// a letter whatever the layout
@MightySceneTest(config = SceneRunnerTest.Config.class)
class KeyLabelGLTest {
    @Test
    void aKeyTheLayoutDoesNotNameHasItsFixedName(SceneRunner runner) {
        runner.frames(1);
        KeyboardManager keyboard = runner.inputs().getKeyboardManager();

        assertEquals("Escape", keyboard.keyLabel(GLFW.GLFW_KEY_ESCAPE));
        assertEquals("Enter", keyboard.keyLabel(GLFW.GLFW_KEY_ENTER));
        assertEquals("Space", keyboard.keyLabel(GLFW.GLFW_KEY_SPACE));
        assertEquals("F5", keyboard.keyLabel(GLFW.GLFW_KEY_F5));
        assertEquals("Left Shift", keyboard.keyLabel(GLFW.GLFW_KEY_LEFT_SHIFT));
        assertEquals("Keypad 1", keyboard.keyLabel(GLFW.GLFW_KEY_KP_1));
        // The last key of GLFW is a key of the keyboard
        assertEquals("Menu", keyboard.keyLabel(GLFW.GLFW_KEY_MENU));
    }

    @Test
    void aLetterHasALabelOfOneCharacterInUpperCase(SceneRunner runner) {
        runner.frames(1);
        KeyboardManager keyboard = runner.inputs().getKeyboardManager();

        for (int key = GLFW.GLFW_KEY_A; key <= GLFW.GLFW_KEY_Z; ++key) {
            String label = keyboard.keyLabel(key);
            assertEquals(1, label.codePointCount(0, label.length()), "label of " + key + ": " + label);
            assertEquals(label.toUpperCase(Locale.ROOT), label, "label of " + key);
        }
    }

    @Test
    void aKeyOutOfBoundsIsRefused(SceneRunner runner) {
        KeyboardManager keyboard = runner.inputs().getKeyboardManager();
        assertThrows(IllegalArgumentException.class, () -> keyboard.keyLabel(GLFW.GLFW_KEY_UNKNOWN));
        assertThrows(IllegalArgumentException.class, () -> keyboard.keyLabel(GLFW.GLFW_KEY_LAST + 1));
    }
}
