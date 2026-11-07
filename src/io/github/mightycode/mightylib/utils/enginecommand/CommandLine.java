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
}
