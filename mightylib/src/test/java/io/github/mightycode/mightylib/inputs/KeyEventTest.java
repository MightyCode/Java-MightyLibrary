package io.github.mightycode.mightylib.inputs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class KeyEventTest {
    @Test
    void letterOfAKeyName() {
        assertEquals('a', KeyEvent.LetterOf("a"));
        assertEquals('a', KeyEvent.LetterOf("A"));
        assertEquals('é', KeyEvent.LetterOf("é"));
        // No name (arrows, F1...) or not a single character: no letter
        assertEquals(0, KeyEvent.LetterOf(null));
        assertEquals(0, KeyEvent.LetterOf(""));
        assertEquals(0, KeyEvent.LetterOf("ab"));
    }

    @Test
    void theLetterIsAskedOnlyWithControl() {
        // Wayland reports an error for keys without a character (dead keys): typed text never asks for a name
        assertFalse(KeyEvent.NeedsLetter(0));
        assertFalse(KeyEvent.NeedsLetter(GLFW_MOD_SHIFT | GLFW_MOD_ALT));
        assertTrue(KeyEvent.NeedsLetter(GLFW_MOD_CONTROL));
        assertTrue(KeyEvent.NeedsLetter(GLFW_MOD_CONTROL | GLFW_MOD_SHIFT));
    }

    @Test
    void theLetterIsLowerCaseAndIndependentOfThePosition() {
        KeyEvent event = new KeyEvent(GLFW_KEY_Q, 16, GLFW_MOD_CONTROL | GLFW_MOD_SHIFT, false, 'A');
        assertEquals('a', event.letter());
        assertEquals(GLFW_KEY_Q, event.key());
        assertTrue(event.isControl());
        assertTrue(event.isShift());
        assertFalse(event.isAlt());
    }
}
