package io.github.mightycode.mightylib.console;

import java.util.ArrayList;
import java.util.List;

/**
 * Text of a command line being typed: cursor, words, history. No input reading, no drawing.
 * The cursor is an index between characters, from 0 (before the first one) to the length (after the last one).
 */
public final class CommandLineEditor {
    public static final int DEFAULT_HISTORY_SIZE = 100;

    private final StringBuilder text;
    private int cursor;

    private final int historySize;
    private final ArrayList<String> history;
    // history.size() : the line being typed (the draft), otherwise an index in the history
    private int historyIndex;
    private String draft;

    public CommandLineEditor() {
        this(DEFAULT_HISTORY_SIZE);
    }

    public CommandLineEditor(int historySize) {
        this.historySize = Math.max(1, historySize);
        text = new StringBuilder();
        history = new ArrayList<>();
        draft = "";
    }

    public String getText() { return text.toString(); }

    public int getCursor() { return cursor; }

    public void setText(String newText) {
        text.setLength(0);
        text.append(newText);
        cursor = text.length();
    }

    public void insert(String inserted) {
        if (inserted == null || inserted.isEmpty())
            return;

        // A pasted text keeps one line
        String clean = inserted.replace("\r", "").replace('\n', ' ').replace('\t', ' ');
        text.insert(cursor, clean);
        cursor += clean.length();
    }

    public void deleteLeft() {
        if (cursor == 0)
            return;

        text.deleteCharAt(cursor - 1);
        --cursor;
    }

    public void deleteRight() {
        if (cursor < text.length())
            text.deleteCharAt(cursor);
    }

    public void deleteWordLeft() {
        int start = wordLeft();
        text.delete(start, cursor);
        cursor = start;
    }

    public void left() {
        if (cursor > 0)
            --cursor;
    }

    public void right() {
        if (cursor < text.length())
            ++cursor;
    }

    public void home() { cursor = 0; }

    public void end() { cursor = text.length(); }

    public void wordLeftMove() { cursor = wordLeft(); }

    public void wordRightMove() { cursor = wordRight(); }

    // Start of the word at the left of the cursor (spaces skipped first)
    private int wordLeft() {
        int position = cursor;
        while (position > 0 && isSeparator(text.charAt(position - 1)))
            --position;
        while (position > 0 && !isSeparator(text.charAt(position - 1)))
            --position;

        return position;
    }

    // End of the word at the right of the cursor (spaces skipped first)
    private int wordRight() {
        int position = cursor;
        while (position < text.length() && isSeparator(text.charAt(position)))
            ++position;
        while (position < text.length() && !isSeparator(text.charAt(position)))
            ++position;

        return position;
    }

    private static boolean isSeparator(char c) {
        return Character.isWhitespace(c);
    }

    /** Replace the characters from start to the cursor (the word being completed) by the given text. */
    public void replaceBeforeCursor(int start, String replacement) {
        if (start < 0 || start > cursor)
            throw new IllegalArgumentException("Start " + start + " after the cursor " + cursor);

        text.replace(start, cursor, replacement);
        cursor = start + replacement.length();
    }

    // ---- History ----

    /**
     * The line is validated: kept in the history (unless empty or equal to the previous one) and the editor is emptied.
     * @return the validated line
     */
    public String submit() {
        String line = text.toString();
        if (!line.trim().isEmpty() && (history.isEmpty() || !history.get(history.size() - 1).equals(line))) {
            history.add(line);
            if (history.size() > historySize)
                history.remove(0);
        }

        text.setLength(0);
        cursor = 0;
        historyIndex = history.size();
        draft = "";

        return line;
    }

    public void previous() {
        if (historyIndex == 0)
            return;

        if (historyIndex == history.size())
            draft = text.toString();

        --historyIndex;
        setText(history.get(historyIndex));
    }

    public void next() {
        if (historyIndex >= history.size())
            return;

        ++historyIndex;
        setText(historyIndex == history.size() ? draft : history.get(historyIndex));
    }

    public List<String> getHistory() {
        return new ArrayList<>(history);
    }
}
