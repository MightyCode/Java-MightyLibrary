package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.inputs.KeyEvent;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.TestCommands;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.suggest;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class ConsoleInputTest {
    private CommandLineEditor editor;
    private List<String> printed;
    private ConsoleInput input;
    // The system clipboard
    private String clipboard;

    private final ConsoleInput.IClipboard fakeClipboard = new ConsoleInput.IClipboard() {
        @Override
        public String get() { return clipboard; }

        @Override
        public void set(String text) { clipboard = text; }
    };

    private static class Command extends BaseCommand {
        Command(String name) {
            super(TestCommands.Id(name), ECommandLevel.USER, "A command of the tests");
        }

        @Override
        protected void syntax(CommandNode root) {
            root.executes(context -> CommandResult.ok(""))
                    .then(argument("word", suggest(() -> Arrays.asList("alpha", "alpine", "beta"))).optional()
                            .executes(context -> CommandResult.ok("")));
        }
    }

    @BeforeEach
    void setUp() {
        // General commands keep a null scene manager, enough without GL
        Commands commands = new Commands();
        TestCommands.add(commands, new Command("zzone"));
        TestCommands.add(commands, new Command("zztwo"));

        editor = new CommandLineEditor();
        printed = new ArrayList<>();
        input = new ConsoleInput(editor, commands::complete, printed::add);
        clipboard = "pasted text";
    }

    // A key without a letter in the layout (arrows, Enter...)
    private static KeyEvent key(int key) {
        return new KeyEvent(key, 0, 0, false, (char) 0);
    }

    private static KeyEvent control(int key) {
        return new KeyEvent(key, 0, GLFW_MOD_CONTROL, false, (char) 0);
    }

    private static KeyEvent shift(int key) {
        return new KeyEvent(key, 0, GLFW_MOD_SHIFT, false, (char) 0);
    }

    private static KeyEvent controlShift(int key) {
        return new KeyEvent(key, 0, GLFW_MOD_CONTROL | GLFW_MOD_SHIFT, false, (char) 0);
    }

    // Ctrl + the key that has this letter in a QWERTY layout
    private static KeyEvent control(char letter) {
        return new KeyEvent(GLFW_KEY_A + (Character.toUpperCase(letter) - 'A'), 0, GLFW_MOD_CONTROL, false, letter);
    }

    // Ctrl + a key at a given position whose letter comes from the layout
    private static KeyEvent control(int key, char letter) {
        return new KeyEvent(key, 0, GLFW_MOD_CONTROL, false, letter);
    }

    private List<String> handle(String text, KeyEvent... keys) {
        return input.handle(text, Arrays.asList(keys), 10, fakeClipboard).stream()
                .map(ConsoleInput.Request::toString).collect(Collectors.toList());
    }

    @Test
    void typedTextThenEnterSubmitsAndEmpties() {
        assertEquals(List.of(), handle("stats fps"));
        assertEquals(List.of("SUBMIT stats fps", "SCROLL_END"), handle("", key(GLFW_KEY_ENTER)));
        assertEquals("", editor.getText());

        // A blank line is not run
        handle("   ");
        assertEquals(List.of("SCROLL_END"), handle("", key(GLFW_KEY_ENTER)));
    }

    @Test
    void editingKeysAndHistory() {
        handle("abc", key(GLFW_KEY_LEFT), key(GLFW_KEY_BACKSPACE));
        assertEquals("ac", editor.getText());
        handle("", key(GLFW_KEY_HOME), key(GLFW_KEY_DELETE));
        assertEquals("c", editor.getText());
        handle("", key(GLFW_KEY_END), key(GLFW_KEY_ENTER));

        handle("x y", control(GLFW_KEY_BACKSPACE));
        assertEquals("x ", editor.getText());
        handle("", key(GLFW_KEY_UP));
        assertEquals("c", editor.getText());
        handle("", key(GLFW_KEY_DOWN));
        assertEquals("x ", editor.getText());

        handle("", control('v'));
        assertEquals("x pasted text", editor.getText());
    }

    @Test
    void shortcutsFollowTheLetterOfTheLayoutNotThePosition() {
        // AZERTY: the key at the position of Q gives 'a', the one of W gives 'z'; V and L do not move, but a
        // layout may move them
        handle("", control(GLFW_KEY_Q, 'v'));
        assertEquals("pasted text", editor.getText());

        assertEquals(List.of("CLEAR"), handle("", control(GLFW_KEY_SEMICOLON, 'l')));
        // The position of V with another letter is not a paste, a key without a letter is no shortcut
        editor.setText("");
        assertEquals(List.of(), handle("", control(GLFW_KEY_V, 'q'), control(GLFW_KEY_L)));
        assertEquals("", editor.getText());
    }

    @Test
    void shiftMovesSelectAndControlShortcutsCopyCutPaste() {
        handle("stats fps", shift(GLFW_KEY_LEFT), shift(GLFW_KEY_LEFT), shift(GLFW_KEY_LEFT));
        assertEquals("fps", editor.selectedText());
        // Ctrl+C copies and keeps the selection
        handle("", control('c'));
        assertEquals("fps", clipboard);
        assertTrue(editor.hasSelection());

        // Ctrl+Shift+Left: by word; typing replaces the selection
        handle("", controlShift(GLFW_KEY_LEFT));
        assertEquals("stats fps", editor.selectedText());
        handle("help");
        assertEquals("help", editor.getText());

        // Ctrl+X cuts, Ctrl+V pastes over the selection
        handle("", shift(GLFW_KEY_HOME), control('x'));
        assertEquals("help", clipboard);
        assertEquals("", editor.getText());
        handle("stats", shift(GLFW_KEY_HOME), control('v'));
        assertEquals("help", editor.getText());
        assertFalse(editor.hasSelection());

        // Nothing selected: Ctrl+C copies nothing
        clipboard = "kept";
        handle("", control('c'), control('x'));
        assertEquals("kept", clipboard);
        assertEquals("help", editor.getText());
    }

    @Test
    void controlASelectsTheCommandLineFirst() {
        handle("stats fps", control('a'));
        assertTrue(editor.isAllSelected());
        // Second press: the log (LogSelection), the command line is no longer selected by it
        handle("", key(GLFW_KEY_END));
        assertFalse(editor.hasSelection());
        // Empty command line: nothing to select in it
        editor.setText("");
        handle("", control('a'));
        assertFalse(editor.hasSelection());
    }

    @Test
    void pastingSeveralLinesRunsACommandPerCompleteLine() {
        clipboard = "stats fps\r\n\r\nhelp\rhelp st";
        handle("x", key(GLFW_KEY_LEFT));
        assertEquals(List.of("SUBMIT stats fps", "SUBMIT help", "SCROLL_END"), handle("", control('v')));
        // The last line stays, the text after the cursor too, the cursor after the pasted part
        assertEquals("help stx", editor.getText());
        assertEquals(7, editor.getCursor());
        assertEquals(List.of("stats fps", "help"), editor.getHistory());
    }

    @Test
    void escapeClearScrollAreRequests() {
        assertEquals(List.of("CLOSE"), handle("", key(GLFW_KEY_ESCAPE)));
        assertEquals(List.of("CLEAR"), handle("", control('l')));
        assertEquals(List.of("SCROLL 10", "SCROLL -10"), handle("", key(GLFW_KEY_PAGE_UP), key(GLFW_KEY_PAGE_DOWN)));
        // Shift+Up/Down: one row; Ctrl+Home/End: top and end of the log
        assertEquals(List.of("SCROLL 1", "SCROLL -1"), handle("", shift(GLFW_KEY_UP), shift(GLFW_KEY_DOWN)));
        assertEquals(List.of("SCROLL_TOP", "SCROLL_END"), handle("", control(GLFW_KEY_HOME), control(GLFW_KEY_END)));
        // F1, F2: actions of the console, not edits
        assertEquals(List.of(), handle("", key(GLFW_KEY_F1), key(GLFW_KEY_F2)));
        assertEquals("", editor.getText());
    }

    @Test
    void tabCompletesTheCommonPartThenListsTheChoices() {
        handle("zzt", key(GLFW_KEY_TAB));
        assertEquals("zztwo ", editor.getText());

        handle("al", key(GLFW_KEY_TAB));
        assertEquals("zztwo alp", editor.getText());
        assertTrue(printed.isEmpty());

        // Nothing more to write: the second Tab lists the choices
        handle("", key(GLFW_KEY_TAB));
        assertEquals(List.of("alpha  alpine"), printed);
        assertEquals("zztwo alp", editor.getText());
    }

    @Test
    void tabInTheMiddleOfTheLineKeepsTheEnd() {
        handle("zzo end", key(GLFW_KEY_LEFT), key(GLFW_KEY_LEFT), key(GLFW_KEY_LEFT), key(GLFW_KEY_LEFT),
                key(GLFW_KEY_TAB));
        assertEquals("zzone  end", editor.getText());
        assertEquals(6, editor.getCursor());
    }

    @Test
    void ambiguousTabWithoutProgressNeedsTwoTabs() {
        handle("zz", key(GLFW_KEY_TAB));
        assertEquals("zz", editor.getText());
        assertTrue(printed.isEmpty());

        // Any other key in between starts again
        handle("", key(GLFW_KEY_END), key(GLFW_KEY_TAB));
        assertTrue(printed.isEmpty());
        handle("", key(GLFW_KEY_TAB));
        assertEquals(List.of("zzone  zztwo"), printed);
    }
}
