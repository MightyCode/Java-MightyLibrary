package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.internal.IGLSharedState;
import io.github.mightycode.mightylib.graphics.internal.IPendingDraws;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TextureData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * The 2D batches of one engine: the mode, the table of the states, and the batch context of each OpenGL context
 * (created when a batch first starts in it). Lives in the GLResources of the engine. Main thread only.
 */
public final class Batches2D implements IBatchBackend, IGLSharedState {
    // Cause of the barrier when the mode changes
    public static final String CAUSE_MODE = "mode";

    private final BatchStateTable table = new BatchStateTable();
    private EBatch2DMode mode;
    // Frames of the engine (frameStarted): the views displayed in the last one
    private long frame;
    // Its references are dropped with the GL elements when GLResources is released
    private Texture whiteTexture;

    private final List<BatchContext> contexts = new ArrayList<>();
    private BatchContext last;
    // Of destroyed contexts: their buffers are deleted once another context is current
    private final List<BatchContext> orphaned = new ArrayList<>();
    // The batches of the views and their names (weak: a view no longer used takes its batch with it)
    private final Map<DrawBatch2D, String> views = new WeakHashMap<>();

    private Batches2D(EBatch2DMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    /** The batches of this engine, created with the mode of its configuration at the first call. */
    public static Batches2D of(Engine engine) {
        return engine.glResources().shared(Batches2D.class, () -> new Batches2D(engine.config().getBatch2DMode()));
    }

    // The batches of the engine of these GL resources, created at its start
    public static Batches2D of(GLResources glResources) {
        return glResources.shared(Batches2D.class, () -> {
            throw new IllegalStateException("The 2D batches are created at the start of the engine");
        });
    }

    /**
     * The white texture of the engine (EngineResources.WHITE_TEXTURE), referenced from the start of the engine to
     * its end: unit 0 of a batched draw whose program reads no texture, or whose material has none.
     */
    public Texture whiteTexture() {
        if (whiteTexture == null)
            throw new IllegalStateException("The white texture is taken once the resources of the engine are loaded");
        return whiteTexture;
    }

    // Once the batch of the engine is loaded (its white texture), before any scene draws
    public void acquireWhiteTexture(GLResources glResources, Resources resources) {
        if (whiteTexture == null)
            whiteTexture = (Texture) glResources.acquire(
                    resources.getResourceOrFallback(TextureData.class, EngineResources.WHITE_TEXTURE), true);
    }

    public BatchStateTable table() {
        return table;
    }

    // A frame of the engine starts, before its windows are displayed
    public void frameStarted() {
        ++frame;
        table.frameStarted();
    }

    @Override
    public long frame() {
        return frame;
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

    /** A batch of a view of this engine, named for the command batch (forgotten with its view). */
    public DrawBatch2D createBatch(String viewName) {
        DrawBatch2D batch = new DrawBatch2D(this);
        views.put(batch, viewName);
        return batch;
    }

    /**
     * For the command batch: the mode, the table of the states, the contexts, and each view with a batch (the
     * counters of its last frame).
     */
    public JSONObject describe() {
        JSONArray viewArray = new JSONArray();
        for (Map.Entry<DrawBatch2D, String> view : views.entrySet())
            viewArray.put(view.getKey().describe().put("view", view.getValue()));

        JSONArray contextArray = new JSONArray();
        for (BatchContext batchContext : contexts)
            contextArray.put(new JSONObject().put("context", batchContext.context().getName())
                    .put("waitingView", batchContext.active() == null ? JSONObject.NULL
                            : views.getOrDefault(batchContext.active(), "?"))
                    .put("ringBlocks", batchContext.pendingRingBlocks()));

        return new JSONObject()
                .put("mode", mode.optionName())
                .put("table", new JSONObject().put("materialParts", table.materialParts())
                        .put("materialPartsThreshold", table.materialPartsThreshold())
                        .put("cameras", table.cameras()).put("generation", table.generation())
                        .put("partsGeneration", table.partsGeneration()).put("resets", table.resets()))
                .put("contexts", contextArray)
                .put("views", viewArray);
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
        whiteTexture = null;
        table.delete();
    }
}
