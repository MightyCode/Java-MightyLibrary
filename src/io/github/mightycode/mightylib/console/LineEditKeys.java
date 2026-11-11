package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.inputs.KeyEvent;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The keys that edit one line of text, shared by the command line of the console and the text fields of the GUI:
 * the cursor, the selection (Shift extends it) and the deletions. No GL, no window.
 */
public final class LineEditKeys {
    private LineEditKeys() {}

    /**
     * Left and Right (Ctrl: by word), Home and End (alone), Backspace (Ctrl: the word before) and Delete.
     * @return true if the key was one of them (and was applied)
     */
    public static boolean Handle(CommandLineEditor editor, KeyEvent key) {
        switch (key.key()) {
            case GLFW_KEY_BACKSPACE:
                if (key.isControl())
                    editor.deleteWordLeft();
                else
                    editor.deleteLeft();
                return true;
            case GLFW_KEY_DELETE:
                editor.deleteRight();
                return true;
            case GLFW_KEY_LEFT:
                if (key.isControl())
                    editor.wordLeftMove(key.isShift());
                else
                    editor.left(key.isShift());
                return true;
            case GLFW_KEY_RIGHT:
                if (key.isControl())
                    editor.wordRightMove(key.isShift());
                else
                    editor.right(key.isShift());
                return true;
            case GLFW_KEY_HOME:
                // Ctrl+Home is the top of a log, not of a line
                if (key.isControl())
                    return false;
                editor.home(key.isShift());
                return true;
            case GLFW_KEY_END:
                if (key.isControl())
                    return false;
                editor.end(key.isShift());
                return true;
            default:
                return false;
        }
    }

    /** True for Ctrl + a key that has a letter in the layout of the system, not Ctrl+Alt (AltGr types characters). */
    public static boolean IsShortcut(KeyEvent key) {
        return key.isControl() && !key.isAlt() && key.letter() != 0;
    }

    /**
     * Ctrl+A (all), Ctrl+C, Ctrl+X, Ctrl+V of a line of text on its own: a pasted text loses its line feeds.
     * @return true if the key was one of them
     */
    public static boolean HandleShortcut(CommandLineEditor editor, KeyEvent key, ConsoleInput.IClipboard clipboard) {
        if (!IsShortcut(key))
            return false;

        switch (key.letter()) {
            case 'a':
                editor.selectAll();
                return true;
            case 'c':
                if (clipboard != null && editor.hasSelection())
                    clipboard.set(editor.selectedText());
                return true;
            case 'x':
                if (clipboard != null && editor.hasSelection())
                    clipboard.set(editor.cutSelection());
                return true;
            case 'v':
                if (clipboard != null) {
                    editor.insert(clipboard.get());
                }
                return true;
            default:
                return false;
        }
    }
}
