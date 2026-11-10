package io.github.mightycode.mightylib.utils.enginecommand;

/**
 * Told of every submitted command (Commands.submit) and of its result: the console log shows them whatever their source.
 * level: the level of the source when the command runs, the same for its answer even when the command changes the
 * level of its source (cheats off).
 */
public interface ICommandListener {
    // Main thread, just before the execution
    void executed(ICommandSource source, ECommandLevel level, String line);

    // Main thread, or the thread completing a deferred result
    void answered(ICommandSource source, ECommandLevel level, String line, CommandResult result);
}
