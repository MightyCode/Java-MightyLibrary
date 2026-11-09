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
        SUBMIT, CLOSE, SCROLL, SCROLL_END, CLEAR
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
        // Lines to scroll toward the oldest ones, negative toward the newest (SCROLL)
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
                editor.home();
                break;
            case GLFW_KEY_END:
                editor.end();
                break;
            case GLFW_KEY_UP:
                editor.previous();
                break;
            case GLFW_KEY_DOWN:
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
            case GLFW_KEY_V:
                if (key.isControl() && clipboard != null) {
                    String pasted = clipboard.get();
                    if (pasted != null)
                        editor.insert(pasted);
                }
                break;
            case GLFW_KEY_L:
                if (key.isControl() && !key.isRepeat())
                    requests.add(new Request(ERequest.CLEAR, null, 0));
                break;
            default:
                // Text comes from the typed text; other keys (F1, F2...) are actions read by the console
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
