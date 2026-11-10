package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleLayoutTest {
    // Every character is 1 pixel wide, 10 per row, rows of 20 pixels
    private static final float WIDTH = 10;
    private static final float ROW_HEIGHT = 20;

    private static ConsoleLayout layout(ConsoleLog log, int rows) {
        ConsoleLayout layout = new ConsoleLayout(log, c -> 1f);
        layout.setGeometry(WIDTH, rows, ROW_HEIGHT);
        layout.setViewer(ECommandLevel.DEV);
        return layout;
    }

    private static List<String> shown(ConsoleLayout layout) {
        layout.refresh();
        return layout.rows().stream().map(ConsoleLayout.Row::text).collect(Collectors.toList());
    }

    private static long add(ConsoleLog log, String text) {
        return log.add(ConsoleLog.EType.RESULT, null, text, ECommandLevel.USER).get(0).getId();
    }

    @Test
    void linesAreCutAtTheWidth() {
        ConsoleLog log = new ConsoleLog(10);
        add(log, "abcdefghijklmnopqrstuvwxy");
        ConsoleLayout layout = layout(log, 5);

        assertEquals(List.of("abcdefghij", "klmnopqrst", "uvwxy"), shown(layout));
        assertEquals(10, layout.rows().get(1).start());
        assertEquals(20, layout.rows().get(1).end());
        // Fewer rows than the panel: at the bottom
        assertEquals(2, layout.slotOf(0));
        // Tabulations shown as 4 spaces
        add(log, "a\tb");
        assertEquals("a    b", shown(layout).get(3));
    }

    @Test
    void aViewScrolledUpStaysWhenLinesArrive() {
        ConsoleLog log = new ConsoleLog(100);
        for (int i = 1; i <= 10; ++i)
            add(log, "l" + i);
        ConsoleLayout layout = layout(log, 3);

        assertEquals(List.of("l8", "l9", "l10"), shown(layout));
        assertNull(layout.anchor());

        layout.scroll(2);
        assertEquals(List.of("l6", "l7", "l8"), shown(layout));
        ConsoleLayout.Anchor anchor = layout.anchor();
        assertEquals(new ConsoleLayout.Anchor(8, 0), anchor);

        add(log, "l11");
        add(log, "l12");
        assertEquals(List.of("l6", "l7", "l8"), shown(layout));
        assertEquals(anchor, layout.anchor());
        assertFalse(layout.isAtEnd());

        // Back down: anchored on l10 while l11 and l12 are below, then the end is followed again
        layout.scroll(-2);
        assertEquals(List.of("l8", "l9", "l10"), shown(layout));
        layout.scroll(-5);
        assertNull(layout.anchor());
        assertEquals(List.of("l10", "l11", "l12"), shown(layout));
        add(log, "l13");
        assertEquals(List.of("l11", "l12", "l13"), shown(layout));
    }

    @Test
    void scrollOneRowInsideAWrappedLine() {
        ConsoleLog log = new ConsoleLog(10);
        add(log, "short");
        long wrapped = add(log, "abcdefghijklmnopqrstuvwxy");
        add(log, "end");
        ConsoleLayout layout = layout(log, 2);

        assertEquals(List.of("uvwxy", "end"), shown(layout));
        layout.scroll(1);
        assertEquals(List.of("klmnopqrst", "uvwxy"), shown(layout));
        assertEquals(new ConsoleLayout.Anchor(wrapped, 0), layout.anchor());
        layout.scroll(1);
        assertEquals(List.of("abcdefghij", "klmnopqrst"), shown(layout));
        assertEquals(new ConsoleLayout.Anchor(wrapped, 1), layout.anchor());
        layout.scroll(1);
        assertEquals(List.of("short", "abcdefghij"), shown(layout));

        // Past the top: clamped, the anchor is what is shown
        layout.scroll(1);
        assertEquals(List.of("short", "abcdefghij"), shown(layout));
        assertEquals(new ConsoleLayout.Anchor(wrapped, 2), layout.anchor());
        layout.scroll(-1);
        assertEquals(List.of("abcdefghij", "klmnopqrst"), shown(layout));

        // A wider panel: the rows hidden below are clamped to the line, the anchor is kept
        layout.setGeometry(30, 2, ROW_HEIGHT);
        assertEquals(List.of("short", "abcdefghijklmnopqrstuvwxy"), shown(layout));
        assertEquals(new ConsoleLayout.Anchor(wrapped, 1), layout.anchor());
        layout.setGeometry(WIDTH, 2, ROW_HEIGHT);
        assertEquals(List.of("abcdefghij", "klmnopqrst"), shown(layout));

        layout.scrollToEnd();
        layout.scrollToTop();
        assertEquals(List.of("short", "abcdefghij"), shown(layout));
        layout.scrollToEnd();
        assertEquals(List.of("uvwxy", "end"), shown(layout));
    }

    @Test
    void aForgottenAnchorGoesToTheOldestLineKept() {
        ConsoleLog log = new ConsoleLog(5);
        for (int i = 1; i <= 5; ++i)
            add(log, "l" + i);
        ConsoleLayout layout = layout(log, 2);

        layout.scrollToTop();
        assertEquals(List.of("l1", "l2"), shown(layout));
        assertEquals(new ConsoleLayout.Anchor(2, 0), layout.anchor());

        add(log, "l6");
        add(log, "l7");
        assertEquals(List.of("l3", "l4"), shown(layout));
        assertEquals(new ConsoleLayout.Anchor(4, 0), layout.anchor());
    }

    @Test
    void aClearedLogGoesBackToTheEnd() {
        ConsoleLog log = new ConsoleLog(10);
        for (int i = 1; i <= 5; ++i)
            add(log, "l" + i);
        ConsoleLayout layout = layout(log, 2);
        layout.setViewer(ECommandLevel.USER);

        layout.scroll(2);
        assertEquals(List.of("l2", "l3"), shown(layout));
        // Cleared for the player only (Ctrl+L under DEV): the end, where nothing is left
        log.clear(ECommandLevel.USER);
        assertEquals(List.of(), shown(layout));
        assertNull(layout.anchor());
        add(log, "l6");
        assertEquals(List.of("l6"), shown(layout));

        layout.setViewer(ECommandLevel.DEV);
        layout.scroll(3);
        assertNotNull(layout.anchor());
        log.clear();
        add(log, "l7");
        assertEquals(List.of("l7"), shown(layout));
        assertNull(layout.anchor());
    }

    @Test
    void linesHiddenFromTheViewerAreNotShown() {
        ConsoleLog log = new ConsoleLog(10);
        add(log, "seen");
        log.add(ConsoleLog.EType.OUT, null, "program output");
        ConsoleLayout layout = layout(log, 3);
        layout.setViewer(ECommandLevel.USER);

        assertEquals(List.of("seen"), shown(layout));
        // A line the viewer doesn't see changes nothing
        assertFalse(layout.refresh());
        log.add(ConsoleLog.EType.OUT, null, "more output");
        assertFalse(layout.refresh());
    }

    @Test
    void anAnchorOnAHiddenLineComesBackWhenItIsShownAgain() {
        ConsoleLog log = new ConsoleLog(20);
        for (int i = 1; i <= 8; ++i) {
            if (i % 2 == 0)
                log.add(ConsoleLog.EType.OUT, null, "out" + i);
            else
                add(log, "l" + i);
        }
        add(log, "l9");
        ConsoleLayout layout = layout(log, 2);

        // The bottom of the view on out6
        layout.scroll(3);
        assertEquals(List.of("l5", "out6"), shown(layout));
        ConsoleLayout.Anchor anchor = layout.anchor();
        assertEquals(new ConsoleLayout.Anchor(6, 0), anchor);

        // The output hidden: the nearest visible line before it, the anchor kept
        Set<ConsoleLog.EType> output = EnumSet.of(ConsoleLog.EType.OUT, ConsoleLog.EType.ERR);
        layout.setHiddenTypes(output);
        assertEquals(List.of("l3", "l5"), shown(layout));
        assertEquals(anchor, layout.anchor());
        // A hidden line arriving rebuilds nothing, a shown one does
        log.add(ConsoleLog.EType.ERR, null, "err10");
        assertFalse(layout.refresh());
        add(log, "l11");
        assertTrue(layout.refresh());
        assertEquals(List.of("l3", "l5"), shown(layout));

        layout.setHiddenTypes(EnumSet.noneOf(ConsoleLog.EType.class));
        assertEquals(List.of("l5", "out6"), shown(layout));
        assertEquals(anchor, layout.anchor());

        // Every line before the anchor hidden: the view starts at the top
        layout.scrollToTop();
        assertEquals(List.of("l1", "out2"), shown(layout));
        layout.setHiddenTypes(EnumSet.of(ConsoleLog.EType.OUT, ConsoleLog.EType.RESULT));
        assertEquals(List.of("err10"), shown(layout));
    }

    @Test
    void pointsOfThePanelToPlacesInTheLogAndBack() {
        ConsoleLog log = new ConsoleLog(10);
        long id = add(log, "abcdefghijklm");
        ConsoleLayout layout = layout(log, 3);
        assertEquals(List.of("abcdefghij", "klm"), shown(layout));

        // The two rows are in the slots 1 and 2; nearest character boundary
        assertEquals(new LogPosition(id, 2), layout.positionAt(2.4f, ROW_HEIGHT + 1));
        assertEquals(new LogPosition(id, 3), layout.positionAt(2.6f, ROW_HEIGHT + 1));
        assertEquals(new LogPosition(id, 13), layout.positionAt(100, 2 * ROW_HEIGHT + 1));
        assertEquals(new LogPosition(id, 10), layout.positionAt(-5, 2 * ROW_HEIGHT + 1));
        // Outside the rows: the nearest row
        assertEquals(new LogPosition(id, 0), layout.positionAt(0, 0));
        assertEquals(new LogPosition(id, 13), layout.positionAt(100, 10 * ROW_HEIGHT));

        assertEquals(0, layout.rowIndexOf(new LogPosition(id, 9)));
        // The end of a part is the beginning of the next one, the end of the line is in its last part
        assertEquals(1, layout.rowIndexOf(new LogPosition(id, 10)));
        assertEquals(1, layout.rowIndexOf(new LogPosition(id, 13)));
        assertEquals(-1, layout.rowIndexOf(new LogPosition(id + 1, 0)));
        assertEquals(2f, layout.xOf(layout.rows().get(1), 12));
        assertEquals(0f, layout.xOf(layout.rows().get(1), 4));
    }

    @Test
    void linesBelowTheViewAreCountedWithTheFilterAndTheLevel() {
        ConsoleLog log = new ConsoleLog(100);
        for (int i = 1; i <= 6; ++i)
            add(log, "l" + i);
        log.add(ConsoleLog.EType.OUT, null, "out7", ECommandLevel.DEV);
        add(log, "l8");
        ConsoleLayout layout = layout(log, 2);

        // At the end: no indicator
        assertEquals(List.of("out7", "l8"), shown(layout));
        assertEquals(0, layout.linesBelow());
        assertNull(layout.linesBelowText());
        assertTrue(layout.isAtEnd());

        layout.scroll(3);
        assertEquals(List.of("l4", "l5"), shown(layout));
        assertEquals(3, layout.linesBelow());
        assertEquals("v 3 lines", layout.linesBelowText());
        assertFalse(layout.isAtEnd());

        // Hidden lines are not counted
        layout.setHiddenTypes(EnumSet.of(ConsoleLog.EType.OUT));
        assertEquals(List.of("l4", "l5"), shown(layout));
        assertEquals(2, layout.linesBelow());
        // Nor the lines the viewer does not see
        layout.setHiddenTypes(EnumSet.noneOf(ConsoleLog.EType.class));
        layout.setViewer(ECommandLevel.USER);
        assertEquals(2, layout.linesBelow());

        // A new line below
        add(log, "l9");
        layout.refresh();
        assertEquals(3, layout.linesBelow());
        layout.scrollToEnd();
        shown(layout);
        assertEquals(0, layout.linesBelow());
    }

    @Test
    void aLineCutByTheViewCountsAsOneLineBelow() {
        ConsoleLog log = new ConsoleLog(10);
        add(log, "abcdefghijklmnopqrstuvwxy");
        ConsoleLayout layout = layout(log, 2);
        layout.scroll(1);

        assertEquals(List.of("abcdefghij", "klmnopqrst"), shown(layout));
        assertEquals(1, layout.linesBelow());
        assertEquals("v 1 line", layout.linesBelowText());
    }

    @Test
    void theCountOfLinesBelowStopsAtTheMaximum() {
        ConsoleLog log = new ConsoleLog(3000);
        for (int i = 0; i < 1500; ++i)
            add(log, "l" + i);
        ConsoleLayout layout = layout(log, 2);
        layout.scrollToTop();

        assertEquals(ConsoleLayout.MAX_LINES_BELOW + 1, layout.linesBelow());
        assertEquals("v 999+ lines", layout.linesBelowText());

        // Below the maximum: the exact count
        layout.scroll(-600);
        shown(layout);
        assertEquals(898, layout.linesBelow());
        assertEquals("v 898 lines", layout.linesBelowText());
    }

    @Test
    void theIndicatorIsAtTheRightOfTheLastRow() {
        ConsoleLog log = new ConsoleLog(100);
        for (int i = 1; i <= 5; ++i)
            add(log, "l" + i);
        ConsoleLayout layout = new ConsoleLayout(log, c -> 1f);
        layout.setGeometry(100, 2, 20);
        layout.setViewer(ECommandLevel.DEV);
        layout.setPanelGeometry(6, 0, 20, 0);

        assertEquals(0, layout.indicatorWidth());
        assertFalse(layout.isOnIndicator(90, 30));

        layout.scroll(1);
        // "v 1 line" = 8 characters, 4 pixels around
        assertEquals(16, layout.indicatorWidth());
        assertEquals(90, layout.indicatorLeft());
        assertTrue(layout.isOnIndicator(95, 30));
        assertFalse(layout.isOnIndicator(80, 30));
        assertFalse(layout.isOnIndicator(95, 10));
        assertFalse(layout.isOnIndicator(95, 45));
    }
}
