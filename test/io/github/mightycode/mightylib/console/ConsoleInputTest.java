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

    // ---- Mouse: rows from y = 0 to 100, the command line below, its text from x = 20, characters 1 pixel wide ----

    private static final float INPUT_Y = 110;

    private static ConsoleLayout mouseLayout(ConsoleLog log) {
        ConsoleLayout layout = new ConsoleLayout(log, c -> 1f);
        layout.setGeometry(100, 5, 20);
        layout.setViewer(ECommandLevel.DEV);
        layout.setPanelGeometry(6, 0, 20, 0);
        return layout;
    }

    private static ConsoleInput.MouseState press(float x, float y, int clicks, boolean shift) {
        return new ConsoleInput.MouseState(x, y, true, true, false, clicks, shift);
    }

    private static ConsoleInput.MouseState hold(float x, float y) {
        return new ConsoleInput.MouseState(x, y, false, true, false, 0, false);
    }

    private static ConsoleInput.MouseState release(float x, float y) {
        return new ConsoleInput.MouseState(x, y, false, false, true, 0, false);
    }

    @Test
    void aClickPlacesTheCursorAndDraggingSelects() {
        ConsoleLayout layout = mouseLayout(new ConsoleLog(10));
        handle("stats fps");

        input.handleMouse(press(22.4f, INPUT_Y, 1, false), layout);
        assertEquals(2, editor.getCursor());
        assertFalse(editor.hasSelection());

        input.handleMouse(hold(25, INPUT_Y), layout);
        assertEquals("ats", editor.selectedText());
        // Dragging out of the line: the nearest character
        input.handleMouse(hold(500, 0), layout);
        assertEquals("ats fps", editor.selectedText());
        input.handleMouse(release(500, 0), layout);
        // Released: moving changes nothing
        input.handleMouse(new ConsoleInput.MouseState(20, INPUT_Y, false, false, false, 0, false), layout);
        assertEquals("ats fps", editor.selectedText());

        // Shift+click extends from the anchor
        input.handleMouse(press(20, INPUT_Y, 1, true), layout);
        assertEquals("st", editor.selectedText());
        input.handleMouse(release(20, INPUT_Y), layout);
        input.handleMouse(press(29, INPUT_Y, 1, false), layout);
        assertFalse(editor.hasSelection());
        assertEquals(9, editor.getCursor());
    }

    @Test
    void doubleClickSelectsAWordTripleClickTheLine() {
        ConsoleLayout layout = mouseLayout(new ConsoleLog(10));
        handle("stats fps now");

        input.handleMouse(press(27, INPUT_Y, 1, false), layout);
        input.handleMouse(release(27, INPUT_Y), layout);
        input.handleMouse(press(27, INPUT_Y, 2, false), layout);
        assertEquals("fps", editor.selectedText());
        // Dragging after a double click keeps the word
        input.handleMouse(hold(35, INPUT_Y), layout);
        assertEquals("fps", editor.selectedText());
        input.handleMouse(release(27, INPUT_Y), layout);

        input.handleMouse(press(27, INPUT_Y, 3, false), layout);
        assertTrue(editor.isAllSelected());

        // A press in the log does not touch the command line
        editor.setText("abc");
        input.handleMouse(press(10, 50, 1, false), layout);
        assertEquals(3, editor.getCursor());
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
