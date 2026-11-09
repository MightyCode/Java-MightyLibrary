package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandListener;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandSource;

/**
 * Writes every submitted command and its result in the console log, whatever its source (in-game console, standard
 * input, debug server): what is typed elsewhere is seen in the game, and followed by the debug server clients.
 */
public final class CommandEcho implements ICommandListener {
    private final ConsoleLog log;

    public CommandEcho(ConsoleLog log) {
        this.log = log;
    }

    @Override
    public void executed(ICommandSource source, String line) {
        log.add(ConsoleLog.EType.COMMAND, source.name(), line);
    }

    @Override
    public void answered(ICommandSource source, String line, CommandResult result) {
        String text = result.toString();
        if (text == null || text.isEmpty())
            return;

        log.add(result.isSuccess() ? ConsoleLog.EType.RESULT : ConsoleLog.EType.ERROR, source.name(), text);
    }
}
