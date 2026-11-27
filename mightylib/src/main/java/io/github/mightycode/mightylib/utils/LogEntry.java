package io.github.mightycode.mightylib.utils;

import java.time.Instant;
import java.util.Objects;

/** One problem sent through {@link Logger}: immutable, safe to keep and to read from any thread. */
public final class LogEntry {
    private final ELogLevel level;
    private final String category;
    private final String message;
    private final Throwable error;
    private final String thread;
    private final Instant time;

    public LogEntry(ELogLevel level, String category, String message, Throwable error, String thread, Instant time) {
        this.level = Objects.requireNonNull(level, "level");
        this.category = Objects.requireNonNull(category, "category");
        this.message = Objects.requireNonNull(message, "message");
        this.error = error;
        this.thread = Objects.requireNonNull(thread, "thread");
        this.time = Objects.requireNonNull(time, "time");
    }

    public ELogLevel level() {
        return level;
    }

    public String category() {
        return category;
    }

    /** The message without the "--" prefix of the printed line. */
    public String message() {
        return message;
    }

    /** The exception of the problem, null if none. */
    public Throwable error() {
        return error;
    }

    /** Name of the thread that sent the problem. */
    public String thread() {
        return thread;
    }

    public Instant time() {
        return time;
    }

    /** One line: level, category, message and exception if any. */
    @Override
    public String toString() {
        return level + " [" + category + "] " + message + (error != null ? ": " + error : "");
    }
}
