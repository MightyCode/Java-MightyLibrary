package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
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

    // What its batch program reads: its draws are written in this format (another format is another part)
    BatchFormat format();

    // The format of its instanced batch program (hasInstanced), null when none
    default BatchFormat instancedFormat() {
        return null;
    }

    // Its draws of quads can be drawn by instances now (an instanced batch program, loaded, reading its format)
    default boolean hasInstanced() {
        return false;
    }

    // Like apply, with the instanced batch program (only when hasInstanced)
    default void applyInstanced(boolean linear, Matrix4f projection, Matrix4f view) {
        throw new UnsupportedOperationException("No instanced batch program");
    }
}
