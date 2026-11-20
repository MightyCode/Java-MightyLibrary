package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.internal.IGLSharedState;
import io.github.mightycode.mightylib.graphics.internal.IPendingDraws;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.Engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The 2D batches of one engine: the mode, the table of the states, and the batch context of each OpenGL context
 * (created when a batch first starts in it). Lives in the GLResources of the engine. Main thread only.
 */
public final class Batches2D implements IBatchBackend, IGLSharedState {
    // Cause of the barrier when the mode changes
    public static final String CAUSE_MODE = "mode";

    private final BatchStateTable table = new BatchStateTable();
    private EBatch2DMode mode;

    private final List<BatchContext> contexts = new ArrayList<>();
    private BatchContext last;
    // Of destroyed contexts: their buffers are deleted once another context is current
    private final List<BatchContext> orphaned = new ArrayList<>();

    private Batches2D(EBatch2DMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    /** The batches of this engine, created with the mode of its configuration at the first call. */
    public static Batches2D of(Engine engine) {
        return of(engine.glResources(), engine.config().getBatch2DMode());
    }

    // The mode is the one at the creation only
    public static Batches2D of(GLResources glResources, EBatch2DMode mode) {
        return glResources.shared(Batches2D.class, () -> new Batches2D(mode));
    }

    public BatchStateTable table() {
        return table;
    }

    @Override
    public EBatch2DMode mode() {
        return mode;
    }

    /**
     * Takes effect at once: the draws waiting in the current context are sent first, the others at their flush with
     * the new mode. The buffers of the previous mode are deleted, those of the new one created in the current context
     * (the GL objects of the engine stay the same whatever the modes it went through).
     */
    public void setMode(EBatch2DMode mode) {
        Objects.requireNonNull(mode, "mode");
        if (mode == this.mode)
            return;

        GLContext.FlushCurrentDraws(CAUSE_MODE);
        this.mode = mode;
        if (GLContext.currentOrNull() == null)
            return;

        for (BatchContext batchContext : contexts)
            batchContext.keepUploaderOf(mode);
        prepare();
    }

    /**
     * The batch context of the current OpenGL context and the buffers of the mode, created now (at the start of the
     * engine, at the opening of a context): what a game draws later creates no GL object.
     */
    public void prepare() {
        BatchContext batchContext = target();
        if (mode.isBatched())
            batchContext.uploader();
    }

    @Override
    public BatchContext target() {
        if (!orphaned.isEmpty())
            deleteOrphaned();

        GLContext context = GLContext.current();
        if (last != null && last.context() == context)
            return last;

        for (BatchContext batchContext : contexts) {
            if (batchContext.context() == context) {
                last = batchContext;
                return batchContext;
            }
        }

        IPendingDraws other = context.pendingDraws();
        if (other != null)
            throw new IllegalStateException("The OpenGL context " + context.getName()
                    + " already has draws of another engine");

        BatchContext created = new BatchContext(context, this);
        context.setPendingDraws(created);
        contexts.add(created);
        last = created;
        return created;
    }

    // The batch contexts alive (command batch)
    public List<BatchContext> contexts() {
        return new ArrayList<>(contexts);
    }

    void contextDestroyed(BatchContext batchContext) {
        contexts.remove(batchContext);
        if (last == batchContext)
            last = null;
        orphaned.add(batchContext);
    }

    private void deleteOrphaned() {
        for (BatchContext batchContext : orphaned)
            batchContext.delete(false);
        orphaned.clear();
    }

    // Called at the frames where nothing is sent (main window current): the buffers of the closed contexts go then
    @Override
    public void loadingIdle() {
        if (!orphaned.isEmpty() && GLContext.currentOrNull() != null)
            deleteOrphaned();
    }

    // GLResources released: the GL objects of every context (none when no context is current: freed with them)
    @Override
    public void delete() {
        boolean canDelete = GLContext.currentOrNull() != null;
        if (canDelete)
            deleteOrphaned();

        for (BatchContext batchContext : contexts) {
            if (batchContext.active() != null)
                batchContext.active().discard();
            if (canDelete)
                batchContext.delete(!batchContext.context().isDestroyed());
            if (batchContext.context().pendingDraws() == batchContext)
                batchContext.context().setPendingDraws(null);
        }

        contexts.clear();
        orphaned.clear();
        last = null;
        table.delete();
    }
}
