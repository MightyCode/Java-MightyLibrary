package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.console.Console;
import io.github.mightycode.mightylib.console.ConsoleLog;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.function.Consumer;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.choice;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.integer;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.longValue;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

/**
 * Drive the in-game console and read its log (program output and commands of every source).
 */
public class ConsoleGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "console";
    private static final int DEFAULT_LOG_LINES = 20;

    private final Commands commands;

    public ConsoleGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.USER,
                "Drive the in-game console and read its log");
        this.commands = commands;
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("Mode of the in-game console (closed, overlay, detached)",
                        context -> status(Views.engine().console()))
                .then(drive("open", "The console over the main window (²)", Console::open))
                .then(drive("close", "Close the console", Console::close))
                .then(drive("toggle", "Open the console, or close it", Console::toggle))
                .then(drive("detach", "The console in a window of its own (F2)", Console::detach))
                .then(drive("attach", "The console back on the main window", Console::attach))
                .then(literal("clear", "Clear the lines of the console").executes(context -> {
                    ConsoleLog log = Views.engine().consoleLog();
                    if (log == null)
                        return CommandResult.error("No console log");
                    // Below DEV, only for the source and the levels below: the developer keeps the log
                    log.clear(context.currentLevel());
                    return CommandResult.ok("");
                }))
                // The whole log (program output included) is for the developer
                .then(literal("log", "The last lines of the log, with their id").level(ECommandLevel.DEV)
                        .executes(context -> log(Views.engine().consoleLog(), DEFAULT_LOG_LINES, null))
                        .then(literal("since", "The lines written after the line with this id")
                                .then(argument("id", longValue(0, Long.MAX_VALUE), "Id of a line")
                                        .executes(context -> log(Views.engine().consoleLog(), 0, context.get("id")))))
                        .then(argument("count", integer(1, Integer.MAX_VALUE),
                                "Number of lines (" + DEFAULT_LOG_LINES + " by default)")
                                .executes(context -> log(Views.engine().consoleLog(), context.get("count"), null))))
                .then(literal("copy", "Copy the last lines of the console to the clipboard, as shown"
                        + " (the program output only when it is shown)")
                        .option(Option.of("all", "Copy the program output too, even when the console hides it")
                                .level(ECommandLevel.DEV))
                        .executes("Copy every line of the console", context -> copy(context, 0))
                        .then(argument("count", integer(1, Integer.MAX_VALUE), "Number of lines, the last ones")
                                .executes(context -> copy(context, context.get("count")))))
                // The program output is seen in DEV only
                .then(literal("logs", "Show or hide the program output (stdout, stderr) in the console")
                        .level(ECommandLevel.DEV)
                        .executes(context -> logs(null))
                        .then(argument("state", choice("on", "off"), "Show the program output or hide it")
                                .executes(context -> logs(context.get("state").equals("on")))))
                .then(literal("follow", "Receive every new line of the log (debug server, see DebugServer)")
                        .level(ECommandLevel.DEV)
                        .executes(context -> follow(true))
                        .then(argument("state", choice("on", "off"), "Follow the log or stop")
                                .executes(context -> follow(context.get("state").equals("on")))));
    }

    // A sub-command that acts on the console
    private static CommandNode drive(String word, String description, Consumer<Console> action) {
        return literal(word, description).executes(context -> {
            Console console = Views.engine().console();
            if (console == null)
                return status(null);

            action.accept(console);
            return status(console);
        });
    }

    private static CommandResult copy(CommandContext context, int count) {
        Console console = Views.engine().console();
        if (console == null)
            return status(null);

        // Commands run on the main thread just after their echo: the newest COMMAND line is this one, not copied
        long echo = console.getLog().newestId(ConsoleLog.EType.COMMAND);
        String text = console.copyLast(count, context.currentLevel(), context.has("all"),
                echo > 0 ? echo - 1 : Long.MAX_VALUE);
        if (text.isEmpty())
            return CommandResult.ok("No line to copy", new JSONObject().put("lines", 0).put("chars", 0));

        int lines = text.split("\n", -1).length;
        return CommandResult.ok(lines + (lines == 1 ? " line copied" : " lines copied"),
                new JSONObject().put("lines", lines).put("chars", text.length()));
    }

    // null: switch
    private static CommandResult logs(Boolean shown) {
        Console console = Views.engine().console();
        if (console == null)
            return status(null);

        console.setLogsShown(shown == null ? !console.areLogsShown() : shown);
        return CommandResult.ok(console.areLogsShown() ? "Logs shown" : "Logs hidden",
                new JSONObject().put("logsShown", console.areLogsShown()));
    }

    private CommandResult follow(boolean on) {
        Engine engine = Views.engine();
        if (engine.consoleLog() == null)
            return CommandResult.error("No console log: enable the console or the debug mode");
        if (!commands.getCurrentSource().setFollowLog(on))
            return CommandResult.error("The source " + commands.getCurrentSource().name() + " can't follow the console log");

        return CommandResult.ok(on ? "Following the console log" : "Console log no longer followed");
    }

    private static CommandResult status(Console console) {
        if (console == null)
            return CommandResult.error("The console is disabled (EngineConfig.setCommandAccess)");

        return CommandResult.ok("Console " + console.getMode().name().toLowerCase(), console.stateJson());
    }

    // The last lines, or the lines after the id (when not null)
    private static CommandResult log(ConsoleLog log, int count, Long since) {
        if (log == null)
            return CommandResult.error("No console log: enable the console or the debug mode");

        List<ConsoleLog.Line> lines = since == null ? log.last(count) : log.since(since);

        JSONArray array = new JSONArray();
        for (ConsoleLog.Line line : lines) {
            array.put(new JSONObject()
                    .put("id", line.getId())
                    .put("type", line.getType().name())
                    .put("level", line.getLevel().name())
                    .put("source", line.getSource() == null ? JSONObject.NULL : line.getSource())
                    .put("text", line.getText()));
        }

        return CommandResult.ok(lines.size() + " line(s) of the console log", new JSONObject().put("lines", array));
    }
}
