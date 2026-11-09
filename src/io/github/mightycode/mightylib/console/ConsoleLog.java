package io.github.mightycode.mightylib.console;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lines shown by the console: the program output (stdout, stderr) and the commands of every source with their result.
 * Ring buffer: the oldest lines are forgotten once the capacity is reached. Thread safe, written from any thread.
 */
public final class ConsoleLog {
    public static final int DEFAULT_CAPACITY = 5000;

    public enum EType {
        OUT, ERR, COMMAND, RESULT, ERROR
    }

    public static final class Line {
        private final long id;
        private final EType type;
        private final String source;
        private final String text;

        Line(long id, EType type, String source, String text) {
            this.id = id;
            this.type = type;
            this.source = source;
            this.text = text;
        }

        // Increasing from 1, never reused
        public long getId() { return id; }
        public EType getType() { return type; }
        // Source of a command or of its result (game, stdin, server...), null for the program output
        public String getSource() { return source; }
        public String getText() { return text; }

        @Override
        public String toString() {
            if (type == EType.COMMAND)
                return "[" + source + "] > " + text;

            return text;
        }
    }

    private final Line[] lines;
    private int start;
    private int size;
    private long nextId;
    // Changes at each modification (added lines or clear): a display only rebuilds when it changed
    private long version;

    private final CopyOnWriteArrayList<Consumer<Line>> listeners;

    public ConsoleLog() {
        this(DEFAULT_CAPACITY);
    }

    public ConsoleLog(int capacity) {
        if (capacity < 1)
            throw new IllegalArgumentException("The capacity of a console log must be positive");

        lines = new Line[capacity];
        nextId = 1;
        listeners = new CopyOnWriteArrayList<>();
    }

    /** Add one entry, split on the line breaks. */
    public void add(EType type, String source, String text) {
        if (text == null)
            return;

        String[] split = text.split("\r?\n", -1);
        int count = split.length;
        // A final line break does not add an empty line
        if (count > 1 && split[count - 1].isEmpty())
            --count;

        List<Line> added = new ArrayList<>(count);
        synchronized (this) {
            for (int i = 0; i < count; ++i) {
                Line line = new Line(nextId++, type, source, split[i]);
                append(line);
                added.add(line);
            }
            ++version;
        }

        for (Line line : added) {
            for (Consumer<Line> listener : listeners)
                listener.accept(line);
        }
    }

    private void append(Line line) {
        if (size < lines.length) {
            lines[(start + size) % lines.length] = line;
            ++size;
        } else {
            lines[start] = line;
            start = (start + 1) % lines.length;
        }
    }

    public synchronized void clear() {
        for (int i = 0; i < lines.length; ++i)
            lines[i] = null;

        start = 0;
        size = 0;
        ++version;
    }

    public synchronized long version() {
        return version;
    }

    public synchronized int size() {
        return size;
    }

    public int capacity() {
        return lines.length;
    }

    // The last count lines (or less), oldest first
    public synchronized List<Line> last(int count) {
        int taken = Math.max(0, Math.min(count, size));
        List<Line> result = new ArrayList<>(taken);
        for (int i = size - taken; i < size; ++i)
            result.add(lines[(start + i) % lines.length]);

        return result;
    }

    // The lines whose id is greater than the given one, oldest first (the forgotten ones are lost)
    public synchronized List<Line> since(long id) {
        List<Line> result = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            Line line = lines[(start + i) % lines.length];
            if (line.id > id)
                result.add(line);
        }

        return result;
    }

    // Line at an index from the oldest kept one
    public synchronized Line get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Line " + index + " of " + size);

        return lines[(start + index) % lines.length];
    }

    /** Called for each added line, on the thread that adds it: must be short and never write to System.out. */
    public void addListener(Consumer<Line> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Line> listener) {
        listeners.remove(listener);
    }
}
