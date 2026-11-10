package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.console.Console;
import io.github.mightycode.mightylib.console.ConsoleLog;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Drive the in-game console and read its log (program output and commands of every source).
 */
public class ConsoleGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "console";
    private static final int DEFAULT_LOG_LINES = 20;

    private static final List<String> MODES = Arrays.asList(
            "open", "close", "toggle", "detach", "attach", "clear");
    // The whole log (program output included) is for the developer
    private static final List<String> DEV_MODES = Arrays.asList("log", "follow");

    private final Commands commands;

    public ConsoleGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.USER);
        this.commands = commands;
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        if (args.length == 2) {
            if (!hasLevel(ECommandLevel.DEV))
                return MODES;

            List<String> modes = new ArrayList<>(MODES);
            modes.addAll(DEV_MODES);
            return modes;
        }
        if (!hasLevel(ECommandLevel.DEV))
            return Collections.emptyList();
        if (args.length == 3 && args[1].equalsIgnoreCase("follow"))
            return Arrays.asList("on", "off");
        if (args.length == 3 && args[1].equalsIgnoreCase("log"))
            return Collections.singletonList("since");
        return Collections.emptyList();
    }

    @Override
    public CommandResult execute(String[] args) {
        String mode = args.length > 1 ? args[1].toLowerCase() : "";
        if (DEV_MODES.contains(mode))
            requireLevel(ECommandLevel.DEV, args[1]);

        Engine engine = Views.engine();
        Console console = engine.console();

        if (args.length == 1)
            return status(console);

        switch (mode) {
            case "log":
                return log(engine.consoleLog(), args);
            case "follow": {
                if (engine.consoleLog() == null)
                    return CommandResult.error("No console log: enable the console or the debug mode");

                boolean on = args.length < 3 || args[2].equalsIgnoreCase("on");
                if (args.length >= 3 && !on && !args[2].equalsIgnoreCase("off"))
                    return unknownArgumentError(args[2]);
                if (!commands.getCurrentSource().setFollowLog(on))
                    return CommandResult.error("The source " + commands.getCurrentSource().name() + " can't follow the console log");

                return CommandResult.ok(on ? "Following the console log" : "Console log no longer followed");
            }
            case "clear":
                if (engine.consoleLog() == null)
                    return CommandResult.error("No console log");
                // Below DEV, only for the source and the levels below: the developer keeps the log
                engine.consoleLog().clear(currentLevel());
                return CommandResult.ok("");
            default:
                break;
        }

        if (console == null)
            return CommandResult.error("The console is disabled (EngineConfig.setCommandAccess)");

        switch (mode) {
            case "open":
                console.open();
                break;
            case "close":
                console.close();
                break;
            case "toggle":
                console.toggle();
                break;
            case "detach":
                console.detach();
                break;
            case "attach":
                console.attach();
                break;
            default:
                return unknownArgumentError(args[1]);
        }

        return status(console);
    }

    private static CommandResult status(Console console) {
        if (console == null)
            return CommandResult.error("The console is disabled (EngineConfig.setCommandAccess)");

        return CommandResult.ok("Console " + console.getMode().name().toLowerCase(),
                new JSONObject().put("mode", console.getMode().name()));
    }

    // console log [n] | console log since ID
    private CommandResult log(ConsoleLog log, String[] args) {
        if (log == null)
            return CommandResult.error("No console log: enable the console or the debug mode");

        List<ConsoleLog.Line> lines;
        try {
            if (args.length >= 4 && args[2].equalsIgnoreCase("since"))
                lines = log.since(Long.parseLong(args[3]));
            else
                lines = log.last(args.length >= 3 ? Integer.parseInt(args[2]) : DEFAULT_LOG_LINES);
        } catch (NumberFormatException e) {
            return CommandResult.error("Not a number : " + e.getMessage());
        }

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

    @Override
    public String help() {
        return super.help() +
                "\nconsole : mode of the in-game console (closed, overlay, detached)" +
                "\nconsole open|close|toggle : the console over the main window (F1)" +
                "\nconsole detach|attach : the console in a window of its own, or back on the main window (F2)" +
                "\nconsole clear : clear the lines of the console" +
                "\nconsole log [n] : the last n lines of the log (" + DEFAULT_LOG_LINES + " by default), with their id" +
                "\nconsole log since ID : the lines written after the line ID" +
                "\nconsole follow [on|off] : (debug server) receive every new line of the log, see DebugServer";
    }
}
