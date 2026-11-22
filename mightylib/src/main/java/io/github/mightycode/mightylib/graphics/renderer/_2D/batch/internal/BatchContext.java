package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.internal.IPendingDraws;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;

/**
 * The 2D batches in one OpenGL context: the batch whose draws wait in it (one at a time: a view that starts a batch
 * sends the draws of the previous one first), and the buffers they are sent with, created at the first flush of
 * each mode. The pending draws of the context (GLContext.flushDraws): sent before a draw alone, a clear, a
 * framebuffer, another context made current. Main thread only.
 */
public final class BatchContext implements IBatchTarget, IPendingDraws {
    // Cause of the barrier when the batch of another view of the context starts
    public static final String CAUSE_VIEW = "view";

    private final GLContext context;
    private final Batches2D owner;
    private DrawBatch2D active;

    // Modes ORPHAN and INSTANCED
    private OrphanUploader orphan;
    private RingUploader ring;

    BatchContext(GLContext context, Batches2D owner) {
        this.context = context;
        this.owner = owner;
    }

    GLContext context() {
        return context;
    }

    // The batch whose draws wait in this context, null when none
    public DrawBatch2D active() {
        return active;
    }

    @Override
    public void begin(DrawBatch2D batch) {
        if (active != null && active != batch)
            active.barrier(CAUSE_VIEW);
        active = batch;
    }

    @Override
    public void ended(DrawBatch2D batch) {
        if (active == batch)
            active = null;
    }

    @Override
    public IBatchUploader uploader() {
        if (owner.mode() == EBatch2DMode.RING) {
            if (ring == null) {
                ring = new RingUploader();
                ring.prepare(owner.formats());
            }
            return ring;
        }

        if (orphan == null) {
            orphan = new OrphanUploader();
            orphan.prepare(owner.formats());
        }
        return orphan;
    }

    @Override
    public BatchStateTable table() {
        return owner.table();
    }

    @Override
    public void flush(Object cause) {
        if (active != null)
            active.barrier(cause);
    }

    // The draws waiting are dropped, the vertex arrays died with the context: the buffers wait for another context
    @Override
    public void contextDestroyed() {
        if (active != null)
            active.discard();
        owner.contextDestroyed(this);
    }

    // The mode changed: the buffers of the other modes are deleted (a context of the engine is current)
    void keepUploaderOf(EBatch2DMode mode) {
        if (mode != EBatch2DMode.RING && ring != null) {
            ring.delete();
            ring = null;
        }

        if ((mode == EBatch2DMode.RING || mode == EBatch2DMode.OFF) && orphan != null) {
            orphan.delete();
            orphan = null;
        }
    }

    // With a context of the engine current: the buffers; the vertex arrays too when their context is alive
    void delete(boolean contextAlive) {
        if (orphan != null) {
            if (contextAlive)
                orphan.delete();
            else
                orphan.deleteBuffers();
            orphan = null;
        }

        if (ring != null) {
            if (contextAlive)
                ring.delete();
            else
                ring.deleteBuffers();
            ring = null;
        }
    }

    // Blocks of the ring waiting for the GPU (command batch, tests)
    public int pendingRingBlocks() {
        return ring == null ? 0 : ring.pendingBlocks();
    }
}
