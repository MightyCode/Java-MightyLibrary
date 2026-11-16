package io.github.mightycode.mightylib.main;

/**
 * DEBUG: console, commands, debug server, statistics in the title, strict resources available.
 * RELEASE: none of them; a resource that fails is replaced by its fallback with a log line.
 */
public enum EEngineMode {
    DEBUG,
    RELEASE
}
