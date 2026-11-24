package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;

/**
 * What the batches of an engine share: the mode, the view culling, and the target of the current OpenGL context. Main thread only.
 */
public interface IBatchBackend {
    // The target of the OpenGL context current now
    IBatchTarget target();

    EBatch2DMode mode();

    // The frame of the engine being displayed, or the last one displayed (0 when frames are not counted)
    default long frame() {
        return 0;
    }

    // The tools to check the view culling by eye (command batch cull), null without
    default ViewCullTools cullTools() {
        return null;
    }

    // The view culling of the batched draws (DrawBatch2D.isCulled): on by default
    default boolean viewCulling() {
        return true;
    }
}
