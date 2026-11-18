package io.github.mightycode.mightylib.test;

/**
 * An engine with a window can't start here: no screen (no display server), or no OpenGL 3.3 core context.
 * The JUnit extension turns it into a skipped test with this reason.
 */
public final class GLUnavailableException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    GLUnavailableException(String message) {
        super(message);
    }
}
