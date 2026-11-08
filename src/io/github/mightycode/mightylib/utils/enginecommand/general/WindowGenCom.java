package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.EWindowMode;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONObject;

/**
 * Chooses the window that the commands input, screenshot, monitors, scene, camera and exit work on.
 */
public class WindowGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "window";

    public WindowGenCom() {
        super(COMMAND_NAME);
    }

    @Override
    public CommandResult execute(String[] args) {
        if (checkArgumentHelp(args))
            return CommandResult.ok(help());

        Engine engine = Views.engine();

        if (args.length == 1)
            return describeTarget(engine, "Target window");

        if (args[1].equalsIgnoreCase("main")) {
            engine.setCommandWindow(null);
            return describeTarget(engine, "Target window is the main window again");
        }

        if (args[1].equalsIgnoreCase("mode"))
            return changeMode(engine, args);

        boolean close = args[1].equalsIgnoreCase("close");
        String reference = close ? (args.length > 2 ? args[2] : null) : args[1];

        WindowContext window = reference == null ? engine.commandWindow() : engine.findWindow(reference);
        if (window == null)
            return CommandResult.error("Unknown window ->" + reference + "<-, give its id, its title or the name of a context (windows)");

        if (close) {
            window.requestClose();
            return CommandResult.ok("Window " + window.getId() + " (" + window.getName() + ") closes at the end of the frame");
        }

        engine.setCommandWindow(window);
        return describeTarget(engine, "Target window");
    }

    // window mode MODE [ID|TITLE|CONTEXT]
    private static CommandResult changeMode(Engine engine, String[] args) {
        EWindowMode mode = args.length > 2 ? EWindowMode.FromText(args[2]) : null;
        if (mode == null)
            return CommandResult.error("Give a mode : window mode windowed|fullscreen|borderless [ID|TITLE|CONTEXT]");

        String reference = args.length > 3 ? args[3] : null;
        WindowContext window = reference == null ? engine.commandWindow() : engine.findWindow(reference);
        if (window == null)
            return CommandResult.error("Unknown window ->" + reference + "<-, give its id, its title or the name of a context (windows)");

        window.setWindowMode(mode);

        // The applied mode may differ: a screen shows one exclusive fullscreen window at most
        JSONObject data = new JSONObject();
        data.put("id", window.getId());
        data.put("mode", window.getWindow().getInfo().getWindowMode().name().toLowerCase());
        data.put("size", window.getWindow().getInfo().getSizeRef().x + "x" + window.getWindow().getInfo().getSizeRef().y);

        return CommandResult.ok("Window " + window.getId() + " (" + window.getName() + ") : " + window.getDisplay(), data);
    }

    private static CommandResult describeTarget(Engine engine, String message) {
        WindowContext window = engine.commandWindow();

        JSONObject data = new JSONObject();
        data.put("id", window.getId());
        data.put("title", window.getName());
        data.put("context", window.getContext().getName());

        return CommandResult.ok(message + " : " + window.getId() + " (" + window.getName() + ", context "
                + window.getContext().getName() + ")", data);
    }

    @Override
    public String help() {
        return super.help() +
                "\nwindow : the target window" +
                "\nwindow ID|TITLE|CONTEXT : the target window becomes this one (see windows)" +
                "\nwindow main : back to the main window" +
                "\nwindow close [ID|TITLE|CONTEXT] : close the window (the target by default) at the end of the frame" +
                "\nwindow mode windowed|fullscreen|borderless [ID|TITLE|CONTEXT] : change the display mode of the window"
                + " (the target by default), its other display settings stay; a second exclusive fullscreen window on"
                + " a screen becomes borderless" +
                "\nThe target is where input, screenshot, monitors, scene, camera and exit work; it goes back to the"
                + " main window when it is closed";
    }
}
