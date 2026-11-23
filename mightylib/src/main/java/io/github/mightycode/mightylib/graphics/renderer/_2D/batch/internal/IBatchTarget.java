package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;

/**
 * Where a DrawBatch2D sends its draws: the OpenGL context current when the batch gets its first draw since its last
 * flush. A context draws the batches of its views one after the other: at most one batch has draws waiting in it.
 * Main thread only.
 */
public interface IBatchTarget {
    /**
     * The batch gets its first draw since its last flush: the batch that had draws waiting in this context is
     * flushed first (its draws were asked before).
     */
    void begin(DrawBatch2D batch);

    // The batch was flushed: no draw of it waits in this context anymore
    void ended(DrawBatch2D batch);

    // How the flush is sent (the mode of the batches)
    IBatchUploader uploader();

    // The states of the draws (of the engine of the context)
    BatchStateTable table();

    // Size in pixels of the viewport of the context (margins in pixels of the view culling), 0 when unknown
    default int viewportWidth() {
        return 0;
    }

    default int viewportHeight() {
        return 0;
    }
}
