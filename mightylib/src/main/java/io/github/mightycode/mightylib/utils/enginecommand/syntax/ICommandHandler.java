package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;

/**
 * What a branch of a command does, with the values already read and typed. Main thread, under the owner of the
 * command. A CommandException stops it with its message as the error.
 */
@FunctionalInterface
public interface ICommandHandler {
    CommandResult run(CommandContext context);
}
