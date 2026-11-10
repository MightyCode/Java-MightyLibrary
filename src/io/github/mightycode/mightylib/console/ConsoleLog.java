package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;

import java.util.ArrayList;
import java.util.Collections;
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
        // Program output (System.out, System.err): for the developer only
        OUT, ERR,
        COMMAND, RESULT, ERROR,
        // Written by the game for the player (Console.print)
        MESSAGE
    }

    public static final class Line {
        private final long id;
        private final EType type;
        private final String source;
        private final String text;
        private final ECommandLevel level;

        Line(long id, EType type, String source, String text, ECommandLevel level) {
            this.id = id;
            this.type = type;
            this.source = source;
            this.text = text;
            this.level = level;
        }

        // Increasing from 1, never reused
        public long getId() { return id; }
        public EType getType() { return type; }
        // The lowest level that sees the line
        public ECommandLevel getLevel() { return level; }
        public boolean isVisible(ECommandLevel viewer) { return viewer.includes(level); }
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
    // Per viewer level: changes only when what this level sees changes (a hidden line changes nothing)
    private final long[] levelVersions = new long[ECommandLevel.values().length];
    // Per viewer level: the lines up to this id were cleared for it (clear by a level below DEV)
    private final long[] clearedUpTo = new long[ECommandLevel.values().length];

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

    /** Add one entry, split on the line breaks: the program output is seen by DEV, the rest by every level. */
    public void add(EType type, String source, String text) {
        add(type, source, text, type == EType.OUT || type == EType.ERR ? ECommandLevel.DEV : ECommandLevel.USER);
    }

    /** Add one entry, split on the line breaks, seen from the level on. Gives the lines added. */
    public List<Line> add(EType type, String source, String text, ECommandLevel level) {
        if (text == null)
            return Collections.emptyList();

        String[] split = text.split("\r?\n", -1);
        int count = split.length;
        // A final line break does not add an empty line
        if (count > 1 && split[count - 1].isEmpty())
            --count;

        List<Line> added = new ArrayList<>(count);
        synchronized (this) {
            for (int i = 0; i < count; ++i) {
                Line line = new Line(nextId++, type, source, split[i], level);
                append(line);
                added.add(line);
            }
            ++version;
            for (ECommandLevel viewer : ECommandLevel.values()) {
                if (viewer.includes(level))
                    ++levelVersions[viewer.ordinal()];
            }
        }

        for (Line line : added) {
            for (Consumer<Line> listener : listeners)
                listener.accept(line);
        }

        return Collections.unmodifiableList(added);
    }

    /**
     * Let lower levels see lines already written (the command that lowered the level of its source, cheats off, and
     * its answer stay seen by the source). Never raises a level; the forgotten lines are ignored.
     */
    public synchronized void lower(List<Line> written, ECommandLevel level) {
        boolean changed = false;
        for (Line line : written) {
            for (int i = size - 1; i >= 0; --i) {
                int index = (start + i) % lines.length;
                Line current = lines[index];
                if (current.id != line.id)
                    continue;

                if (!level.includes(current.level)) {
                    lines[index] = new Line(current.id, current.type, current.source, current.text, level);
                    for (ECommandLevel viewer : ECommandLevel.values()) {
                        if (viewer.includes(level) && !viewer.includes(current.level))
                            ++levelVersions[viewer.ordinal()];
                    }
                    changed = true;
                }
                break;
            }
        }

        if (changed)
            ++version;
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

    // Forget every line: for every viewer, the debug server clients included
    public synchronized void clear() {
        for (int i = 0; i < lines.length; ++i)
            lines[i] = null;

        start = 0;
        size = 0;
        ++version;
        for (int i = 0; i < levelVersions.length; ++i)
            ++levelVersions[i];
    }

    /**
     * Clear asked by a viewer: DEV forgets every line; a lower level only stops seeing the current lines, it and the
     * levels below (the player's console), the developer still reads them (console log, debug server).
     */
    public synchronized void clear(ECommandLevel viewer) {
        if (viewer == ECommandLevel.DEV) {
            clear();
            return;
        }

        for (ECommandLevel level : ECommandLevel.values()) {
            if (viewer.includes(level)) {
                clearedUpTo[level.ordinal()] = nextId - 1;
                ++levelVersions[level.ordinal()];
            }
        }
        ++version;
    }

    public synchronized long version() {
        return version;
    }

    // Changes only when what the viewer sees changes
    public synchronized long version(ECommandLevel viewer) {
        return levelVersions[viewer.ordinal()];
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

    // The last count lines (or less) the level sees, oldest first, without the ones it cleared
    public synchronized List<Line> last(int count, ECommandLevel viewer) {
        long cleared = clearedUpTo[viewer.ordinal()];
        List<Line> fromNewest = new ArrayList<>();
        for (int i = size - 1; i >= 0 && fromNewest.size() < count; --i) {
            Line line = lines[(start + i) % lines.length];
            if (line.id <= cleared)
                break;
            if (line.isVisible(viewer))
                fromNewest.add(line);
        }

        Collections.reverse(fromNewest);
        return fromNewest;
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
