package io.github.mightycode.mightylib.resources;

/**
 * Expected failure of a resource: the data is wrong (missing or unreadable file, bad format, malformed content),
 * not the code. Logged on one line, without stack trace; any other exception thrown while loading is a bug.
 */
public class ResourceException extends RuntimeException {
    public ResourceException(String message) {
        super(message, null, false, false);
    }

    // The cause is only kept for its message: no stack trace either
    public ResourceException(String message, Throwable cause) {
        super(message + (cause == null || cause.getMessage() == null ? "" : ": " + cause.getMessage()),
                cause, false, false);
    }
}
