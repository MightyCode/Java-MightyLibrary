package io.github.mightycode.mightylib.utils;

import java.io.PrintStream;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.opengl.GL20.*;

/**
 * Channel of the problems of the process (static like {@link io.github.mightycode.mightylib.main.Workers}: code without
 * access to the engine writes to it). Each problem prints one "--" line to {@link System#err} and is sent to the
 * listeners (engine command {@code problems}, test kit, crash report of a game). Information goes to System.out.
 */
public final class Logger {
    // Categories of the engine; a game gives its own
    public static final String CATEGORY_ENGINE = "engine";
    public static final String CATEGORY_GL = "gl";
    public static final String CATEGORY_SOUND = "sound";
    public static final String CATEGORY_RESOURCES = "resources";
    public static final String CATEGORY_SHADERS = "shaders";
    public static final String CATEGORY_INPUT = "input";
    public static final String CATEGORY_CONFIG = "config";

    private static final String PREFIX = "--";

    // Read on every problem, written rarely: no lock on the sending thread
    private static final List<ILogListener> LISTENERS = new CopyOnWriteArrayList<>();
    // A listener that logs while it receives a problem would loop: its problems are only printed
    private static final ThreadLocal<boolean[]> DISPATCHING = ThreadLocal.withInitial(() -> new boolean[1]);

    // glGetError after the sensitive GL calls, GL objects created on the main thread only (EngineConfig.setOpenGLErrorCheck).
    // Read from any thread (GenDeleteResources checks the calling thread)
    private static volatile boolean glErrorCheck = true;

    private Logger() {
    }

    public static boolean isGLErrorCheck() {
        return glErrorCheck;
    }

    public static void setGLErrorCheck(boolean glErrorCheck) {
        Logger.glErrorCheck = glErrorCheck;
    }

    public static String getCurrentStackTrace() {
        return Thread.currentThread().getStackTrace()[2].toString();
    }

    // ---- Listeners ----

    /** Adds a listener of every problem, from any thread; added twice, it receives each problem twice. */
    public static void AddListener(ILogListener listener) {
        LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Removes the listener (one registration), from any thread; true if it was registered. */
    public static boolean RemoveListener(ILogListener listener) {
        return LISTENERS.remove(listener);
    }

    // ---- Problems ----

    /** Bug: incoherent state, value that cannot happen, error of a library. */
    public static void Error(String category, String message) {
        Log(ELogLevel.ERROR, category, message, null);
    }

    /** Bug that carries an exception: the line, then the stack trace. */
    public static void Error(String category, String message, Throwable error) {
        Log(ELogLevel.ERROR, category, message, error);
    }

    /** Invalid data or setting the engine expects and recovers from. */
    public static void Warn(String category, String message) {
        Log(ELogLevel.WARNING, category, message, null);
    }

    /**
     * Unexpected exception that is handled (the engine goes on): one "--" line saying what failed, then the stack trace.
     * Error of the category {@link #CATEGORY_ENGINE}. Expected failures (wrong data, missing file reported to the user)
     * are a {@link #Warn} instead.
     */
    public static void LogException(String message, Throwable error) {
        Log(ELogLevel.ERROR, CATEGORY_ENGINE, message, Objects.requireNonNull(error, "error"));
    }

    /** Sends a problem: prints its line to System.err, then gives it to each listener. */
    // A listener that throws is removed while the list is iterated: the iterator of a CopyOnWriteArrayList reads a copy
    @SuppressWarnings("ModifyCollectionInEnhancedForLoop")
    public static void Log(ELogLevel level, String category, String message, Throwable error) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(message, "message");

        Print(System.err, message, error);
        if (LISTENERS.isEmpty())
            return;

        boolean[] dispatching = DISPATCHING.get();
        if (dispatching[0])
            return;

        LogEntry entry = new LogEntry(level, category, message, error, Thread.currentThread().getName(), Instant.now());
        dispatching[0] = true;
        try {
            for (ILogListener listener : LISTENERS) {
                try {
                    listener.onLog(entry);
                } catch (RuntimeException | Error e) {
                    // Out of the channel: printed raw, never sent again
                    LISTENERS.remove(listener);
                    System.err.println(PREFIX + "Log listener removed, it threw: " + e);
                    e.printStackTrace(System.err);
                }
            }
        } finally {
            dispatching[0] = false;
        }
    }

    private static void Print(PrintStream stream, String message, Throwable error) {
        if (error == null) {
            stream.println(PREFIX + message);
            return;
        }

        // The line and its stack trace stay together when several threads print
        synchronized (stream) {
            stream.println(PREFIX + message + ": " + error);
            error.printStackTrace(stream);
        }
    }

    // ---- OpenGL ----

    /** glGetError: an error is sent as an {@link ELogLevel#ERROR} of the category gl, then thrown. */
    public static void CheckOpenGLError(String message) {
        if (!glErrorCheck)
            return;

        int error = glGetError();
        if (error != GL_NO_ERROR) {
            Error(CATEGORY_GL, message + " Error: " + error);

            throw new RuntimeException(getCurrentStackTrace());
        }
    }

    public static void CheckOpenGLError() {
        CheckOpenGLError("");
    }
}
