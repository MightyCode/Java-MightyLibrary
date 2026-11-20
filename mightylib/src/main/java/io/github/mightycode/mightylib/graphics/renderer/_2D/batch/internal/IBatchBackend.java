package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;

/**
 * What the batches of an engine share: the mode, and the target of the current OpenGL context. Main thread only.
 */
public interface IBatchBackend {
    // The target of the OpenGL context current now
    IBatchTarget target();

    EBatch2DMode mode();
}
