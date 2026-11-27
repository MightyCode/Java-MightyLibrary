package io.github.mightycode.mightylib.utils;

/** Severity of a problem sent through {@link Logger}. */
public enum ELogLevel {
    /** Invalid data or setting the engine expects and recovers from (unknown property, fallback resource). */
    WARNING,
    /** Bug: incoherent state, unexpected exception, OpenGL or OpenAL error. */
    ERROR
}
