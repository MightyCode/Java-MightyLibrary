package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.inputs.InputFocus;
import io.github.mightycode.mightylib.inputs.KeyEvent;
import io.github.mightycode.mightylib.inputs.text.CommandLineEditor;
import io.github.mightycode.mightylib.inputs.text.IClipboard;
import io.github.mightycode.mightylib.inputs.text.LineEditKeys;

import java.util.Collections;
import java.util.List;
import java.util.function.ToDoubleFunction;

import static org.lwjgl.glfw.GLFW.*;

/**
 * What a text field does with the inputs of a tick, without GL: takes the focus on a click inside, gives it back on
 * a click outside, Escape or Enter, edits the line with the keys the console shares (LineEditKeys), and keeps the
 * part of the line shown around the cursor. The TextField draws it.
 */
public final class TextFieldModel {
    /** The inputs of one tick, read raw (what the owner of the focus reads). */
    static final class Tick {
        String typedText = "";
        List<KeyEvent> keys = Collections.emptyList();
        IClipboard clipboard;
        // The left button of the mouse
        boolean mousePressed;
        boolean mouseHeld;
        boolean shift;
        // The cursor is over the field
        boolean mouseInside;
        // Pixels from the left of the text area
        float mouseX;
    }

    private final Object owner;
    private final CommandLineEditor editor = new CommandLineEditor(1);
    private final ToDoubleFunction<CharSequence> width;

    private boolean changed;
    private boolean submitted;
    private int leftWith = -1;
    private boolean dragging;

    // First character shown (the line scrolls around the cursor) and width of the text area
    private int scroll;
    private float available = 1;

    /**
     * @param owner the object that holds the focus of the window (the field)
     * @param width width of a text in the font of the field, linear in its length
     */
    public TextFieldModel(Object owner, ToDoubleFunction<CharSequence> width) {
        this.owner = owner;
        this.width = width;
    }

    public CommandLineEditor editor() { return editor; }

    public String getText() { return editor.getText(); }

    /** Replaces the text (cursor at the end); not told as a change. */
    public void setText(String text) {
        editor.setText(text == null ? "" : text);
        scroll = 0;
    }

    /** True at the tick the text changed (typed, deleted, pasted). */
    public boolean isChanged() { return changed; }

    /** True at the tick Enter was pressed in the field. */
    public boolean isSubmitted() { return submitted; }

    /**
     * The key (GLFW_KEY_ESCAPE, ENTER, KP_ENTER, UP, DOWN) that gave the focus back at this tick, -1 if none (a click
     * elsewhere is not a key): the scene decides what Down or Escape mean.
     */
    public int getLeftWith() { return leftWith; }

    public boolean hasFocus(InputFocus focus) { return focus.isCurrent(owner); }

    public void setAvailableWidth(float available) { this.available = Math.max(1, available); }

    public void update(InputFocus focus, Tick tick) {
        changed = false;
        submitted = false;
        leftWith = -1;

        boolean focusedAtStart = focus.isCurrent(owner);
        if (tick.mousePressed)
            press(focus, tick);

        if (!tick.mouseHeld)
            dragging = false;
        else if (dragging && !tick.mousePressed && focus.isCurrent(owner))
            editor.moveTo(indexAt(tick.mouseX), true);

        // What was typed before the focus came is not for the field (InputFocus)
        if (!focusedAtStart || !focus.isCurrent(owner))
            return;

        String before = editor.getText();
        editor.insert(tick.typedText);
        for (KeyEvent key : tick.keys) {
            boolean leaving = IsLeavingKey(key.key());
            // A held Escape or Enter repeats: only the first one counts
            if (leaving && key.isRepeat())
                continue;

            if (leaving) {
                submitted = key.key() == GLFW_KEY_ENTER || key.key() == GLFW_KEY_KP_ENTER;
                leftWith = key.key();
                focus.release(owner);
                break;
            }

            if (!LineEditKeys.Handle(editor, key))
                LineEditKeys.HandleShortcut(editor, key, tick.clipboard);
        }

        changed = !before.equals(editor.getText());
        keepCursorVisible();
    }

    // The keys that give the focus back: Escape, Enter, and Up and Down (a one-line field has no use for them)
    private static boolean IsLeavingKey(int key) {
        return key == GLFW_KEY_ESCAPE || key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER
                || key == GLFW_KEY_UP || key == GLFW_KEY_DOWN;
    }

    private void press(InputFocus focus, Tick tick) {
        if (tick.mouseInside) {
            focus.request(owner);
            editor.moveTo(indexAt(tick.mouseX), tick.shift);
            dragging = true;
        } else if (focus.isRequested(owner)) {
            // The click is still for what is under the cursor
            focus.releaseForClick(owner);
            dragging = false;
        }
    }

    // ---- What is shown ----

    /** First character of the line shown. */
    public int visibleStart() { return scroll; }

    /** After the last character of the line shown. */
    public int visibleEnd() {
        String text = editor.getText();
        int end = scroll;
        float used = 0;
        while (end < text.length()) {
            float advance = (float) width.applyAsDouble(text.subSequence(end, end + 1));
            if (used + advance > available)
                break;
            used += advance;
            ++end;
        }
        return end;
    }

    /** Pixels from the left of the text area to the character of the line. */
    public float xOf(int index) {
        String text = editor.getText();
        int clamped = Math.max(scroll, Math.min(index, text.length()));
        return (float) width.applyAsDouble(text.subSequence(scroll, clamped));
    }

    // The index of the line under x (pixels from the left of the text area), the nearest one
    int indexAt(float x) {
        String text = editor.getText();
        int index = scroll;
        float position = 0;
        while (index < text.length()) {
            float advance = (float) width.applyAsDouble(text.subSequence(index, index + 1));
            if (x < position + advance * 0.5f)
                break;
            position += advance;
            ++index;
        }
        return index;
    }

    // The line scrolls so that the cursor stays in the text area
    private void keepCursorVisible() {
        String text = editor.getText();
        int cursor = editor.getCursor();
        scroll = Math.min(scroll, Math.min(cursor, text.length()));
        while (scroll < cursor && width.applyAsDouble(text.subSequence(scroll, cursor)) > available)
            ++scroll;
    }
}
