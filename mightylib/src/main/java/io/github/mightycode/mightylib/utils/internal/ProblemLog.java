package io.github.mightycode.mightylib.utils.internal;

import io.github.mightycode.mightylib.utils.ELogLevel;
import io.github.mightycode.mightylib.utils.ILogListener;
import io.github.mightycode.mightylib.utils.LogEntry;
import io.github.mightycode.mightylib.utils.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The problems the engine received (command {@code problems}): the last ones in a ring, and their counts by level and
 * by category since the start or the last {@link #clear()}. Fed by the {@link Logger} channel from any thread.
 * Guarded by the instance: synchronize on it to read several values of one state.
 */
public final class ProblemLog implements ILogListener {
    public static final int DEFAULT_CAPACITY = 200;

    private final int capacity;
    private final ArrayDeque<LogEntry> last;
    private final EnumMap<ELogLevel, Long> levels = new EnumMap<>(ELogLevel.class);
    // Category, then level
    private final TreeMap<String, EnumMap<ELogLevel, Long>> categories = new TreeMap<>();
    private boolean installed;

    public ProblemLog() {
        this(DEFAULT_CAPACITY);
    }

    public ProblemLog(int capacity) {
        if (capacity < 1)
            throw new IllegalArgumentException("The capacity of the problem log must be at least 1: " + capacity);
        this.capacity = capacity;
        last = new ArrayDeque<>(capacity);
    }

    /** Listens to the channel of {@link Logger} until {@link #uninstall()}; twice does nothing. */
    public synchronized void install() {
        if (installed)
            return;
        installed = true;
        Logger.AddListener(this);
    }

    public synchronized void uninstall() {
        if (!installed)
            return;
        installed = false;
        Logger.RemoveListener(this);
    }

    @Override
    public synchronized void onLog(LogEntry entry) {
        if (last.size() == capacity)
            last.removeFirst();
        last.addLast(entry);

        levels.merge(entry.level(), 1L, Long::sum);
        categories.computeIfAbsent(entry.category(), category -> new EnumMap<>(ELogLevel.class))
                .merge(entry.level(), 1L, Long::sum);
    }

    public int capacity() {
        return capacity;
    }

    /** Problems of this level received since the start or the last clear (kept or not by the ring). */
    public synchronized long count(ELogLevel level) {
        return levels.getOrDefault(level, 0L);
    }

    /** Counts by category, then by level (levels without problem absent), sorted by category. */
    public synchronized Map<String, Map<ELogLevel, Long>> categories() {
        Map<String, Map<ELogLevel, Long>> copy = new TreeMap<>();
        for (Map.Entry<String, EnumMap<ELogLevel, Long>> category : categories.entrySet())
            copy.put(category.getKey(), Collections.unmodifiableMap(new EnumMap<>(category.getValue())));
        return Collections.unmodifiableMap(copy);
    }

    /** The last problems kept by the ring, oldest first: at most count, of this level (null for every level). */
    public synchronized List<LogEntry> last(ELogLevel level, int count) {
        List<LogEntry> chosen = new ArrayList<>(Math.min(count, last.size()));
        Iterator<LogEntry> newestFirst = last.descendingIterator();
        while (newestFirst.hasNext() && chosen.size() < count) {
            LogEntry entry = newestFirst.next();
            if (level == null || entry.level() == level)
                chosen.add(entry);
        }
        Collections.reverse(chosen);
        return chosen;
    }

    /** Number of problems the ring keeps. */
    public synchronized int size() {
        return last.size();
    }

    public synchronized void clear() {
        last.clear();
        levels.clear();
        categories.clear();
    }
}
