package io.github.mightycode.mightylib.console;

/**
 * A place in the console log: a line (its id) and a character of its displayed text (ConsoleLayout.DisplayedText),
 * from 0 (before the first character) to the length (after the last one). Independent of the width of the panel.
 */
final class LogPosition implements Comparable<LogPosition> {
    private final long lineId;
    private final int character;

    LogPosition(long lineId, int character) {
        this.lineId = lineId;
        this.character = character;
    }

    long lineId() { return lineId; }

    int character() { return character; }

    @Override
    public int compareTo(LogPosition other) {
        if (lineId != other.lineId)
            return Long.compare(lineId, other.lineId);

        return Integer.compare(character, other.character);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LogPosition))
            return false;

        LogPosition position = (LogPosition) other;
        return lineId == position.lineId && character == position.character;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(lineId) * 31 + character;
    }

    @Override
    public String toString() {
        return lineId + ":" + character;
    }
}
