package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.inputs.KeyEvent;
import io.github.mightycode.mightylib.utils.enginecommand.CommandCompletion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Turns what is typed in the console (text and keys of a tick) into edits of its command line and into requests
 * (run a line, close, scroll...). No GL, no window: the console gives it the inputs of the window it is shown in.
 */
public final class ConsoleInput {
    public enum ERequest {
        SUBMIT, CLOSE, SCROLL, SCROLL_TOP, SCROLL_END, CLEAR
    }

    public static final class Request {
        private final ERequest type;
        private final String line;
        private final int amount;

        private Request(ERequest type, String line, int amount) {
            this.type = type;
            this.line = line;
            this.amount = amount;
        }

        public ERequest getType() { return type; }
        // The line to run (SUBMIT)
        public String getLine() { return line; }
        // Rows to scroll toward the oldest ones, negative toward the newest (SCROLL)
        public int getAmount() { return amount; }

        @Override
        public String toString() {
            return type + (line != null ? " " + line : "") + (type == ERequest.SCROLL ? " " + amount : "");
        }
    }

    private final CommandLineEditor editor;
    private final Function<String, CommandCompletion> completer;
    // Written in the console log: the choices of an ambiguous completion
    private final Consumer<String> printer;

    // The last key was a Tab that completed nothing: the next one lists the choices (like bash)
    private boolean listOnNextTab;

    // Selection in the log, null if none; one selection at a time: the log one or the command line one
    private LogSelection logSelection;
    // Layout of the panel the inputs are for, during handle and handleMouse (null: not displayed yet)
    private ConsoleLayout layout;

    /**
     * @param completer gives the completion of the text before the cursor (Commands.complete)
     * @param printer   shows the choices of an ambiguous completion, asked by a second Tab
     */
    public ConsoleInput(CommandLineEditor editor, Function<String, CommandCompletion> completer,
                        Consumer<String> printer) {
        this.editor = editor;
        this.completer = completer;
        this.printer = printer;
    }

    public CommandLineEditor getEditor() { return editor; }

    // The selection in the log, null if none (or empty)
    LogSelection getLogSelection() {
        return logSelection == null || logSelection.isEmpty() ? null : logSelection;
    }

    // The bounds of the selection on forgotten lines go to the oldest line kept; it is dropped if they all are
    private void keepLogSelection() {
        if (logSelection != null && layout != null && !logSelection.keepFrom(layout.oldestKeptId()))
            logSelection = null;
    }

    /** The clipboard of the system: read on Ctrl+V only, written on Ctrl+C and Ctrl+X. */
    public interface IClipboard {
        String get();

        void set(String text);
    }

    /**
     * Apply the inputs of one tick: the typed text first, then the keys in their order.
     * @param pageLines lines scrolled by Page Up / Page Down
     */
    public List<Request> handle(String typedText, List<KeyEvent> keys, int pageLines, IClipboard clipboard) {
        return handle(typedText, keys, pageLines, clipboard, null);
    }

    /** @param layout the log as shown (selection, copy), null before the console is displayed */
    List<Request> handle(String typedText, List<KeyEvent> keys, int pageLines, IClipboard clipboard,
                         ConsoleLayout layout) {
        this.layout = layout;
        keepLogSelection();
        List<Request> requests = new ArrayList<>();

        if (typedText != null && !typedText.isEmpty()) {
            editor.insert(typedText);
            listOnNextTab = false;
            logSelection = null;
        }

        for (KeyEvent key : keys) {
            handleKey(key, pageLines, clipboard, requests);
            // A selection in the command line removes the one of the log
            if (editor.hasSelection())
                logSelection = null;
        }

        return requests;
    }

    private void handleKey(KeyEvent key, int pageLines, IClipboard clipboard, List<Request> requests) {
        boolean wasTab = listOnNextTab;
        listOnNextTab = false;

        // Shortcuts follow the letter of the layout of the system, not the position of the key. Not with Alt: AltGr
        // comes as Ctrl+Alt on Windows and types characters (AltGr+L: 'ł' in Polish), received as typed text
        if (key.isControl() && !key.isAlt() && key.letter() != 0) {
            handleShortcut(key, clipboard, requests);
            return;
        }

        switch (key.key()) {
            case GLFW_KEY_ENTER:
            case GLFW_KEY_KP_ENTER:
                if (!key.isRepeat()) {
                    logSelection = null;
                    String line = editor.submit();
                    if (!line.trim().isEmpty())
                        requests.add(new Request(ERequest.SUBMIT, line, 0));
                    requests.add(new Request(ERequest.SCROLL_END, null, 0));
                }
                break;
            case GLFW_KEY_ESCAPE:
                if (!key.isRepeat())
                    requests.add(new Request(ERequest.CLOSE, null, 0));
                break;
            case GLFW_KEY_BACKSPACE:
                logSelection = null;
                if (key.isControl())
                    editor.deleteWordLeft();
                else
                    editor.deleteLeft();
                break;
            case GLFW_KEY_DELETE:
                logSelection = null;
                editor.deleteRight();
                break;
            // Shift extends the selection of the command line
            case GLFW_KEY_LEFT:
                if (key.isControl())
                    editor.wordLeftMove(key.isShift());
                else
                    editor.left(key.isShift());
                break;
            case GLFW_KEY_RIGHT:
                if (key.isControl())
                    editor.wordRightMove(key.isShift());
                else
                    editor.right(key.isShift());
                break;
            case GLFW_KEY_HOME:
                // Ctrl: the top of the log, alone: the beginning of the command line
                if (key.isControl())
                    requests.add(new Request(ERequest.SCROLL_TOP, null, 0));
                else
                    editor.home(key.isShift());
                break;
            case GLFW_KEY_END:
                if (key.isControl())
                    requests.add(new Request(ERequest.SCROLL_END, null, 0));
                else
                    editor.end(key.isShift());
                break;
            case GLFW_KEY_UP:
                // Shift: one row of the log, alone: the history
                if (key.isShift())
                    requests.add(new Request(ERequest.SCROLL, null, 1));
                else
                    editor.previous();
                break;
            case GLFW_KEY_DOWN:
                if (key.isShift())
                    requests.add(new Request(ERequest.SCROLL, null, -1));
                else
                    editor.next();
                break;
            case GLFW_KEY_PAGE_UP:
                requests.add(new Request(ERequest.SCROLL, null, pageLines));
                break;
            case GLFW_KEY_PAGE_DOWN:
                requests.add(new Request(ERequest.SCROLL, null, -pageLines));
                break;
            case GLFW_KEY_TAB:
                complete(wasTab);
                break;
            default:
                // Text comes from the typed text; other keys (², F2...) are actions read by the console
                break;
        }
    }

    // Ctrl + a key that has a letter in the layout of the system
    private void handleShortcut(KeyEvent key, IClipboard clipboard, List<Request> requests) {
        switch (key.letter()) {
            case 'a':
                // First press: the command line; a second one (or an empty line): the whole log shown
                if (!editor.getText().isEmpty() && !editor.isAllSelected()) {
                    editor.selectAll();
                } else if (layout != null) {
                    LogPosition first = layout.firstPosition();
                    LogPosition last = layout.lastPosition();
                    if (first != null) {
                        editor.clearSelection();
                        logSelection = new LogSelection(first, last);
                    }
                }
                break;
            case 'c':
                // The selection stays; nothing without a selection
                copy(clipboard, false);
                break;
            case 'x':
                // Cut in the command line, copy in the log
                copy(clipboard, true);
                break;
            case 'v':
                logSelection = null;
                if (clipboard != null)
                    paste(clipboard.get(), requests);
                break;
            case 'l':
                if (!key.isRepeat())
                    requests.add(new Request(ERequest.CLEAR, null, 0));
                break;
            default:
                break;
        }
    }

    private void copy(IClipboard clipboard, boolean cut) {
        if (clipboard == null)
            return;

        LogSelection selection = getLogSelection();
        if (selection != null && layout != null)
            clipboard.set(layout.copyText(selection.start(), selection.end()));
        else if (editor.hasSelection())
            clipboard.set(cut ? editor.cutSelection() : editor.selectedText());
    }

    // ---- Mouse ----

    /** The left button of the mouse during a tick, in pixels from the top left of the panel. */
    static final class MouseState {
        final float x;
        final float y;
        final boolean pressed;
        final boolean held;
        final boolean released;
        // 1, 2 or 3 on a press (ClickCounter), 0 otherwise
        final int clicks;
        final boolean shift;
        // Time of the tick (System.nanoTime): the automatic scroll while dragging out of the log
        final long nanoTime;

        MouseState(float x, float y, boolean pressed, boolean held, boolean released, int clicks, boolean shift,
                   long nanoTime) {
            this.x = x;
            this.y = y;
            this.pressed = pressed;
            this.held = held;
            this.released = released;
            this.clicks = clicks;
            this.shift = shift;
            this.nanoTime = nanoTime;
        }
    }

    // Dragging above or below the log scrolls it by one row each time
    static final long AUTO_SCROLL_NANOSECONDS = 50_000_000L;

    private enum EDrag {
        NONE, INPUT, LOG
    }

    // What the button held since its press selects
    private EDrag drag = EDrag.NONE;
    private long lastAutoScroll;

    /**
     * Apply the mouse of one tick, in the command line or in the log: a press places the cursor or starts a selection
     * (Shift: extends the selection), dragging selects (out of the log: it scrolls), a double click selects a word, a
     * triple click the line (the log line, not its row).
     * @param layout where the rows and the command line are in the panel
     */
    List<Request> handleMouse(MouseState mouse, ConsoleLayout layout) {
        this.layout = layout;
        keepLogSelection();
        List<Request> requests = new ArrayList<>();

        if (mouse.pressed && layout.isOnIndicator(mouse.x, mouse.y)) {
            // The indicator of the lines below: back to the end
            drag = EDrag.NONE;
            requests.add(new Request(ERequest.SCROLL_END, null, 0));
        } else if (mouse.pressed && mouse.y >= layout.rowsBottom()) {
            listOnNextTab = false;
            logSelection = null;
            pressInput(mouse);
        } else if (mouse.pressed) {
            listOnNextTab = false;
            pressLog(mouse);
        } else if (mouse.held && drag == EDrag.INPUT) {
            editor.moveTo(layout.inputIndexAt(editor.getText(), mouse.x), true);
        } else if (mouse.held && drag == EDrag.LOG && logSelection != null) {
            dragLog(mouse);
        }

        if (mouse.released || !mouse.held) {
            drag = EDrag.NONE;
            // A click without move selects nothing
            if (logSelection != null && logSelection.isEmpty())
                logSelection = null;
        }

        return requests;
    }

    private void pressInput(MouseState mouse) {
        String text = editor.getText();
        int index = layout.inputIndexAt(text, mouse.x);
        drag = EDrag.NONE;

        switch (mouse.clicks) {
            case 2:
                // The word under the cursor (the separators of Ctrl+Left/Right)
                editor.select(WordStart(text, index), WordEnd(text, index));
                break;
            case 3:
                editor.selectAll();
                break;
            default:
                editor.moveTo(index, mouse.shift);
                drag = EDrag.INPUT;
                break;
        }
    }

    private void pressLog(MouseState mouse) {
        LogPosition position = layout.logPositionAt(mouse.x, mouse.y);
        drag = EDrag.NONE;
        if (position == null)
            return;

        editor.clearSelection();
        String text = mouse.clicks >= 2 ? layout.displayedText(position.lineId()) : null;
        if (mouse.clicks == 2 && text != null) {
            int index = Math.min(position.character(), text.length());
            logSelection = new LogSelection(new LogPosition(position.lineId(), WordStart(text, index)),
                    new LogPosition(position.lineId(), WordEnd(text, index)));
        } else if (mouse.clicks == 3 && text != null) {
            logSelection = new LogSelection(new LogPosition(position.lineId(), 0),
                    new LogPosition(position.lineId(), text.length()));
        } else {
            if (mouse.shift && logSelection != null)
                logSelection.extendTo(position);
            else
                logSelection = new LogSelection(position, position);
            drag = EDrag.LOG;
            lastAutoScroll = mouse.nanoTime;
        }
    }

    private void dragLog(MouseState mouse) {
        int direction = mouse.y < layout.rowsTop() ? 1 : mouse.y >= layout.rowsBottom() ? -1 : 0;
        if (direction != 0 && mouse.nanoTime - lastAutoScroll >= AUTO_SCROLL_NANOSECONDS) {
            layout.scroll(direction);
            lastAutoScroll = mouse.nanoTime;
        }

        // Out of the rows: the nearest row, scrolled
        LogPosition position = layout.logPositionAt(mouse.x, mouse.y);
        if (position != null)
            logSelection.extendTo(position);
    }

    // Start of the word around the index (the separators of Ctrl+Left/Right)
    private static int WordStart(CharSequence text, int index) {
        int start = index;
        while (start > 0 && !CommandLineEditor.IsSeparator(text.charAt(start - 1)))
            --start;
        return start;
    }

    private static int WordEnd(CharSequence text, int index) {
        int end = index;
        while (end < text.length() && !CommandLineEditor.IsSeparator(text.charAt(end)))
            ++end;
        return end;
    }

    // A text of several lines runs a command per complete line, the last one stays in the command line
    private void paste(String pasted, List<Request> requests) {
        if (pasted == null)
            return;

        List<String> lines = editor.paste(pasted);
        for (String line : lines)
            requests.add(new Request(ERequest.SUBMIT, line, 0));
        if (!lines.isEmpty())
            requests.add(new Request(ERequest.SCROLL_END, null, 0));
    }

    // Tab: write what all the choices share; a second Tab without progress lists them
    private void complete(boolean secondTab) {
        editor.clearSelection();
        String beginning = editor.getText().substring(0, editor.getCursor());
        CommandCompletion completion = completer.apply(beginning);

        String replacement = completion.replacement();
        if (replacement != null) {
            editor.replaceBeforeCursor(completion.getStart(), replacement);
            listOnNextTab = !completion.isUnique();
            return;
        }

        if (completion.getCandidates().size() > 1) {
            if (secondTab)
                printer.accept(String.join("  ", completion.getCandidates()));
            else
                listOnNextTab = true;
        }
    }
}
