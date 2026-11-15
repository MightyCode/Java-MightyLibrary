package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.services.IEngineService;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.json.JSONObject;

/**
 * The in-game console (service mightylib:console, Engine.console): registered by the engine only when the player has an
 * access to the commands (EngineConfig.setCommandAccess). A plugin that uses it declares after(EngineServices.CONSOLE)
 * and tests Engine.hasService.
 */
public interface IConsoleService extends IEngineService {
    enum EMode {
        CLOSED, OVERLAY, DETACHED
    }

    /** Write a message of the game for the player, seen from USER on. Any thread. */
    void print(String text);

    /** Write a message of the game, seen from the level on (CHEAT: only when the cheats are on). Any thread. */
    void print(String text, ECommandLevel level);

    // Over the main window
    void open();

    void close();

    void toggle();

    /** The console in a window of its own; stays on the main window if the window can't be created. */
    void detach();

    /** Back to the main window, open. */
    void attach();

    EMode getMode();

    // The program output among the lines shown
    void setLogsShown(boolean shown);

    boolean areLogsShown();

    // The data of the command results (JSON) among the lines shown
    void setDataShown(boolean shown);

    boolean isDataShown();

    /**
     * Copy the last lines of the console to the clipboard of the system: the lines the level sees, without the hidden
     * types (unless all). Main thread.
     * @param count lines to copy, 0 or less: every line kept
     * @return the copied text, empty (clipboard unchanged) when there is no line
     */
    String copyLast(int count, ECommandLevel level, boolean all);

    // The same, the lines up to upToId only: a command copies the lines before its own echo (the newest COMMAND line)
    String copyLast(int count, ECommandLevel level, boolean all, long upToId);

    // State for the developer (command console): mode, logs shown, scroll, selection
    JSONObject stateJson();

    // What the console shows
    ConsoleLog getLog();
}
