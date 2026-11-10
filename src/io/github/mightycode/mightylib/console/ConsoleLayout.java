package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Layout of the console log in a panel, without GL: the lines cut to the width, the rows shown, the scroll and the
 * conversions between a point of the panel and a place in the log. The width of the characters is given (the font in
 * the game, a fixed width in the tests).
 * The scroll is anchored on a line of the log, not counted from the bottom: new lines do not move a view scrolled
 * up. The work grows with what is shown, not with the size of the log.
 */
final class ConsoleLayout {
    /** Width of a character in pixels. */
    interface ICharAdvance {
        float of(char c);
    }

    /** The bottom of the view: a line of the log and the rows of this line hidden below the view. */
    static final class Anchor {
        private final long lineId;
        private final int rowsBelow;

        Anchor(long lineId, int rowsBelow) {
            this.lineId = lineId;
            this.rowsBelow = rowsBelow;
        }

        long lineId() { return lineId; }

        int rowsBelow() { return rowsBelow; }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Anchor))
                return false;

            Anchor anchor = (Anchor) other;
            return lineId == anchor.lineId && rowsBelow == anchor.rowsBelow;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(lineId) * 31 + rowsBelow;
        }

        @Override
        public String toString() {
            return lineId + "+" + rowsBelow;
        }
    }

    /** One row shown: a part of a log line, from the character start (included) to end (excluded). */
    static final class Row {
        private final ConsoleLog.Line line;
        private final String text;
        private final int start;
        private final int part;
        private final int parts;

        private Row(ConsoleLog.Line line, String text, int start, int part, int parts) {
            this.line = line;
            this.text = text;
            this.start = start;
            this.part = part;
            this.parts = parts;
        }

        ConsoleLog.Line line() { return line; }

        String text() { return text; }

        int start() { return start; }

        int end() { return start + text.length(); }

        // Index of the row in its line, and number of rows of the line
        int part() { return part; }

        int parts() { return parts; }

        boolean isLastPart() { return part == parts - 1; }
    }

    private final ConsoleLog log;
    private final ICharAdvance advance;

    private float width = 1;
    private int visibleRows;
    private float rowHeight = 1;
    private ECommandLevel viewer = ECommandLevel.USER;

    // null: follow the end of the log
    private Anchor anchor;

    // Computed by build()
    private List<Row> rows = Collections.emptyList();
    private Anchor bottom;
    private boolean atEnd = true;
    private boolean dirty = true;
    private long builtVersion = -1;

    ConsoleLayout(ConsoleLog log, ICharAdvance advance) {
        this.log = log;
        this.advance = advance;
    }

    /** Text of a line as shown: tabulations replaced by 4 spaces. The characters of a LogPosition are counted in it. */
    static String DisplayedText(ConsoleLog.Line line) {
        return line.toString().replace("\t", "    ");
    }

    // ---- Settings ----

    /**
     * @param width     width of the text, in pixels
     * @param rows      rows shown at once
     * @param rowHeight distance between two rows, in pixels
     */
    void setGeometry(float width, int rows, float rowHeight) {
        if (width == this.width && rows == visibleRows && rowHeight == this.rowHeight)
            return;

        this.width = width;
        this.visibleRows = Math.max(0, rows);
        this.rowHeight = Math.max(1, rowHeight);
        dirty = true;
    }

    // Only the lines this level sees are shown
    void setViewer(ECommandLevel viewer) {
        if (viewer == this.viewer)
            return;

        this.viewer = viewer;
        dirty = true;
    }

    // The width of the characters changed (font size)
    void invalidate() {
        dirty = true;
    }

    int visibleRows() { return visibleRows; }

    float rowHeight() { return rowHeight; }

    // ---- Rows ----

    /** Compute the rows again if the log (what the viewer sees), the scroll or the settings changed. */
    boolean refresh() {
        if (!dirty && log.version(viewer) == builtVersion)
            return false;

        build();
        return true;
    }

    // The rows shown, oldest first; fewer than the visible rows when the log is short (the panel puts them at the bottom)
    List<Row> rows() { return rows; }

    // Slot of the panel (0: the top row) where a row of rows() is drawn
    int slotOf(int rowIndex) {
        return visibleRows - rows.size() + rowIndex;
    }

    // null: the view follows the end of the log
    Anchor anchor() { return anchor; }

    // The bottom of what is shown (null: nothing shown), whatever the anchor
    Anchor bottom() { return bottom; }

    // Nothing visible below the view
    boolean isAtEnd() { return atEnd; }

    private void build() {
        dirty = false;
        // Read before the lines: a line added meanwhile changes it, the next refresh builds again
        builtVersion = log.version(viewer);
        rows = Collections.emptyList();
        bottom = null;
        atEnd = true;
        if (visibleRows <= 0)
            return;

        ConsoleLog.Line bottomLine;
        int hidden;
        boolean normalize = false;

        if (anchor != null && anchor.lineId <= log.clearedUpTo(viewer)) {
            // The log was cleared: back to the end
            anchor = null;
        }

        if (anchor == null) {
            List<ConsoleLog.Line> newest = log.upTo(Long.MAX_VALUE, 1, viewer, null);
            if (newest.isEmpty())
                return;
            bottomLine = newest.get(0);
            hidden = 0;
        } else if (anchor.lineId < log.oldestId()) {
            // The line was forgotten (ring buffer): the oldest line kept, at the top
            List<ConsoleLog.Line> oldest = log.after(0, 1, viewer, null);
            if (oldest.isEmpty()) {
                anchor = null;
                return;
            }
            bottomLine = oldest.get(0);
            hidden = wrap(DisplayedText(bottomLine)).size() - 1;
            normalize = true;
        } else {
            // The line itself, or the nearest visible line before it: the anchor does not change, showing the
            // hidden lines again comes back to the same place
            List<ConsoleLog.Line> found = log.upTo(anchor.lineId, 1, viewer, null);
            if (!found.isEmpty()) {
                bottomLine = found.get(0);
                int parts = wrap(DisplayedText(bottomLine)).size();
                hidden = bottomLine.getId() == anchor.lineId ? Math.min(anchor.rowsBelow, parts - 1) : 0;
            } else {
                List<ConsoleLog.Line> next = log.after(anchor.lineId, 1, viewer, null);
                if (next.isEmpty())
                    return;
                bottomLine = next.get(0);
                hidden = wrap(DisplayedText(bottomLine)).size() - 1;
            }
        }

        fill(bottomLine, hidden);

        if (normalize)
            anchor = atEnd ? null : bottom;
    }

    // The rows ending with the given one, then the rows below when the oldest line is reached (view clamped at the top)
    private void fill(ConsoleLog.Line bottomLine, int hidden) {
        List<Row> bottomParts = rowsOf(bottomLine);
        List<Row> fromNewest = new ArrayList<>();
        for (int p = bottomParts.size() - 1 - hidden; p >= 0 && fromNewest.size() < visibleRows; --p)
            fromNewest.add(bottomParts.get(p));

        if (fromNewest.size() < visibleRows) {
            // Every line gives at least one row: that many lines are enough
            List<ConsoleLog.Line> before = log.upTo(bottomLine.getId() - 1, visibleRows - fromNewest.size(), viewer, null);
            for (int i = before.size() - 1; i >= 0 && fromNewest.size() < visibleRows; --i) {
                List<Row> parts = rowsOf(before.get(i));
                for (int p = parts.size() - 1; p >= 0 && fromNewest.size() < visibleRows; --p)
                    fromNewest.add(parts.get(p));
            }
        }

        List<Row> result = new ArrayList<>(visibleRows);
        for (int i = fromNewest.size() - 1; i >= 0; --i)
            result.add(fromNewest.get(i));

        // The oldest line is shown and there is room left: the rows below the anchor come up
        if (result.size() < visibleRows) {
            for (int p = bottomParts.size() - hidden; p < bottomParts.size() && result.size() < visibleRows; ++p)
                result.add(bottomParts.get(p));

            if (result.size() < visibleRows) {
                List<ConsoleLog.Line> after = log.after(bottomLine.getId(), visibleRows - result.size(), viewer, null);
                for (ConsoleLog.Line line : after) {
                    for (Row row : rowsOf(line)) {
                        if (result.size() >= visibleRows)
                            break;
                        result.add(row);
                    }
                }
            }
        }

        rows = Collections.unmodifiableList(result);
        Row last = result.get(result.size() - 1);
        bottom = new Anchor(last.line.getId(), last.parts - 1 - last.part);
        atEnd = last.isLastPart() && log.after(last.line.getId(), 1, viewer, null).isEmpty();
    }

    private List<Row> rowsOf(ConsoleLog.Line line) {
        List<String> parts = wrap(DisplayedText(line));
        List<Row> result = new ArrayList<>(parts.size());
        int start = 0;
        for (int p = 0; p < parts.size(); ++p) {
            result.add(new Row(line, parts.get(p), start, p, parts.size()));
            start += parts.get(p).length();
        }

        return result;
    }

    // Cut a text on the characters that pass the width (at least one character per row)
    List<String> wrap(String text) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        float used = 0;
        for (int i = 0; i < text.length(); ++i) {
            float charWidth = advance.of(text.charAt(i));
            if (used + charWidth > width && i > start) {
                parts.add(text.substring(start, i));
                start = i;
                used = 0;
            }
            used += charWidth;
        }

        parts.add(text.substring(start));
        return parts;
    }

    // ---- Scroll ----

    /** Toward the oldest lines (positive) or the newest (negative), by rows, from what is shown. */
    void scroll(int amount) {
        refresh();
        if (amount == 0 || bottom == null)
            return;

        ConsoleLog.Line line = rows.get(rows.size() - 1).line;
        int hidden = bottom.rowsBelow + amount;

        if (amount > 0) {
            int parts = wrap(DisplayedText(line)).size();
            while (hidden >= parts) {
                List<ConsoleLog.Line> previous = log.upTo(line.getId() - 1, 1, viewer, null);
                if (previous.isEmpty()) {
                    // The top of the log: clamped by the layout
                    hidden = parts - 1;
                    break;
                }
                hidden -= parts;
                line = previous.get(0);
                parts = wrap(DisplayedText(line)).size();
            }
        } else {
            while (hidden < 0) {
                List<ConsoleLog.Line> next = log.after(line.getId(), 1, viewer, null);
                if (next.isEmpty()) {
                    scrollToEnd();
                    return;
                }
                line = next.get(0);
                hidden += wrap(DisplayedText(line)).size();
            }
        }

        moveTo(line, hidden);
    }

    /** The oldest line kept at the top. */
    void scrollToTop() {
        List<ConsoleLog.Line> oldest = log.after(0, 1, viewer, null);
        if (oldest.isEmpty())
            return;

        moveTo(oldest.get(0), wrap(DisplayedText(oldest.get(0))).size() - 1);
    }

    /** Follow the end of the log again. */
    void scrollToEnd() {
        if (anchor != null) {
            anchor = null;
            dirty = true;
        }
    }

    // The anchor becomes what is really shown (clamped at the top), or the end
    private void moveTo(ConsoleLog.Line line, int hidden) {
        anchor = new Anchor(line.getId(), hidden);
        build();
        anchor = atEnd ? null : bottom;
    }

    // ---- Conversions ----

    /**
     * The place in the log under a point of the panel, the nearest one outside the rows; null when nothing is shown.
     * @param x pixels from the left of the text
     * @param y pixels from the top of the first slot (the top row of the panel)
     */
    LogPosition positionAt(float x, float y) {
        refresh();
        if (rows.isEmpty())
            return null;

        int slot = (int) Math.floor(y / rowHeight);
        int index = Math.max(0, Math.min(rows.size() - 1, slot - slotOf(0)));
        Row row = rows.get(index);
        return new LogPosition(row.line.getId(), row.start + indexAt(row.text, x));
    }

    /** Index in rows() of the row that shows the place, -1 when it is not shown. */
    int rowIndexOf(LogPosition position) {
        for (int i = 0; i < rows.size(); ++i) {
            Row row = rows.get(i);
            if (row.line.getId() != position.lineId())
                continue;

            // The end of a part is the beginning of the next one, except for the last part
            if (position.character() < row.end() || (row.isLastPart() && position.character() >= row.start))
                return i;
        }

        return -1;
    }

    /** Pixels from the left of the text to the character of the row (clamped to the row). */
    float xOf(Row row, int character) {
        int end = Math.max(0, Math.min(row.text.length(), character - row.start));
        return width(row.text, 0, end);
    }

    // Index between two characters nearest to x (0: before the first one, length: after the last one)
    int indexAt(CharSequence text, float x) {
        float used = 0;
        for (int i = 0; i < text.length(); ++i) {
            float charWidth = advance.of(text.charAt(i));
            if (x < used + charWidth / 2)
                return i;
            used += charWidth;
        }

        return text.length();
    }

    float width(CharSequence text, int start, int end) {
        float used = 0;
        for (int i = start; i < end; ++i)
            used += advance.of(text.charAt(i));

        return used;
    }
}
