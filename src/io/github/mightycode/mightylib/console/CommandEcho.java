package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandListener;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandSource;

/**
 * Writes every submitted command and its result in the console log, whatever its source (in-game console, standard
 * input, debug server): what is typed elsewhere is seen in the game, and followed by the debug server clients.
 * The command and its message are seen from the level of the source on, the data (details for the developer) by DEV.
 */
public final class CommandEcho implements ICommandListener {
    private final ConsoleLog log;

    public CommandEcho(ConsoleLog log) {
        this.log = log;
    }

    @Override
    public void executed(ICommandSource source, ECommandLevel level, String line) {
        log.add(ConsoleLog.EType.COMMAND, source.name(), line, level);
    }

    @Override
    public void answered(ICommandSource source, ECommandLevel level, String line, CommandResult result) {
        ConsoleLog.EType type = result.isSuccess() ? ConsoleLog.EType.RESULT : ConsoleLog.EType.ERROR;

        String message = result.getMessage();
        if (message != null && !message.isEmpty())
            log.add(type, source.name(), message, level);

        if (result.getData() != null)
            log.add(type, source.name(), result.getData().toString(2), ECommandLevel.DEV);
    }
}
