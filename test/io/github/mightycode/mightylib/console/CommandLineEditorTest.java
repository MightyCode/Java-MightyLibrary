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

    @Test
    void movesWithShiftExtendTheSelectionWithoutRemoveIt() {
        CommandLineEditor editor = new CommandLineEditor();
        editor.insert("stats fps now");
        editor.wordLeftMove(true);
        assertEquals("now", editor.selectedText());
        editor.wordLeftMove(true);
        assertEquals("fps now", editor.selectedText());
        // Back past the anchor: the other side
        editor.end(true);
        assertFalse(editor.hasSelection());
        editor.home(true);
        assertEquals("stats fps now", editor.selectedText());
        assertTrue(editor.isAllSelected());
        editor.right(true);
        assertEquals("tats fps now", editor.selectedText());
        assertEquals(1, editor.selectionStart());
        assertEquals(13, editor.selectionEnd());

        // Left without Shift: the beginning of the selection, which is removed
        editor.left();
        assertFalse(editor.hasSelection());
        assertEquals(1, editor.getCursor());
        editor.select(6, 9);
        editor.right();
        assertEquals(9, editor.getCursor());
        assertFalse(editor.hasSelection());
        editor.select(0, 5);
        editor.end();
        assertFalse(editor.hasSelection());
    }

    @Test
    void typingAndDeletingReplaceTheSelection() {
        CommandLineEditor editor = new CommandLineEditor();
        editor.insert("stats fps");
        editor.select(6, 9);
        editor.insert("all");
        assertEquals("stats all", editor.getText());
        assertEquals(9, editor.getCursor());

        editor.select(0, 6);
        editor.deleteLeft();
        assertEquals("all", editor.getText());
        editor.select(1, 2);
        editor.deleteRight();
        assertEquals("al", editor.getText());
        editor.selectAll();
        editor.deleteWordLeft();
        assertEquals("", editor.getText());

        editor.insert("stats fps");
        editor.select(9, 5);
        assertEquals(" fps", editor.cutSelection());
        assertEquals("stats", editor.getText());
        assertEquals(5, editor.getCursor());
    }

    @Test
    void historyCompletionAndSubmitRemoveTheSelection() {
        CommandLineEditor editor = new CommandLineEditor();
        editor.insert("first");
        editor.submit();
        editor.insert("second");
        editor.selectAll();
        editor.previous();
        assertFalse(editor.hasSelection());
        assertEquals("first", editor.getText());
        editor.selectAll();
        editor.previous();
        assertFalse(editor.hasSelection());
        editor.selectAll();
        editor.next();
        assertFalse(editor.hasSelection());

        editor.select(0, 2);
        editor.replaceBeforeCursor(0, "se");
        assertFalse(editor.hasSelection());
        editor.selectAll();
        editor.submit();
        assertFalse(editor.hasSelection());
    }

    @Test
    void pasteSeveralLines() {
        CommandLineEditor editor = new CommandLineEditor();
        editor.insert("ab");
        editor.select(1, 2);
        assertEquals(List.of("a1", "2"), editor.paste("1\n\n2\r\n3\t"));
        // The selection was replaced: nothing after the cursor
        assertEquals("3 ", editor.getText());
        assertEquals(2, editor.getCursor());

        // One line: inserted
        assertEquals(List.of(), editor.paste("x"));
        assertEquals("3 x", editor.getText());
    }
}
