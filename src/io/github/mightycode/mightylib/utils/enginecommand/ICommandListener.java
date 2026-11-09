package io.github.mightycode.mightylib.utils.enginecommand;

/**
 * Told of every submitted command (Commands.submit) and of its result: the console log shows them whatever their source.
 */
public interface ICommandListener {
    // Main thread, just before the execution
    void executed(ICommandSource source, String line);

    // Main thread, or the thread completing a deferred result
    void answered(ICommandSource source, String line, CommandResult result);
}
