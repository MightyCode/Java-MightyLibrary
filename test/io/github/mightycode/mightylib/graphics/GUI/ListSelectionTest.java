package io.github.mightycode.mightylib.graphics.GUI;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListSelectionTest {
    private static List<String> items(int count) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < count; ++i)
            list.add("item" + i);
        return list;
    }

    private static ListSelection<String> list(int visibleRows, int count) {
        ListSelection<String> selection = new ListSelection<>(visibleRows);
        selection.setItems(items(count));
        return selection;
    }

    @Test
    void emptyListSelectsNothing() {
        ListSelection<String> selection = list(3, 0);

        assertEquals(-1, selection.selected());
        assertNull(selection.selectedItem());
        assertFalse(selection.move(1));
        assertFalse(selection.select(2));
        assertEquals(0, selection.first());
    }

    @Test
    void firstItemIsSelectedByDefault() {
        ListSelection<String> selection = list(3, 10);

        assertEquals(0, selection.selected());
        assertEquals("item0", selection.selectedItem());
    }

    @Test
    void movingDownScrollsToKeepTheSelectionShown() {
        ListSelection<String> selection = list(3, 10);

        selection.move(1);
        selection.move(1);
        assertEquals(2, selection.selected());
        assertEquals(0, selection.first());

        selection.move(1);
        assertEquals(3, selection.selected());
        assertEquals(1, selection.first());
    }

    @Test
    void movingUpScrollsBack() {
        ListSelection<String> selection = list(3, 10);
        selection.select(9);
        assertEquals(7, selection.first());

        selection.move(-1);
        selection.move(-1);
        selection.move(-1);
        assertEquals(6, selection.selected());
        assertEquals(6, selection.first());
    }

    @Test
    void noLoopAtTheEnds() {
        ListSelection<String> selection = list(3, 4);

        assertTrue(selection.isAtStart());
        assertFalse(selection.move(-1));
        assertEquals(0, selection.selected());

        selection.select(3);
        assertTrue(selection.isAtEnd());
        assertFalse(selection.move(1));
        assertEquals(3, selection.selected());
    }

    @Test
    void selectClampsToTheItems() {
        ListSelection<String> selection = list(3, 4);

        selection.select(100);
        assertEquals(3, selection.selected());
        selection.select(-5);
        assertEquals(0, selection.selected());
        assertEquals(0, selection.first());
    }

    @Test
    void selectTellsIfTheSelectionChanged() {
        ListSelection<String> selection = list(3, 4);

        assertFalse(selection.select(0));
        assertTrue(selection.select(2));
        assertFalse(selection.select(2));
    }

    @Test
    void scrollingDoesNotMoveTheSelection() {
        ListSelection<String> selection = list(3, 10);

        assertTrue(selection.scrollTo(5));
        assertEquals(5, selection.first());
        assertEquals(0, selection.selected());

        // Clamped
        selection.scrollTo(100);
        assertEquals(7, selection.first());
        selection.scrollTo(-3);
        assertEquals(0, selection.first());
        assertFalse(selection.scrollTo(0));
    }

    @Test
    void movingBringsBackASelectionScrolledOutOfView() {
        ListSelection<String> selection = list(3, 10);
        selection.scrollTo(7);

        selection.move(1);
        assertEquals(1, selection.selected());
        assertEquals(1, selection.first());
    }

    @Test
    void newItemsKeepTheSelectedOneIfItIsStillThere() {
        ListSelection<String> selection = list(3, 10);
        selection.select(6);

        boolean changed = selection.setItems(List.of("item2", "item6", "item8"));
        assertFalse(changed);
        assertEquals(1, selection.selected());
        assertEquals("item6", selection.selectedItem());
        assertEquals(0, selection.first());
    }

    @Test
    void newItemsWithoutTheSelectedOneSelectTheFirst() {
        ListSelection<String> selection = list(3, 10);
        selection.select(6);

        boolean changed = selection.setItems(List.of("a", "b"));
        assertTrue(changed);
        assertEquals(0, selection.selected());
        assertEquals("a", selection.selectedItem());
        assertEquals(0, selection.first());
    }

    @Test
    void selectionFollowsTheItemWhenItsPlaceChanges() {
        ListSelection<String> selection = list(3, 10);
        selection.select(2);

        List<String> shifted = items(10);
        shifted.add(0, "new");
        shifted.add(0, "new2");
        selection.setItems(shifted);

        assertEquals("item2", selection.selectedItem());
        assertEquals(4, selection.selected());
        // Still shown
        assertTrue(selection.first() <= 4 && selection.first() + 3 > 4);
    }

    @Test
    void emptyingTheListLeavesNoSelectionAndItComesBack() {
        ListSelection<String> selection = list(3, 10);
        selection.select(4);

        selection.setItems(List.of());
        assertEquals(-1, selection.selected());
        assertEquals(0, selection.first());

        assertTrue(selection.setItems(items(5)));
        assertEquals(0, selection.selected());
    }

    @Test
    void shorterListClampsTheFirstShown() {
        ListSelection<String> selection = list(3, 10);
        selection.scrollTo(7);

        selection.setItems(items(4));
        assertEquals(0, selection.selected());
        assertEquals(0, selection.first());
    }
}
