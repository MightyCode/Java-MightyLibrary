package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.CommandScript;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.longValue;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.path;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;

/**
 * Run the commands written in a file, one per line (see CommandScript).
 */
public class ExecGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "exec";
    public static final long DEFAULT_TIMEOUT_SECONDS = 300;

    private final Commands commands;
    // Scripts running, to refuse a script that runs itself (directly or not)
    private final Set<Path> running;

    public ExecGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Run the commands written in a file, one per line, path relative to the working directory");
        this.commands = commands;
        this.running = ConcurrentHashMap.newKeySet();
    }

    @Override
    protected void syntax(CommandNode root) {
        root.option(Option.of("timeout", "Seconds before the script is stopped (" + DEFAULT_TIMEOUT_SECONDS
                        + " by default)").value("seconds", longValue(1, Long.MAX_VALUE))
                        .defaultValue(DEFAULT_TIMEOUT_SECONDS))
                .then(argument("file", path(), "Lines run in the same tick until one answers later (wait, screenshot,"
                        + " exec...), the next line waits for it. Blank lines and lines starting with # are ignored,"
                        + " the script stops at the first failed line")
                        .executes(context -> run(context.get("file"), context.get("timeout"))));
    }

    private CommandResult run(Path file, long timeout) {
        Path path = file.toAbsolutePath().normalize();
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return CommandResult.error("Can't read " + path + " : " + e);
        }

        if (!running.add(path))
            return CommandResult.error(path + " is already running (a script can't run itself)");

        CommandScript script = new CommandScript(commands, path.getFileName().toString(), lines);
        script.getResult().whenComplete((result, exception) -> running.remove(path));

        // The lines that can run now run in this tick, the others at the next ticks
        if (!script.step())
            commands.addTickTask(script::step);

        return CommandResult.later(script.getResult(), timeout);
    }
}
