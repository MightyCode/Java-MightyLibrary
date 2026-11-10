package io.github.mightycode.mightylib.utils.enginecommand;

/**
 * Told of every submitted command (Commands.submit) and of its result: the console log shows them whatever their source.
 * level: the level of the source when the command runs. A command that lowers the level of its source (cheats off) is
 * then told to lowered, and answered with the lower level: the source still sees the line that lowered its level.
 */
public interface ICommandListener {
    // Main thread, just before the execution
    void executed(ICommandSource source, ECommandLevel level, String line);

    // Main thread, right after the execution of the command last told to executed (before any other): its source now
    // has a lower level
    default void lowered(ICommandSource source, ECommandLevel level, String line) {}

    // Main thread, or the thread completing a deferred result
    void answered(ICommandSource source, ECommandLevel level, String line, CommandResult result);
}
