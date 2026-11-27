package io.github.mightycode.mightylib.utils;

/**
 * Receives every problem sent through {@link Logger} (registered by {@link Logger#AddListener}).
 * Called on the thread that sent the problem (main thread, workers, OpenGL driver thread): it must be thread safe and
 * short. An exception it throws removes it from the channel.
 */
public interface ILogListener {
    void onLog(LogEntry entry);
}
