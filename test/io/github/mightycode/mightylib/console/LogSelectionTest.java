package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogSelectionTest {
    private static long add(ConsoleLog log, String text) {
        return log.add(ConsoleLog.EType.RESULT, null, text, ECommandLevel.USER).get(0).getId();
    }

    @Test
    void bothDirectionsGiveTheSameText() {
        ConsoleLog log = new ConsoleLog(10);
        long first = add(log, "alpha beta");
        long second = add(log, "gamma\tdelta");

        LogSelection forward = new LogSelection(new LogPosition(first, 6), new LogPosition(second, 5));
        LogSelection backward = new LogSelection(new LogPosition(second, 5), new LogPosition(first, 6));
        assertEquals(forward.start(), backward.start());
        assertEquals(forward.end(), backward.end());

        String text = LogSelection.CopyText(log, ECommandLevel.DEV, null, backward.anchor(), backward.focus());
        assertEquals("beta\ngamma", text);
        // Tabulations as shown, the character counted in the displayed text
        assertEquals("a    d", LogSelection.CopyText(log, ECommandLevel.DEV, null,
                new LogPosition(second, 4), new LogPosition(second, 10)));
        assertFalse(forward.isEmpty());
        assertTrue(new LogSelection(new LogPosition(first, 2), new LogPosition(first, 2)).isEmpty());
    }

    @Test
    void aLineCutAtTheWidthIsCopiedInOnePiece() {
        ConsoleLog log = new ConsoleLog(10);
        long id = add(log, "abcdefghijklmnopqrstuvwxy");
        ConsoleLayout layout = new ConsoleLayout(log, c -> 1f);
        layout.setGeometry(10, 5, 20);
        layout.setViewer(ECommandLevel.DEV);
        layout.refresh();

        // From the 3rd character of the first row to the 2nd of the third one
        LogPosition from = layout.positionAt(2, 2 * 20 + 1);
        LogPosition to = layout.positionAt(2, 4 * 20 + 1);
        assertEquals(new LogPosition(id, 2), from);
        assertEquals("cdefghijklmnopqrstuv", layout.copyText(from, to));
    }

    @Test
    void hiddenLinesAreNotCopied() {
        ConsoleLog log = new ConsoleLog(10);
        long first = add(log, "seen");
        log.add(ConsoleLog.EType.OUT, null, "program output");
        long last = log.add(ConsoleLog.EType.COMMAND, "game", "help", ECommandLevel.USER).get(0).getId();

        LogPosition from = new LogPosition(first, 0);
        LogPosition to = new LogPosition(last, 100);
        assertEquals("seen\nprogram output\n[game] > help", LogSelection.CopyText(log, ECommandLevel.DEV, null, from, to));
        // The player doesn't see the program output, the filter of the console hides it
        assertEquals("seen\n[game] > help", LogSelection.CopyText(log, ECommandLevel.USER, null, from, to));
        assertEquals("seen\n[game] > help", LogSelection.CopyText(log, ECommandLevel.DEV,
                line -> line.getType() != ConsoleLog.EType.OUT, from, to));
        // A bound on a hidden line: nothing of it
        assertEquals("seen", LogSelection.CopyText(log, ECommandLevel.USER, null, from, new LogPosition(first + 1, 3)));
    }

    @Test
    void forgottenBounds() {
        LogSelection selection = new LogSelection(new LogPosition(8, 3), new LogPosition(3, 2));
        assertTrue(selection.keepFrom(2));
        assertEquals(new LogPosition(3, 2), selection.start());

        // One bound forgotten: the beginning of the oldest line kept
        assertTrue(selection.keepFrom(5));
        assertEquals(new LogPosition(5, 0), selection.start());
        assertEquals(new LogPosition(8, 3), selection.end());

        // Every line forgotten: dropped
        assertFalse(selection.keepFrom(9));
    }
}
