package io.github.mightycode.mightylib.utils.enginecommand;

/**
 * Thrown by a command to stop with a readable error (BaseCommand.requireLaunched...): the message is the answer,
 * without the "Command X failed" of an unexpected exception.
 */
public class CommandException extends RuntimeException {
    public CommandException(String message) {
        super(message);
    }
}
