package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.utils.ELogLevel;
import io.github.mightycode.mightylib.utils.ILogListener;
import io.github.mightycode.mightylib.utils.LogEntry;
import io.github.mightycode.mightylib.utils.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The problems of the Logger channel a test engine receives, test by test, and the problems the test announces.
 * Fed from any thread (main thread, workers, OpenGL driver); read and reset by the thread of the test.
 */
final class ProblemRecorder implements ILogListener {
    // An error without exception: where it was sent from (the listener runs on the thread that sent it)
    private static final class Received {
        final LogEntry entry;
        final Throwable where;

        Received(LogEntry entry, Throwable where) {
            this.entry = entry;
            this.where = where;
        }

        Throwable cause() {
            return entry.error() != null ? entry.error() : where;
        }
    }

    private static final class Expected {
        final ELogLevel level;
        final Pattern pattern;
        boolean met;

        Expected(ELogLevel level, Pattern pattern) {
            this.level = level;
            this.pattern = pattern;
        }

        boolean matches(LogEntry entry) {
            return entry.level() == level && (pattern.matcher(entry.message()).find()
                    || entry.error() != null && pattern.matcher(entry.error().toString()).find());
        }

        @Override
        public String toString() {
            return level + " matching \"" + pattern.pattern() + "\"";
        }
    }

    // Guarded by this
    private final List<Received> received = new ArrayList<>();
    private final List<Expected> expected = new ArrayList<>();
    private boolean installed;

    synchronized void install() {
        if (installed)
            return;
        installed = true;
        Logger.AddListener(this);
    }

    synchronized void uninstall() {
        if (!installed)
            return;
        installed = false;
        Logger.RemoveListener(this);
    }

    @Override
    public void onLog(LogEntry entry) {
        Throwable where = entry.error() == null && entry.level() == ELogLevel.ERROR
                ? new Throwable("Sent through Logger here (thread " + entry.thread() + ")") : null;
        synchronized (this) {
            received.add(new Received(entry, where));
        }
    }

    synchronized void expect(ELogLevel level, String regex) {
        expected.add(new Expected(Objects.requireNonNull(level, "level"),
                Pattern.compile(Objects.requireNonNull(regex, "regex"))));
    }

    /** Everything received since the last reset, warnings included, oldest first. */
    synchronized List<LogEntry> problems() {
        List<LogEntry> entries = new ArrayList<>(received.size());
        for (Received problem : received)
            entries.add(problem.entry);
        return entries;
    }

    /** Forgets what was received and announced: a new test begins. */
    synchronized void reset() {
        received.clear();
        expected.clear();
    }

    /**
     * The failure of what was received since the last reset, null if none: an error no announce covers, or (with
     * checkAnnounced) an announce no problem met. An announce covers every problem it matches. The message names
     * each error (the cause of the failure is the stack trace of the first one) and lists the warnings.
     */
    synchronized AssertionError failure(boolean checkAnnounced) {
        for (Expected announce : expected)
            announce.met = false;

        List<Received> unexpected = new ArrayList<>();
        List<LogEntry> warnings = new ArrayList<>();
        for (Received problem : received) {
            boolean covered = false;
            for (Expected announce : expected) {
                if (announce.matches(problem.entry)) {
                    announce.met = true;
                    covered = true;
                }
            }
            if (!covered && problem.entry.level() == ELogLevel.ERROR)
                unexpected.add(problem);
            else if (problem.entry.level() == ELogLevel.WARNING)
                warnings.add(problem.entry);
        }

        List<Expected> unmet = new ArrayList<>();
        if (checkAnnounced) {
            for (Expected announce : expected) {
                if (!announce.met)
                    unmet.add(announce);
            }
        }
        if (unexpected.isEmpty() && unmet.isEmpty())
            return null;

        StringBuilder message = new StringBuilder();
        if (!unexpected.isEmpty()) {
            message.append(unexpected.size()).append(" unexpected error(s) sent through Logger (announce a wanted one"
                    + " with TestEngine.expectProblem):");
            List<String> lines = new ArrayList<>();
            for (Received problem : unexpected)
                lines.add(problem.entry + " (thread " + problem.entry.thread() + ")");
            AppendCounted(message, lines);
        }
        if (!unmet.isEmpty()) {
            if (message.length() > 0)
                message.append("\n");
            message.append("Announced problem(s) that never came:");
            for (Expected announce : unmet)
                message.append("\n  ").append(announce);
        }
        if (!warnings.isEmpty()) {
            message.append("\nWarnings (").append(warnings.size()).append("):");
            List<String> lines = new ArrayList<>();
            for (LogEntry warning : warnings)
                lines.add(warning.toString());
            AppendCounted(message, lines);
        }

        return new AssertionError(message.toString(), unexpected.isEmpty() ? null : unexpected.get(0).cause());
    }

    // One line per distinct text, in the order of their first time, with their number when repeated (each frame)
    private static void AppendCounted(StringBuilder message, List<String> lines) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String line : lines)
            counts.merge(line, 1, Integer::sum);
        for (Map.Entry<String, Integer> line : counts.entrySet())
            message.append("\n  ").append(line.getKey()).append(line.getValue() > 1 ? " (x" + line.getValue() + ")" : "");
    }
}
