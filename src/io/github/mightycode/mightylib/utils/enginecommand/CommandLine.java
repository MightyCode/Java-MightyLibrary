package io.github.mightycode.mightylib.utils.enginecommand;

import java.util.ArrayList;

/**
 * Split a command line into words.
 * Words are separated by whitespace; double quotes group words ("screenshot \"a b.png\"" -> screenshot, a b.png).
 * Backslashes are kept as they are, so Windows paths can be written without escaping.
 */
public final class CommandLine {
    private CommandLine() {}

    /**
     * @throws IllegalArgumentException if a quote is not closed
     */
    public static String[] Split(String line) {
        ArrayList<String> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        // A word is started even if it is empty ("" gives an empty word)
        boolean inWord = false;

        for (int i = 0; i < line.length(); ++i) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
                inWord = true;
            } else if (!inQuotes && Character.isWhitespace(c)) {
                if (inWord) {
                    words.add(current.toString());
                    current.setLength(0);
                    inWord = false;
                }
            } else {
                current.append(c);
                inWord = true;
            }
        }

        if (inQuotes)
            throw new IllegalArgumentException("Quote not closed in ->" + line + "<-");

        if (inWord)
            words.add(current.toString());

        return words.toArray(new String[0]);
    }

    /** Words of the beginning of a line being typed: the last one may be unfinished (empty after a space). */
    public static final class Partial {
        private final String[] words;
        private final int lastStart;

        Partial(String[] words, int lastStart) {
            this.words = words;
            this.lastStart = lastStart;
        }

        // At least one word, the last one being the word being typed (without its quotes)
        public String[] getWords() { return words; }

        // Index in the line where the word being typed starts (its opening quote included)
        public int getLastStart() { return lastStart; }

        public String getLast() { return words[words.length - 1]; }
    }

    /** Split the text typed before the cursor, never fails (an unclosed quote belongs to the last word). */
    public static Partial SplitPartial(String beginning) {
        ArrayList<String> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean inWord = false;
        int wordStart = 0;

        for (int i = 0; i < beginning.length(); ++i) {
            char c = beginning.charAt(i);

            if (!inWord && !(Character.isWhitespace(c) && !inQuotes))
                wordStart = i;

            if (c == '"') {
                inQuotes = !inQuotes;
                inWord = true;
            } else if (!inQuotes && Character.isWhitespace(c)) {
                if (inWord) {
                    words.add(current.toString());
                    current.setLength(0);
                    inWord = false;
                }
                wordStart = i + 1;
            } else {
                current.append(c);
                inWord = true;
            }
        }

        // The word being typed, empty after a space
        words.add(current.toString());

        return new Partial(words.toArray(new String[0]), inWord ? wordStart : beginning.length());
    }

    // The word written so that Split gives it back (quoted when it holds whitespace or is empty)
    public static String Quote(String word) {
        boolean needsQuotes = word.isEmpty();
        for (int i = 0; i < word.length() && !needsQuotes; ++i)
            needsQuotes = Character.isWhitespace(word.charAt(i));

        return needsQuotes ? "\"" + word + "\"" : word;
    }
}
