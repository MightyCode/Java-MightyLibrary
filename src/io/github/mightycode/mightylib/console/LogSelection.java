package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;

import java.util.List;
import java.util.function.Predicate;

/**
 * A selection in the console log: from an anchor (where the mouse was pressed) to a focus (where it is now), in
 * places of the log (line id, character of the displayed text), so independent of the width and of the scroll.
 */
final class LogSelection {
    private LogPosition anchor;
    private LogPosition focus;

    LogSelection(LogPosition anchor, LogPosition focus) {
        this.anchor = anchor;
        this.focus = focus;
    }

    LogPosition anchor() { return anchor; }

    LogPosition focus() { return focus; }

    // The first place selected, whatever the direction of the selection
    LogPosition start() { return anchor.compareTo(focus) <= 0 ? anchor : focus; }

    // After the last character selected
    LogPosition end() { return anchor.compareTo(focus) <= 0 ? focus : anchor; }

    boolean isEmpty() { return anchor.equals(focus); }

    void extendTo(LogPosition position) {
        focus = position;
    }

    /**
     * The lines before oldestId are forgotten (ring buffer) or cleared: a bound on them goes to the beginning of the
     * oldest line kept.
     * @return false when every line of the selection is forgotten: the selection is dropped
     */
    boolean keepFrom(long oldestId) {
        if (end().lineId() < oldestId)
            return false;

        if (anchor.lineId() < oldestId)
            anchor = new LogPosition(oldestId, 0);
        if (focus.lineId() < oldestId)
            focus = new LogPosition(oldestId, 0);
        return true;
    }

    /**
     * The text shown between two places of the log, as copied: the lines joined by \n (not cut at the width of the
     * panel), tabulations as shown; the lines the viewer doesn't see or the filter hides are left out.
     * Ctrl+C and the command console copy.
     */
    static String CopyText(ConsoleLog log, ECommandLevel viewer, Predicate<ConsoleLog.Line> filter,
                           LogPosition from, LogPosition to) {
        if (from.compareTo(to) > 0) {
            LogPosition swap = from;
            from = to;
            to = swap;
        }

        List<ConsoleLog.Line> lines = log.between(from.lineId(), to.lineId(), viewer, filter);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines.size(); ++i) {
            ConsoleLog.Line line = lines.get(i);
            String shown = ConsoleLayout.DisplayedText(line);
            int start = line.getId() == from.lineId() ? Math.max(0, Math.min(shown.length(), from.character())) : 0;
            int end = line.getId() == to.lineId() ? Math.max(start, Math.min(shown.length(), to.character()))
                    : shown.length();

            if (i > 0)
                text.append('\n');
            text.append(shown, start, end);
        }

        return text.toString();
    }

    // The last lines up to the end of the log
    static String LastLinesText(ConsoleLog log, ECommandLevel viewer, Predicate<ConsoleLog.Line> filter, int count) {
        return LastLinesText(log, viewer, filter, count, Long.MAX_VALUE);
    }

    /**
     * The last lines the viewer sees (and the filter shows), as copied like a selection: joined by line breaks.
     * Empty when there is no line. The command "console copy".
     * @param count lines to copy, 0 or less: every line kept
     * @param upToId the newest line that may be copied (Long.MAX_VALUE: the end of the log)
     */
    static String LastLinesText(ConsoleLog log, ECommandLevel viewer, Predicate<ConsoleLog.Line> filter, int count,
                                long upToId) {
        List<ConsoleLog.Line> lines = log.upTo(upToId, count > 0 ? count : Integer.MAX_VALUE, viewer, filter);
        if (lines.isEmpty())
            return "";

        ConsoleLog.Line last = lines.get(lines.size() - 1);
        return CopyText(log, viewer, filter, new LogPosition(lines.get(0).getId(), 0),
                new LogPosition(last.getId(), ConsoleLayout.DisplayedText(last).length()));
    }

    @Override
    public String toString() {
        return anchor + "-" + focus;
    }
}
