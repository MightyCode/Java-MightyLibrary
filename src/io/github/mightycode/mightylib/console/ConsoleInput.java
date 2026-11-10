package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.inputs.KeyEvent;
import io.github.mightycode.mightylib.utils.enginecommand.CommandCompletion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    /**
     * Apply the inputs of one tick: the typed text first, then the keys in their order.
     * @param pageLines lines scrolled by Page Up / Page Down
     * @param clipboard read on Ctrl+V only
     */
    public List<Request> handle(String typedText, List<KeyEvent> keys, int pageLines, Supplier<String> clipboard) {
        List<Request> requests = new ArrayList<>();

        if (typedText != null && !typedText.isEmpty()) {
            editor.insert(typedText);
            listOnNextTab = false;
        }

        for (KeyEvent key : keys)
            handleKey(key, pageLines, clipboard, requests);

        return requests;
    }

    private void handleKey(KeyEvent key, int pageLines, Supplier<String> clipboard, List<Request> requests) {
        boolean wasTab = listOnNextTab;
        listOnNextTab = false;

        // Shortcuts follow the letter of the layout of the system, not the position of the key
        if (key.isControl() && key.letter() != 0) {
            handleShortcut(key, clipboard, requests);
            return;
        }

        switch (key.key()) {
            case GLFW_KEY_ENTER:
            case GLFW_KEY_KP_ENTER:
                if (!key.isRepeat()) {
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
                if (key.isControl())
                    editor.deleteWordLeft();
                else
                    editor.deleteLeft();
                break;
            case GLFW_KEY_DELETE:
                editor.deleteRight();
                break;
            case GLFW_KEY_LEFT:
                if (key.isControl())
                    editor.wordLeftMove();
                else
                    editor.left();
                break;
            case GLFW_KEY_RIGHT:
                if (key.isControl())
                    editor.wordRightMove();
                else
                    editor.right();
                break;
            case GLFW_KEY_HOME:
                // Ctrl: the top of the log, alone: the beginning of the command line
                if (key.isControl())
                    requests.add(new Request(ERequest.SCROLL_TOP, null, 0));
                else
                    editor.home();
                break;
            case GLFW_KEY_END:
                if (key.isControl())
                    requests.add(new Request(ERequest.SCROLL_END, null, 0));
                else
                    editor.end();
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
    private void handleShortcut(KeyEvent key, Supplier<String> clipboard, List<Request> requests) {
        switch (key.letter()) {
            case 'v':
                if (clipboard != null) {
                    String pasted = clipboard.get();
                    if (pasted != null)
                        editor.insert(pasted);
                }
                break;
            case 'l':
                if (!key.isRepeat())
                    requests.add(new Request(ERequest.CLEAR, null, 0));
                break;
            default:
                break;
        }
    }

    // Tab: write what all the choices share; a second Tab without progress lists them
    private void complete(boolean secondTab) {
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
