package io.github.mightycode.mightylib.console;

import java.util.ArrayList;
import java.util.List;

/**
 * Text of a command line being typed: cursor, selection, words, history. No input reading, no drawing.
 * The cursor is an index between characters, from 0 (before the first one) to the length (after the last one).
 * The selection goes from its anchor to the cursor: moving with Shift extends it, moving without removes it, typing or
 * pasting replaces it, deleting removes its text.
 */
public final class CommandLineEditor {
    public static final int DEFAULT_HISTORY_SIZE = 100;
    private static final int NO_SELECTION = -1;

    private final StringBuilder text;
    private int cursor;
    // Other end of the selection, NO_SELECTION if none
    private int selectionAnchor = NO_SELECTION;

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
        clearSelection();
    }

    // ---- Selection ----

    public boolean hasSelection() {
        return selectionAnchor != NO_SELECTION && selectionAnchor != cursor;
    }

    // First character selected (the cursor when nothing is selected)
    public int selectionStart() {
        return hasSelection() ? Math.min(selectionAnchor, cursor) : cursor;
    }

    // After the last character selected (the cursor when nothing is selected)
    public int selectionEnd() {
        return hasSelection() ? Math.max(selectionAnchor, cursor) : cursor;
    }

    public String selectedText() {
        return text.substring(selectionStart(), selectionEnd());
    }

    public boolean isAllSelected() {
        return hasSelection() && selectionStart() == 0 && selectionEnd() == text.length();
    }

    public void clearSelection() {
        selectionAnchor = NO_SELECTION;
    }

    public void selectAll() {
        selectionAnchor = 0;
        cursor = text.length();
    }

    /** Select from start to end; the cursor goes to end. */
    public void select(int start, int end) {
        selectionAnchor = clamp(start);
        cursor = clamp(end);
    }

    /** Remove the selected text: the text that was selected. */
    public String cutSelection() {
        String cut = selectedText();
        deleteSelection();
        return cut;
    }

    private boolean deleteSelection() {
        if (!hasSelection()) {
            clearSelection();
            return false;
        }

        int start = selectionStart();
        text.delete(start, selectionEnd());
        cursor = start;
        clearSelection();
        return true;
    }

    // Before a move: with extend the selection starts at the cursor (if there is none), otherwise it is removed
    private void prepareMove(boolean extend) {
        if (!extend)
            clearSelection();
        else if (selectionAnchor == NO_SELECTION)
            selectionAnchor = cursor;
    }

    private int clamp(int position) {
        return Math.max(0, Math.min(text.length(), position));
    }

    // ---- Edition ----

    public void insert(String inserted) {
        if (inserted == null || inserted.isEmpty())
            return;

        deleteSelection();
        // A pasted text keeps one line
        String clean = inserted.replace("\r", "").replace('\n', ' ').replace('\t', ' ');
        text.insert(cursor, clean);
        cursor += clean.length();
    }

    /**
     * Paste a text that may have several lines (\r\n and \r count as \n): the text becomes the part before the
     * selection, the pasted text, then the part after it. Every complete line (followed by a line break) is validated
     * (submit: in the history), the last one stays in the editor, the cursor after the pasted part.
     * @return the validated lines that are not blank, in their order
     */
    public List<String> paste(String pasted) {
        List<String> validated = new ArrayList<>();
        if (pasted == null || pasted.isEmpty())
            return validated;

        String normalized = pasted.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.indexOf('\n') < 0) {
            insert(normalized);
            return validated;
        }

        deleteSelection();
        String after = text.substring(cursor);
        String whole = text.substring(0, cursor) + normalized.replace('\t', ' ') + after;
        String[] lines = whole.split("\n", -1);
        for (int i = 0; i < lines.length - 1; ++i) {
            text.setLength(0);
            text.append(lines[i]);
            String line = submit();
            if (!line.trim().isEmpty())
                validated.add(line);
        }

        setText(lines[lines.length - 1]);
        cursor = text.length() - after.length();
        return validated;
    }

    public void deleteLeft() {
        if (deleteSelection() || cursor == 0)
            return;

        text.deleteCharAt(cursor - 1);
        --cursor;
    }

    public void deleteRight() {
        if (deleteSelection())
            return;

        if (cursor < text.length())
            text.deleteCharAt(cursor);
    }

    public void deleteWordLeft() {
        if (deleteSelection())
            return;

        int start = wordLeft();
        text.delete(start, cursor);
        cursor = start;
    }

    // ---- Moves (extend: with Shift, the selection follows the cursor) ----

    public void left() { left(false); }

    public void left(boolean extend) {
        // Without Shift, a selection is left from its beginning
        if (!extend && hasSelection()) {
            cursor = selectionStart();
            clearSelection();
            return;
        }

        prepareMove(extend);
        if (cursor > 0)
            --cursor;
    }

    public void right() { right(false); }

    public void right(boolean extend) {
        if (!extend && hasSelection()) {
            cursor = selectionEnd();
            clearSelection();
            return;
        }

        prepareMove(extend);
        if (cursor < text.length())
            ++cursor;
    }

    public void home() { home(false); }

    public void home(boolean extend) {
        prepareMove(extend);
        cursor = 0;
    }

    public void end() { end(false); }

    public void end(boolean extend) {
        prepareMove(extend);
        cursor = text.length();
    }

    public void wordLeftMove() { wordLeftMove(false); }

    public void wordLeftMove(boolean extend) {
        prepareMove(extend);
        cursor = wordLeft();
    }

    public void wordRightMove() { wordRightMove(false); }

    public void wordRightMove(boolean extend) {
        prepareMove(extend);
        cursor = wordRight();
    }

    /** Put the cursor at a place (a click), extending the selection with extend (Shift+click). */
    public void moveTo(int position, boolean extend) {
        prepareMove(extend);
        cursor = clamp(position);
    }

    // Start of the word at the left of the cursor (spaces skipped first)
    private int wordLeft() {
        return WordLeft(text, cursor);
    }

    // End of the word at the right of the cursor (spaces skipped first)
    private int wordRight() {
        return WordRight(text, cursor);
    }

    // Start of the word at the left of the position (separators skipped first)
    static int WordLeft(CharSequence text, int position) {
        while (position > 0 && IsSeparator(text.charAt(position - 1)))
            --position;
        while (position > 0 && !IsSeparator(text.charAt(position - 1)))
            --position;

        return position;
    }

    // End of the word at the right of the position (separators skipped first)
    static int WordRight(CharSequence text, int position) {
        while (position < text.length() && IsSeparator(text.charAt(position)))
            ++position;
        while (position < text.length() && !IsSeparator(text.charAt(position)))
            ++position;

        return position;
    }

    // The words are separated by these characters (Ctrl+Left/Right, double click)
    static boolean IsSeparator(char c) {
        return Character.isWhitespace(c);
    }

    /** Replace the characters from start to the cursor (the word being completed) by the given text. */
    public void replaceBeforeCursor(int start, String replacement) {
        if (start < 0 || start > cursor)
            throw new IllegalArgumentException("Start " + start + " after the cursor " + cursor);

        clearSelection();
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
        clearSelection();
        historyIndex = history.size();
        draft = "";

        return line;
    }

    public void previous() {
        clearSelection();
        if (historyIndex == 0)
            return;

        if (historyIndex == history.size())
            draft = text.toString();

        --historyIndex;
        setText(history.get(historyIndex));
    }

    public void next() {
        clearSelection();
        if (historyIndex >= history.size())
            return;

        ++historyIndex;
        setText(historyIndex == history.size() ? draft : history.get(historyIndex));
    }

    public List<String> getHistory() {
        return new ArrayList<>(history);
    }
}
