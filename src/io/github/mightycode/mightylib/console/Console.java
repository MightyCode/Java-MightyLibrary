package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.inputs.EngineActions;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.main.DisplaySettings;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowConfig;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandSource;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.json.JSONObject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.glfwFocusWindow;

/**
 * The in-game console, owned by the engine and independent of the scenes: ² opens it over the main
 * window (semi-transparent, the game sees no input meanwhile), F2 moves it to a window of its own and back.
 * It shows the console log lines the level of the session sees (in DEV the program output, the commands of every
 * source) and runs what is typed as commands at that level.
 * Main thread only: update() at the beginning of each tick, displayOverlay() after the scene of the main window.
 */
public final class Console {
    public enum EMode {
        CLOSED, OVERLAY, DETACHED
    }

    private static final float OVERLAY_HEIGHT = 0.45f;
    private static final Color4f OVERLAY_BACKGROUND = new Color4f(0f, 0f, 0f, 0.55f);
    private static final Color4f OVERLAY_INPUT_BACKGROUND = new Color4f(0f, 0f, 0f, 0.35f);
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 500;
    // Rows scrolled by one notch of the mouse wheel
    private static final int WHEEL_ROWS = 3;

    private final Engine engine;
    private final ConsoleLog log;
    private final Commands commands;
    // What is typed in the console: the level of the session (the player)
    private final ICommandSource source;
    private final String fontName;
    private final CommandLineEditor editor;
    private final ConsoleInput input;
    private final ClickCounter clickCounter = new ClickCounter();

    private EMode mode = EMode.CLOSED;
    // Types of log lines the console doesn't show (the log keeps them), shared by the overlay and the window
    private final Set<ConsoleLog.EType> hiddenTypes = EnumSet.noneOf(ConsoleLog.EType.class);

    // Overlay of the main window, created at its first display
    private ConsoleView overlayView;
    private ConsolePanel overlay;

    private ConsoleWindowView windowView;
    private WindowContext window;

    // Window whose inputs the overlay captures
    private WindowContext captured;
    // The capture ends after the tick that closed the overlay: the key that closed it is not seen by the game
    private boolean releaseCapture;
    // Cursor shown by the overlay (a 3D scene may hide it), given back at the end
    private boolean cursorShownBefore = true;
    private boolean displayFailed;

    public Console(Engine engine, ConsoleLog log, Commands commands, String fontName) {
        this.engine = engine;
        this.log = log;
        this.commands = commands;
        this.source = ICommandSource.Named("game", commands::getSessionLevel);
        this.fontName = fontName;
        this.editor = new CommandLineEditor();
        this.input = new ConsoleInput(editor, line -> commands.complete(line, source),
                new CompletionListing(log, this::bell));
    }

    // Level of the lines shown: the one of the session
    ECommandLevel getViewerLevel() {
        return commands.getSessionLevel();
    }

    /** Write a message of the game for the player, seen from USER on. Any thread. */
    public void print(String text) {
        print(text, ECommandLevel.USER);
    }

    /** Write a message of the game, seen from the level on (CHEAT: only when the cheats are on). Any thread. */
    public void print(String text, ECommandLevel level) {
        log.add(ConsoleLog.EType.MESSAGE, null, text, level);
    }

    // A Tab was received but changes nothing: the command line of the shown console flashes
    private void bell() {
        ConsolePanel panel = shownPanel();
        if (panel != null)
            panel.flash();
    }

    // The panel of the window the console is in (the overlay when closed), null before its first display
    private ConsolePanel shownPanel() {
        return mode == EMode.DETACHED && windowView != null ? windowView.getPanel() : overlay;
    }

    /**
     * State of the console for the developer (command console): mode, logs shown, scroll ("end", or the line at the
     * bottom of the view and its rows hidden below), selection (null, or where it is).
     */
    public JSONObject stateJson() {
        JSONObject json = new JSONObject().put("mode", mode.name()).put("logsShown", areLogsShown());

        ConsolePanel panel = shownPanel();
        ConsoleLayout.Anchor anchor = panel == null ? null : panel.getLayout().anchor();
        json.put("scroll", anchor == null ? "end"
                : new JSONObject().put("id", anchor.lineId()).put("row", anchor.rowsBelow()));
        // Lines below the view, at most ConsoleLayout.MAX_LINES_BELOW + 1 (more than the maximum)
        json.put("newLinesBelow", panel == null ? 0 : panel.getLayout().linesBelow());

        LogSelection logSelection = input.getLogSelection();
        if (logSelection != null)
            json.put("selection", new JSONObject().put("in", "log")
                    .put("from", positionJson(logSelection.start())).put("to", positionJson(logSelection.end())));
        else if (editor.hasSelection())
            json.put("selection", new JSONObject().put("in", "input")
                    .put("start", editor.selectionStart()).put("end", editor.selectionEnd()));
        else
            json.put("selection", JSONObject.NULL);

        return json;
    }

    /**
     * Show or hide the program output (stdout, stderr) in the console; the log keeps it (console log, debug server).
     * The view keeps its place: hiding then showing the lines comes back to the same one.
     */
    public void setLogsShown(boolean shown) {
        if (shown) {
            hiddenTypes.remove(ConsoleLog.EType.OUT);
            hiddenTypes.remove(ConsoleLog.EType.ERR);
        } else {
            hiddenTypes.add(ConsoleLog.EType.OUT);
            hiddenTypes.add(ConsoleLog.EType.ERR);
        }
    }

    public boolean areLogsShown() {
        return !hiddenTypes.contains(ConsoleLog.EType.OUT) || !hiddenTypes.contains(ConsoleLog.EType.ERR);
    }

    /**
     * Copy the last lines of the console to the clipboard of the system, as Ctrl+C on a selection of them would: the
     * lines the level sees, without the types hidden by "console logs off" (unless all). Main thread.
     * @param count lines to copy, 0 or less: every line kept
     * @return the copied text, empty (clipboard unchanged) when there is no line
     */
    public String copyLast(int count, ECommandLevel level, boolean all) {
        return copyLast(count, level, all, Long.MAX_VALUE);
    }

    // The same, the lines up to upToId only: a command copies the lines before its own echo (the newest COMMAND line)
    public String copyLast(int count, ECommandLevel level, boolean all, long upToId) {
        Set<ConsoleLog.EType> hidden = all ? Collections.<ConsoleLog.EType>emptySet() : getHiddenTypes();
        String text = LogSelection.LastLinesText(log, level, line -> !hidden.contains(line.getType()), count, upToId);
        if (!text.isEmpty())
            engine.contexts().getMainWindow().getSystemInfo().setClipboardContent(text);
        return text;
    }

    // The types of lines not shown (read by the panels at each display)
    Set<ConsoleLog.EType> getHiddenTypes() { return Collections.unmodifiableSet(hiddenTypes); }

    public ConsoleLog getLog() { return log; }

    public EMode getMode() { return mode; }

    CommandLineEditor getEditor() { return editor; }

    String getFontName() { return fontName; }

    // ---- Changes of mode (keys, command console) ----

    public void open() {
        if (mode == EMode.OVERLAY)
            return;
        if (mode == EMode.DETACHED) {
            focus(window);
            return;
        }

        mode = EMode.OVERLAY;
        capture(engine.contexts().getMainWindow());
    }

    public void close() {
        if (mode == EMode.CLOSED)
            return;

        if (mode == EMode.DETACHED)
            closeWindow();

        mode = EMode.CLOSED;
        releaseCapture = true;
    }

    public void toggle() {
        if (mode == EMode.CLOSED)
            open();
        else
            close();
    }

    /** The console in a window of its own; stays on the main window if the window can't be created. */
    public void detach() {
        if (mode == EMode.DETACHED)
            return;

        ConsoleWindowView view = new ConsoleWindowView(this);
        WindowConfig config = new WindowConfig("Console")
                .setDisplay(new DisplaySettings().setResizable(true))
                .setSize(WINDOW_WIDTH, WINDOW_HEIGHT)
                .setPersistent(true)
                .setProjectInputs(false);

        try {
            window = engine.contexts().getMainWindow().getContext().openWindow(config, view);
        } catch (RuntimeException e) {
            Logger.LogException("The console window can't be opened", e);
            return;
        }

        windowView = view;
        mode = EMode.DETACHED;
        // The main window gets its inputs back after this tick
        releaseCapture = true;
        focus(window);
    }

    /** Back to the main window, open. */
    public void attach() {
        if (mode != EMode.DETACHED)
            return;

        closeWindow();
        mode = EMode.OVERLAY;
        releaseCapture = false;
        WindowContext main = engine.contexts().getMainWindow();
        capture(main);
        focus(main);
    }

    private void closeWindow() {
        WindowContext closing = window;
        window = null;
        windowView = null;
        if (closing != null)
            closing.getContext().closeWindow(closing);
    }

    // The window was closed (by the user, the engine stopping, or attach)
    void onWindowClosed(ConsoleWindowView view) {
        if (view != windowView)
            return;

        window = null;
        windowView = null;
        mode = EMode.CLOSED;
    }

    private void capture(WindowContext target) {
        if (captured == target)
            return;
        releaseNow();

        captured = target;
        target.getInputManager().setCaptured(true);
        MouseManager mouse = target.getMouseManager();
        cursorShownBefore = mouse.getCursorState();
        if (!cursorShownBefore)
            mouse.setCursor(true);
    }

    private void releaseNow() {
        if (captured == null)
            return;

        captured.getInputManager().setCaptured(false);
        if (!cursorShownBefore)
            captured.getMouseManager().setCursor(false);
        captured = null;
    }

    private static void focus(WindowContext target) {
        if (target != null)
            glfwFocusWindow(target.getWindow().getInfo().getWindowId());
    }

    // ---- Run by the engine ----

    /** Beginning of a tick, before the scenes: what is typed is run in this tick. */
    public void update() {
        if (releaseCapture) {
            releaseCapture = false;
            if (mode != EMode.OVERLAY)
                releaseNow();
        }

        WindowContext main = engine.contexts().getMainWindow();
        InputManager mainInputs = main.getInputManager();
        boolean toggle = mainInputs.inputPressed(EngineActions.TOGGLE_CONSOLE);

        switch (mode) {
            case CLOSED:
                if (toggle)
                    open();
                break;
            case OVERLAY:
                if (toggle)
                    close();
                else if (mainInputs.inputPressed(EngineActions.DETACH_CONSOLE))
                    detach();
                else
                    type(main, overlay);
                break;
            case DETACHED:
                if (toggle)
                    focus(window);

                if (window != null && window.getInputManager().inputPressed(EngineActions.DETACH_CONSOLE))
                    attach();
                else if (window != null)
                    type(window, windowView.getPanel());
                break;
        }
    }

    private void type(WindowContext target, ConsolePanel panel) {
        KeyboardManager keyboard = target.getKeyboardManager();
        int pageRows = panel == null ? 10 : panel.getVisibleRows();
        List<ConsoleInput.Request> requests = input.handle(keyboard.getRawTypedText(), keyboard.getRawKeyEvents(),
                pageRows, clipboardOf(target), panel == null ? null : panel.getLayout());

        float wheel = target.getMouseManager().getRawMouseScroll().y;
        if (wheel != 0 && panel != null)
            panel.scroll(Math.round(wheel * WHEEL_ROWS));

        if (panel != null)
            requests.addAll(input.handleMouse(mouseState(target, panel), panel.getLayout()));

        for (ConsoleInput.Request request : requests) {
            switch (request.getType()) {
                case SUBMIT:
                    commands.submit(request.getLine(), source);
                    break;
                case CLOSE:
                    // Escape closes the overlay; the window is closed by F2 or its own button
                    if (mode == EMode.OVERLAY)
                        close();
                    break;
                case CLEAR:
                    // Only what the player sees: the developer keeps the log (console log, debug server)
                    log.clear(getViewerLevel());
                    break;
                case SCROLL:
                    if (panel != null)
                        panel.scroll(request.getAmount());
                    break;
                case SCROLL_TOP:
                    if (panel != null)
                        panel.scrollToTop();
                    break;
                case SCROLL_END:
                    if (panel != null)
                        panel.scrollToEnd();
                    break;
            }
        }
    }

    // The left button of the window's mouse, in pixels from the top left of the panel
    private ConsoleInput.MouseState mouseState(WindowContext target, ConsolePanel panel) {
        MouseManager mouse = target.getMouseManager();
        KeyboardManager keyboard = target.getKeyboardManager();
        Vector2f position = mouse.getRawPos();
        float x = position.x - panel.originX();
        float y = position.y - panel.originY();
        boolean pressed = mouse.rawButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        int clicks = pressed ? clickCounter.press(position.x, position.y, System.nanoTime()) : 0;
        boolean shift = keyboard.getRawKeyState(GLFW_KEY_LEFT_SHIFT) || keyboard.getRawKeyState(GLFW_KEY_RIGHT_SHIFT);

        // A press above the overlay (on the game) is not for the console
        if (pressed && y < 0)
            pressed = false;

        return new ConsoleInput.MouseState(x, y, pressed, mouse.getRawButton(GLFW_MOUSE_BUTTON_LEFT),
                mouse.rawButtonReleased(GLFW_MOUSE_BUTTON_LEFT), clicks, shift, System.nanoTime());
    }

    private static JSONObject positionJson(LogPosition position) {
        return new JSONObject().put("id", position.lineId()).put("char", position.character());
    }

    // The selection in the log, drawn by the panels; null if none
    LogSelection getLogSelection() { return input.getLogSelection(); }

    // The clipboard of the system, through the window the console is typed in
    private static ConsoleInput.IClipboard clipboardOf(WindowContext target) {
        return new ConsoleInput.IClipboard() {
            @Override
            public String get() {
                return target.getSystemInfo().getClipboardContent();
            }

            @Override
            public void set(String text) {
                target.getSystemInfo().setClipboardContent(text);
            }
        };
    }

    /** After the scene of the main window is drawn, with its OpenGL context current. */
    public void displayOverlay() {
        if (mode != EMode.OVERLAY || displayFailed)
            return;

        WindowContext main = engine.contexts().getMainWindow();
        try {
            if (overlay == null) {
                overlayView = new ConsoleView(engine, main);
                try (Views.Scope ignored = Views.enter(overlayView)) {
                    overlay = new ConsolePanel(overlayView, this, OVERLAY_BACKGROUND, OVERLAY_INPUT_BACKGROUND);
                }
            }

            Vector2i size = main.getWindow().getInfo().getSizeRef();
            float height = Math.max(1, Math.round(size.y * OVERLAY_HEIGHT));
            try (Views.Scope ignored = Views.enter(overlayView)) {
                overlay.display(0, size.y - height, size.x, height, true);
            }
        } catch (RuntimeException e) {
            // A console that fails must not stop the game: it stays closed
            displayFailed = true;
            Logger.LogException("The console overlay failed, it is disabled", e);
            close();
        }
    }

    /** End of the engine, with the main window's OpenGL context current: what the overlay created is freed. */
    public void unload() {
        releaseNow();
        if (overlay != null) {
            try (Views.Scope ignored = Views.enter(overlayView)) {
                overlay.unload();
            }
            overlay = null;
        }
    }
}
