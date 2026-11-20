package io.github.mightycode.mightylib.graphics.internal;

/**
 * Draws asked in a context and not sent yet (the 2D batches): sent before what must come after them in this context
 * (a draw alone, a clear, a framebuffer, another context made current). Main thread only.
 */
public interface IPendingDraws {
    /** Sends the waiting draws, if any; cause names what needs them sent (a class, or a name). */
    void flush(Object cause);

    // The context is destroyed: its waiting draws are dropped, its own objects died with it
    void contextDestroyed();
}
