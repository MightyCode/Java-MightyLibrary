package io.github.mightycode.mightylib.graphics.GUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The items of a scrolling list, the selected one and the first one shown, without GL (ScrollList draws it).
 * The selection is always on an item when there is one. Moving it scrolls the list just enough to show it;
 * scrolling by itself (the wheel) leaves it where it is, even out of view.
 */
final class ListSelection<T> {
    private final int visibleRows;
    private final List<T> items = new ArrayList<>();
    private int first;
    private int selected = -1;

    ListSelection(int visibleRows) {
        this.visibleRows = Math.max(1, visibleRows);
    }

    List<T> items() { return Collections.unmodifiableList(items); }

    int count() { return items.size(); }

    int visibleRows() { return visibleRows; }

    /** Index of the first item shown. */
    int first() { return first; }

    /** Index of the selected item, -1 when the list is empty. */
    int selected() { return selected; }

    T selectedItem() { return selected < 0 ? null : items.get(selected); }

    /**
     * Replaces the items. The selection stays on the same item (equals) if it is still there, otherwise it goes to
     * the first one.
     * @return true if the selected item is not the same any more
     */
    boolean setItems(List<? extends T> newItems) {
        T before = selectedItem();

        items.clear();
        items.addAll(newItems);

        int index = before == null ? -1 : items.indexOf(before);
        selected = items.isEmpty() ? -1 : Math.max(0, index);
        first = clampFirst(first);
        ensureVisible();

        return before == null ? selected >= 0 : (selected < 0 || !before.equals(items.get(selected)));
    }

    /** @return true if the selection moved */
    boolean select(int index) {
        if (items.isEmpty())
            return false;

        int clamped = Math.max(0, Math.min(index, items.size() - 1));
        boolean changed = clamped != selected;
        selected = clamped;
        ensureVisible();

        return changed;
    }

    /** One item up (negative) or down; no loop: false at the ends, where the selection stays. */
    boolean move(int delta) {
        if (items.isEmpty())
            return false;

        int target = selected + delta;
        if (target < 0 || target >= items.size())
            return false;

        return select(target);
    }

    boolean isAtStart() { return selected <= 0; }

    boolean isAtEnd() { return selected < 0 || selected >= items.size() - 1; }

    /** @return true if the first item shown changed */
    boolean scrollTo(int newFirst) {
        int clamped = clampFirst(newFirst);
        boolean changed = clamped != first;
        first = clamped;

        return changed;
    }

    private int clampFirst(int value) {
        return Math.max(0, Math.min(value, items.size() - visibleRows));
    }

    private void ensureVisible() {
        if (selected < 0)
            return;

        if (selected < first)
            first = selected;
        else if (selected >= first + visibleRows)
            first = selected - visibleRows + 1;

        first = clampFirst(first);
    }
}
