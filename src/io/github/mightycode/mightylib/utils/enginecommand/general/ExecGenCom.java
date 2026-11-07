package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.CommandScript;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
        super(COMMAND_NAME);
        this.commands = commands;
        this.running = ConcurrentHashMap.newKeySet();
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 2 || args.length > 3)
            return noEnoughArgumentError();

        long timeout = DEFAULT_TIMEOUT_SECONDS;
        if (args.length > 2) {
            try {
                timeout = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                return CommandResult.error("Timeout must be a number of seconds : " + args[2]);
            }

            if (timeout <= 0)
                return CommandResult.error("Timeout must be positive : " + timeout);
        }

        Path path = Paths.get(args[1]).toAbsolutePath().normalize();
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

    @Override
    public String help() {
        return super.help() +
                "\nexec [file] [timeout] : run the commands of a file, one per line, path relative to the working directory" +
                "\nLines run in the same tick until one answers later (wait, screenshot, exec...), the next line waits for it." +
                "\nBlank lines and lines starting with # are ignored, the script stops at the first failed line." +
                "\nUse -> wait [ticks] <- and -> wait loaded <- to let the game run." +
                "\ntimeout : seconds before the script is stopped (" + DEFAULT_TIMEOUT_SECONDS + " by default)";
    }
}
