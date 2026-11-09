package io.github.mightycode.mightylib.utils.enginecommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Completion of the word being typed in a command line: where it starts and the words that can replace it.
 */
public final class CommandCompletion {
    private final int start;
    private final String typed;
    private final List<String> candidates;

    CommandCompletion(int start, String typed, List<String> candidates) {
        this.start = start;
        this.typed = typed;
        this.candidates = candidates;
    }

    // Index in the line where the replaced word starts
    public int getStart() { return start; }

    // The word typed (without quotes)
    public String getTyped() { return typed; }

    // Sorted, without duplicate
    public List<String> getCandidates() { return candidates; }

    public boolean isEmpty() { return candidates.isEmpty(); }

    public boolean isUnique() { return candidates.size() == 1; }

    // Longest beginning shared by the candidates (case of the first one), the typed word if none
    public String commonPrefix() {
        if (candidates.isEmpty())
            return typed;

        String prefix = candidates.get(0);
        for (String candidate : candidates) {
            int length = 0;
            int max = Math.min(prefix.length(), candidate.length());
            while (length < max && Character.toLowerCase(prefix.charAt(length)) == Character.toLowerCase(candidate.charAt(length)))
                ++length;

            prefix = prefix.substring(0, length);
        }

        return prefix.length() >= typed.length() ? prefix : typed;
    }

    /**
     * Text to write in place of the typed word: the only candidate followed by a space,
     * otherwise the common prefix (null when it adds nothing).
     */
    public String replacement() {
        if (isUnique()) {
            String only = candidates.get(0);
            // A folder is followed by what it contains, not by the next word
            if (only.endsWith("/"))
                return only.indexOf(' ') >= 0 ? "\"" + only : only;

            return CommandLine.Quote(only) + " ";
        }

        String prefix = commonPrefix();
        if (candidates.isEmpty() || prefix.equals(typed))
            return null;

        // An unfinished word with whitespace keeps its quote open
        for (int i = 0; i < prefix.length(); ++i) {
            if (Character.isWhitespace(prefix.charAt(i)))
                return "\"" + prefix;
        }

        return prefix;
    }

    /** The values starting with the prefix, case ignored, sorted. */
    public static List<String> Filter(String prefix, Collection<String> values) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        TreeSet<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(lower))
                sorted.add(value);
        }

        return new ArrayList<>(sorted);
    }
}
