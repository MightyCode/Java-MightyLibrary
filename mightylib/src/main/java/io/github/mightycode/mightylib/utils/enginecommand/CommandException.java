package io.github.mightycode.mightylib.utils.enginecommand;

/**
 * Thrown by a command to stop with a readable error (a handler, a condition...): the message is the answer,
 * without the "Command X failed" of an unexpected exception.
 */
public class CommandException extends RuntimeException {
    public CommandException(String message) {
        super(message);
    }
}
