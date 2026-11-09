package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.inputs.KeyEvent;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.TestCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class ConsoleInputTest {
    private CommandLineEditor editor;
    private List<String> printed;
    private ConsoleInput input;

    private static class Command extends BaseCommand {
        Command(String name) {
            super(name);
        }

        @Override
        public CommandResult execute(String[] args) {
            return CommandResult.ok("");
        }

        @Override
        public Collection<String> completeArgument(String[] args) {
            return args.length == 2 ? Arrays.asList("alpha", "alpine", "beta") : Collections.emptyList();
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
    }

    private static KeyEvent key(int key) {
        return new KeyEvent(key, 0, false);
    }

    private static KeyEvent control(int key) {
        return new KeyEvent(key, GLFW_MOD_CONTROL, false);
    }

    private List<String> handle(String text, KeyEvent... keys) {
        return input.handle(text, Arrays.asList(keys), 10, () -> "pasted text").stream()
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

        handle("", control(GLFW_KEY_V));
        assertEquals("x pasted text", editor.getText());
    }

    @Test
    void escapeClearScrollAreRequests() {
        assertEquals(List.of("CLOSE"), handle("", key(GLFW_KEY_ESCAPE)));
        assertEquals(List.of("CLEAR"), handle("", control(GLFW_KEY_L)));
        assertEquals(List.of("SCROLL 10", "SCROLL -10"), handle("", key(GLFW_KEY_PAGE_UP), key(GLFW_KEY_PAGE_DOWN)));
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
