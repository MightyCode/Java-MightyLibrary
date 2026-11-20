package io.github.mightycode.mightylib.graphics.internal;

/**
 * The raw OpenGL calls behind GLStateCache: the real one calls OpenGL, a test one records the calls.
 * Each method is called only when the state of the context changes (or is unknown).
 */
public interface IGLStateApplier {
    /** glEnable / glDisable of one of the capabilities of GLStateCache (BLEND, DEPTH_TEST…). */
    void capability(int capability, boolean enabled);

    void blendFunc(int source, int destination);

    void depthMask(boolean writable);

    void scissor(int x, int y, int width, int height);

    void useProgram(int program);

    /** glActiveTexture(GL_TEXTURE0 + unit). */
    void activeTexture(int unit);

    /** glBindTexture on the active unit. */
    void bindTexture(int target, int texture);
}
