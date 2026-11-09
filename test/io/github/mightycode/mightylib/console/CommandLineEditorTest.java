package io.github.mightycode.mightylib.console;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandLineEditorTest {
    @Test
    void insertAndDeleteAroundTheCursor() {
        CommandLineEditor editor = new CommandLineEditor();
        editor.insert("helo");
        editor.left();
        editor.insert("l");
        assertEquals("hello", editor.getText());
        assertEquals(4, editor.getCursor());

        editor.deleteRight();
        assertEquals("hell", editor.getText());
        editor.deleteLeft();
        assertEquals("hel", editor.getText());
        assertEquals(3, editor.getCursor());

        editor.home();
        editor.deleteLeft();
        assertEquals("hel", editor.getText());
        editor.end();
        editor.deleteRight();
        assertEquals("hel", editor.getText());
    }

    @Test
    void pastedTextStaysOnOneLine() {
        CommandLineEditor editor = new CommandLineEditor();
        editor.insert("a\r\nb\tc");
        assertEquals("a b c", editor.getText());
    }

    @Test
    void wordMovesSkipSpacesThenTheWord() {
        CommandLineEditor editor = new CommandLineEditor();
        editor.insert("input key  ENTER");

        editor.wordLeftMove();
        assertEquals(11, editor.getCursor());
        editor.wordLeftMove();
        assertEquals(6, editor.getCursor());
        editor.wordRightMove();
        assertEquals(9, editor.getCursor());
        editor.wordRightMove();
        assertEquals(16, editor.getCursor());

        editor.deleteWordLeft();
        assertEquals("input key  ", editor.getText());
    }

    @Test
    void historyKeepsTheDraftAndSkipsRepeatsAndBlankLines() {
        CommandLineEditor editor = new CommandLineEditor(10);
        editor.insert("stats");
        assertEquals("stats", editor.submit());
        editor.insert("stats");
        editor.submit();
        editor.insert("   ");
        editor.submit();
        editor.insert("gl");
        editor.submit();
        assertEquals(List.of("stats", "gl"), editor.getHistory());

        editor.insert("dra");
        editor.previous();
        assertEquals("gl", editor.getText());
        editor.previous();
        assertEquals("stats", editor.getText());
        editor.previous();
        assertEquals("stats", editor.getText());
        editor.next();
        assertEquals("gl", editor.getText());
        editor.next();
        assertEquals("dra", editor.getText());
        editor.next();
        assertEquals("dra", editor.getText());
    }

    @Test
    void historyIsLimited() {
        CommandLineEditor editor = new CommandLineEditor(2);
        for (String line : new String[]{"a", "b", "c"}) {
            editor.insert(line);
            editor.submit();
        }

        assertEquals(List.of("b", "c"), editor.getHistory());
    }

    @Test
    void replaceBeforeCursorKeepsTheEnd() {
        CommandLineEditor editor = new CommandLineEditor();
        editor.insert("sce Scene2D");
        editor.home();
        editor.right();
        editor.right();
        editor.right();
        editor.replaceBeforeCursor(0, "scene");
        assertEquals("scene Scene2D", editor.getText());
        assertEquals(5, editor.getCursor());
    }
}
