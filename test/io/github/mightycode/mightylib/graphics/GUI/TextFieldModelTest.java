package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.inputs.InputFocus;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.KeyEvent;
import io.github.mightycode.mightylib.inputs.text.IClipboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

// The inputs of a text field without GL: ten units of width per character
class TextFieldModelTest {
    private static final float CHARACTER = 10;

    private static final class Clipboard implements IClipboard {
        String content = "";

        @Override
        public String get() { return content; }

        @Override
        public void set(String text) { content = text; }
    }

    private InputFocus focus;
    private TextFieldModel model;
    private final Clipboard clipboard = new Clipboard();

    @BeforeEach
    void setUp() {
        focus = new InputManager(null, null, null).focus();
        model = new TextFieldModel("field", text -> text.length() * CHARACTER);
        model.setAvailableWidth(1000);
    }

    private static KeyEvent key(int key) {
        return new KeyEvent(key, 0, 0, false, (char) 0);
    }

    private static KeyEvent key(int key, int mods) {
        return new KeyEvent(key, 0, mods, false, (char) 0);
    }

    private static KeyEvent ctrl(char letter) {
        return new KeyEvent(0, 0, GLFW_MOD_CONTROL, false, letter);
    }

    private TextFieldModel.Tick tick(String typed, KeyEvent... keys) {
        TextFieldModel.Tick tick = new TextFieldModel.Tick();
        tick.typedText = typed;
        tick.keys = List.of(keys);
        tick.clipboard = clipboard;
        return tick;
    }

    // Time of the clicks: a second apart by default, never a double click
    private long now;

    private TextFieldModel.Tick click(float mouseX) {
        return clickAfter(mouseX, 1_000_000_000L);
    }

    // A click soon after the previous one: a double or triple click at the same place
    private TextFieldModel.Tick quickClick(float mouseX) {
        return clickAfter(mouseX, 100_000_000L);
    }

    private TextFieldModel.Tick clickAfter(float mouseX, long nanoseconds) {
        now += nanoseconds;
        TextFieldModel.Tick tick = tick("");
        tick.mousePressed = true;
        tick.mouseHeld = true;
        tick.mouseInside = true;
        tick.mouseX = mouseX;
        tick.nanoTime = now;
        return tick;
    }

    // The field has the focus, as after a click
    private void focused() {
        model.update(focus, click(0));
        assertTrue(model.hasFocus(focus));
    }

    @Test
    void typedTextIsIgnoredWithoutTheFocus() {
        model.update(focus, tick("abc"));

        assertEquals("", model.getText());
        assertFalse(model.isChanged());
    }

    @Test
    void clickInsideTakesTheFocusButNotWhatWasTypedInThatTick() {
        TextFieldModel.Tick tick = click(0);
        tick.typedText = "abc";
        tick.keys = List.of(key(GLFW_KEY_BACKSPACE));
        model.update(focus, tick);

        assertTrue(model.hasFocus(focus));
        assertEquals("", model.getText());
        assertFalse(model.isChanged());

        model.update(focus, tick("abc"));
        assertEquals("abc", model.getText());
        assertTrue(model.isChanged());
    }

    @Test
    void textAndKeysOfATickAreAppliedInTheOrderTheyCame() {
        focused();
        // "a", Backspace, "b" in one tick
        model.update(focus, tick("ab", new KeyEvent(GLFW_KEY_BACKSPACE, 0, 0, false, (char) 0, 1)));

        assertEquals("b", model.getText());
        assertTrue(model.isChanged());
    }

    @Test
    void textTypedAfterTheKeyThatLeavesIsLost() {
        focused();
        // "a", Escape, "b": the "b" is typed once the field no longer has the focus
        model.update(focus, tick("ab", new KeyEvent(GLFW_KEY_ESCAPE, 0, 0, false, (char) 0, 1)));

        assertEquals("a", model.getText());
        assertFalse(model.hasFocus(focus));
    }

    @Test
    void changedLastsOneTick() {
        focused();
        model.update(focus, tick("a"));
        assertTrue(model.isChanged());

        model.update(focus, tick(""));
        assertFalse(model.isChanged());
    }

    @Test
    void clickOutsideGivesTheFocusBack() {
        focused();
        TextFieldModel.Tick tick = tick("");
        tick.mousePressed = true;
        tick.mouseHeld = true;
        tick.mouseInside = false;
        model.update(focus, tick);

        assertFalse(model.hasFocus(focus));
        assertEquals(-1, model.getLeftWith());
        assertFalse(focus.isHeld());
    }

    @Test
    void clickOutsideWithoutTheFocusChangesNothing() {
        TextFieldModel.Tick tick = tick("");
        tick.mousePressed = true;
        tick.mouseInside = false;
        model.update(focus, tick);

        assertFalse(focus.isHeld());
    }

    @Test
    void escapeGivesTheFocusBack() {
        focused();
        model.update(focus, tick("", key(GLFW_KEY_ESCAPE)));

        assertFalse(model.hasFocus(focus));
        assertFalse(focus.isHeld());
        assertFalse(model.isSubmitted());
        assertEquals(GLFW_KEY_ESCAPE, model.getLeftWith());
    }

    @Test
    void enterSubmitsAndGivesTheFocusBack() {
        focused();
        model.update(focus, tick("abc", key(GLFW_KEY_ENTER)));

        assertTrue(model.isSubmitted());
        assertEquals("abc", model.getText());
        assertFalse(model.hasFocus(focus));
        assertEquals(GLFW_KEY_ENTER, model.getLeftWith());

        // One tick only
        model.update(focus, tick(""));
        assertFalse(model.isSubmitted());
        assertEquals(-1, model.getLeftWith());
    }

    @Test
    void upAndDownGiveTheFocusBackAndSaySo() {
        focused();
        model.update(focus, tick("", key(GLFW_KEY_DOWN)));
        assertFalse(model.hasFocus(focus));
        assertFalse(model.isSubmitted());
        assertEquals(GLFW_KEY_DOWN, model.getLeftWith());

        focused();
        model.update(focus, tick("", key(GLFW_KEY_UP)));
        assertEquals(GLFW_KEY_UP, model.getLeftWith());
    }

    @Test
    void aHeldEscapeCountsOnce() {
        focused();
        model.update(focus, tick("", new KeyEvent(GLFW_KEY_ESCAPE, 0, 0, true, (char) 0)));

        assertTrue(model.hasFocus(focus));
    }

    @Test
    void keysAfterTheOneThatLeavesAreNotApplied() {
        focused();
        model.update(focus, tick("ab", key(GLFW_KEY_ENTER), key(GLFW_KEY_BACKSPACE)));

        assertEquals("ab", model.getText());
    }

    @Test
    void theOwnerAboveTheFieldTakesTheKeys() {
        focused();
        focus.request("console");
        model.update(focus, tick("abc", key(GLFW_KEY_ESCAPE)));

        assertEquals("", model.getText());
        assertTrue(focus.isRequested("field"));

        // The console closes: the focus is back to the field
        focus.release("console");
        model.update(focus, tick("x"));
        assertEquals("x", model.getText());
    }

    @Test
    void editKeysAreTheOnesOfTheConsole() {
        focused();
        model.update(focus, tick("hello world"));

        model.update(focus, tick("", key(GLFW_KEY_BACKSPACE)));
        assertEquals("hello worl", model.getText());

        model.update(focus, tick("", key(GLFW_KEY_BACKSPACE, GLFW_MOD_CONTROL)));
        assertEquals("hello ", model.getText());

        model.update(focus, tick("", key(GLFW_KEY_HOME)));
        assertEquals(0, model.editor().getCursor());
        model.update(focus, tick("", key(GLFW_KEY_DELETE)));
        assertEquals("ello ", model.getText());

        model.update(focus, tick("", key(GLFW_KEY_RIGHT, GLFW_MOD_SHIFT)));
        assertEquals("e", model.editor().selectedText());
    }

    @Test
    void shortcutsUseTheClipboard() {
        focused();
        model.update(focus, tick("abc def"));

        model.update(focus, tick("", ctrl('a')));
        model.update(focus, tick("", ctrl('c')));
        assertEquals("abc def", clipboard.content);

        model.update(focus, tick("", ctrl('x')));
        assertEquals("", model.getText());

        clipboard.content = "line one\nline two";
        model.update(focus, tick("", ctrl('v')));
        // One line: the line feed is a space
        assertEquals("line one line two", model.getText());
    }

    @Test
    void clickPlacesTheCursorAtTheNearestCharacter() {
        focused();
        model.update(focus, tick("abcd"));

        model.update(focus, click(14));
        assertEquals(1, model.editor().getCursor());
        model.update(focus, click(16));
        assertEquals(2, model.editor().getCursor());
        model.update(focus, click(500));
        assertEquals(4, model.editor().getCursor());
        model.update(focus, click(-30));
        assertEquals(0, model.editor().getCursor());
    }

    @Test
    void draggingSelects() {
        focused();
        model.update(focus, tick("abcdef"));
        model.update(focus, click(10));

        TextFieldModel.Tick drag = tick("");
        drag.mouseHeld = true;
        drag.mouseInside = true;
        drag.mouseX = 40;
        model.update(focus, drag);

        assertEquals("bcd", model.editor().selectedText());

        // Released: no more drag
        TextFieldModel.Tick released = tick("");
        model.update(focus, released);
        drag.mouseX = 0;
        model.update(focus, drag);
        assertEquals("bcd", model.editor().selectedText());
    }

    @Test
    void shiftClickExtendsTheSelection() {
        focused();
        model.update(focus, tick("abcdef"));
        model.update(focus, click(10));

        TextFieldModel.Tick shifted = click(40);
        shifted.shift = true;
        model.update(focus, shifted);
        assertEquals("bcd", model.editor().selectedText());
    }

    @Test
    void doubleClickSelectsTheWordAndTripleClickTheLine() {
        focused();
        model.update(focus, tick("one two three"));

        // In "two" (characters 4 to 7, 10 pixels each)
        model.update(focus, click(55));
        model.update(focus, quickClick(55));
        assertEquals("two", model.editor().selectedText());

        model.update(focus, quickClick(55));
        assertEquals("one two three", model.editor().selectedText());

        // A fourth click starts again: the cursor only
        model.update(focus, quickClick(55));
        assertFalse(model.editor().hasSelection());
    }

    @Test
    void doubleClickOnAFieldWithoutFocusTakesItAndSelectsTheWord() {
        model.setText("one two");
        model.update(focus, click(15));
        model.update(focus, quickClick(15));

        assertTrue(model.hasFocus(focus));
        assertEquals("one", model.editor().selectedText());
    }

    @Test
    void theMouseHeldAfterADoubleClickDoesNotDragTheSelection() {
        focused();
        model.update(focus, tick("one two"));
        model.update(focus, click(15));
        model.update(focus, quickClick(15));

        TextFieldModel.Tick held = tick("");
        held.mouseHeld = true;
        held.mouseInside = true;
        held.mouseX = 60;
        model.update(focus, held);
        assertEquals("one", model.editor().selectedText());
    }

    @Test
    void clicksFarApartOrSlowAreSingleClicks() {
        focused();
        model.update(focus, tick("one two"));
        model.update(focus, click(15));
        model.update(focus, quickClick(55));
        assertFalse(model.editor().hasSelection());

        model.update(focus, click(55));
        assertFalse(model.editor().hasSelection());
    }

    @Test
    void lineScrollsAroundTheCursor() {
        model.setAvailableWidth(50);
        focused();
        model.update(focus, tick("abcdefgh"));

        // Five characters fit: the last five are shown
        assertEquals(3, model.visibleStart());
        assertEquals(8, model.visibleEnd());
        assertEquals(50, model.xOf(8));

        // The cursor goes back to the beginning: the line follows
        model.update(focus, tick("", key(GLFW_KEY_HOME)));
        assertEquals(0, model.visibleStart());
        assertEquals(5, model.visibleEnd());
    }

    @Test
    void setTextPutsTheCursorAtTheEnd() {
        model.setText("abc");

        assertEquals("abc", model.getText());
        assertEquals(3, model.editor().getCursor());
        assertFalse(model.isChanged());
    }
}
