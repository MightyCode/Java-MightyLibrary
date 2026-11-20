package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import org.joml.Matrix4f;

/**
 * The part of the state of a batched draw that comes from its material: batch program, textures, OpenGL state,
 * common values. An immutable snapshot: equals and hashCode compare its content exactly, so that two materials that
 * draw alike give equal parts (interned once by BatchStateTable). Main thread only.
 */
public interface IBatchMaterial {
    /**
     * Before the draws of a run: applies the OpenGL state, uses the batch program, binds the textures and sends the
     * values, then the camera and the color mode of the run.
     *
     * @param linear true for a linear draw (ERenderColorSpace resolved for the draw)
     */
    void apply(boolean linear, Matrix4f projection, Matrix4f view);
}
