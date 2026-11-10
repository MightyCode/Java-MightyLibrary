package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.EWindowMode;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandException;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.ArgumentType;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.main.Context;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.bool;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.enumOf;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.oneOf;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

/**
 * Chooses the window that the commands input, screenshot, monitors, scene, camera and exit work on.
 */
public class WindowGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "window";

    public WindowGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The window that the commands input, screenshot, monitors, scene, camera and exit work on");
    }

    @Override
    protected void syntax(CommandNode root) {
        ArgumentType<String> reference = oneOf(() -> windowReferences(Views.engine()));

        root.executes("The target window", context -> describeTarget(Views.engine(), "Target window"))
                .then(literal("main", "The target goes back to the main window").executes(context -> {
                    Views.engine().setCommandWindow(null);
                    return describeTarget(Views.engine(), "Target window is the main window again");
                }))
                .then(literal("close", "Close the window (the target by default) at the end of the frame")
                        .executes(context -> close(Views.engine().commandWindow()))
                        .then(argument("window", reference, "Id, title or name of a context (see windows)")
                                .executes(context -> close(find(context.get("window"))))))
                .then(literal("mode", "Change the display mode of the window (the target by default), its other"
                        + " display settings stay; a second exclusive fullscreen window on a screen becomes borderless")
                        .then(argument("mode", enumOf(EWindowMode.class), "Display mode")
                                .executes(context -> changeMode(context.get("mode"), Views.engine().commandWindow()))
                                .then(argument("window", reference, "Id, title or name of a context (see windows)")
                                        .executes(context -> changeMode(context.get("mode"),
                                                find(context.get("window")))))))
                .then(literal("vsync", "Frames of the window (the target by default) wait for the screen refresh or not")
                        .then(argument("enabled", bool(), "true to wait for the screen refresh, false not to")
                                .executes(context -> changeVsync(context.get("enabled"), Views.engine().commandWindow()))
                                .then(argument("window", reference, "Id, title or name of a context (see windows)")
                                        .executes(context -> changeVsync(context.get("enabled"),
                                                find(context.get("window")))))))
                .then(argument("window", reference, "Id, title or name of a context (see windows)")
                        .executes("The target window becomes this one (see windows)", context -> {
                            Views.engine().setCommandWindow(find(context.get("window")));
                            return describeTarget(Views.engine(), "Target window");
                        }));
    }

    // Ids, titles and names of contexts, as accepted by Engine.findWindow
    static List<String> windowReferences(Engine engine) {
        List<String> references = new ArrayList<>();
        for (Context context : engine.contexts().getContexts()) {
            references.add(context.getName());
            for (WindowContext window : context.getWindows()) {
                references.add(String.valueOf(window.getId()));
                references.add(window.getName());
            }
        }
        return references;
    }

    // The window of a reference given by the completion: it exists unless it closed since
    private static WindowContext find(String reference) {
        WindowContext window = Views.engine().findWindow(reference);
        if (window == null)
            throw new CommandException("Unknown window ->" + reference + "<-, give its id, its title or the name of a context (windows)");

        return window;
    }

    private static CommandResult close(WindowContext window) {
        window.requestClose();
        return CommandResult.ok("Window " + window.getId() + " (" + window.getName() + ") closes at the end of the frame");
    }

    private static CommandResult changeMode(EWindowMode mode, WindowContext window) {
        window.setWindowMode(mode);

        // The applied mode may differ: a screen shows one exclusive fullscreen window at most
        JSONObject data = new JSONObject();
        data.put("id", window.getId());
        data.put("mode", window.getWindow().getInfo().getWindowMode().name().toLowerCase());
        data.put("size", window.getWindow().getInfo().getSizeRef().x + "x" + window.getWindow().getInfo().getSizeRef().y);

        return CommandResult.ok("Window " + window.getId() + " (" + window.getName() + ") : " + window.getDisplay(), data);
    }

    private static CommandResult changeVsync(boolean enabled, WindowContext window) {
        window.setDisplay(window.getDisplay().setVsync(enabled));

        JSONObject data = new JSONObject();
        data.put("id", window.getId());
        data.put("vsync", window.getDisplay().isVsync());

        return CommandResult.ok("Window " + window.getId() + " (" + window.getName() + ") : vsync "
                + (enabled ? "on" : "off"), data);
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
}
