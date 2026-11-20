package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchStateTable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchBackend;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchTarget;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchUploader;
import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.scenes.camera.Camera;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EFlip;
import org.apiguardian.api.API;
import org.joml.Matrix4f;
import org.joml.Vector2fc;
import org.joml.Vector4fc;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * The 2D draws of a view, sent together: each batched draw writes its vertices (already transformed, in the fixed
 * format of VERTEX_BYTES) when it is asked, then the draws are sent at the next barrier, neighbors of the same state
 * in a single draw call. By default in the order they were asked (nothing sorted, a constant cost per draw); with a
 * sort key (setDrawSortKey), in the order of their keys inside each segment between two barriers.
 * <p>
 * Barriers (the draws asked before are sent): a renderer drawn alone, a clear of the view, a scissor, a framebuffer
 * bound, flush(), the end of the display of the view, too many draws (MAX_DRAWS). Code that calls OpenGL itself
 * between draws calls flush() first (like RenderState.invalidateContextState after).
 * <p>
 * Low level, for a renderer of its own: beginDraw with a state, vertex and index, endDraw (or quad). Main thread only.
 */
public final class DrawBatch2D {
    /** Bytes of a vertex: position (2 floats, transformed), texture coordinates (2 floats), color (RGBA8). */
    public static final int VERTEX_BYTES = 20;
    /** Low bits of a sort key replaced by the index of the draw in its segment (IDrawSortKey). */
    public static final int CALL_INDEX_BITS = 24;
    public static final long CALL_INDEX_MASK = (1L << CALL_INDEX_BITS) - 1;
    /** Most draws of a segment: one more flushes the batch first (a barrier). */
    public static final int MAX_DRAWS = 1 << CALL_INDEX_BITS;

    // Causes of the barriers that are not a renderer
    public static final String CAUSE_FLUSH = "flush";
    public static final String CAUSE_FULL = "full";
    public static final String CAUSE_SORT_KEY = "sortKey";

    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private static final int[] QUAD_INDICES = {0, 3, 2, 0, 2, 1};

    // The mode, and the target of the context current when the batch starts
    private final IBatchBackend backend;
    // Set while the batch is started: from its first draw (or state) to its flush
    private IBatchTarget target;
    private BatchStateTable table;

    private ByteBuffer vertices = BufferUtils.createByteBuffer(256 * VERTEX_BYTES);
    private int vertexCount;
    // Indices of the vertices, in the order of the draws
    private IntBuffer indices = BufferUtils.createIntBuffer(384);
    private int indexCount;

    // The draw being asked
    private boolean inDraw;
    private int drawState;
    private int drawLayer;
    private float drawDepth;
    private int drawFirstVertex;
    private int drawFirstIndex;

    // Draws of the segment
    private int drawCount;

    // Draw calls of the segment: in the order of the draws, built while they are asked; sorted, built at the flush
    private int[] runStates = new int[64];
    private int[] runFirsts = new int[64];
    private int[] runCounts = new int[64];
    private int runCount;

    // Sorted segment: the draws are kept, then sorted at the flush
    private IDrawSortKey sortKey;
    private int[] entryStates = new int[0];
    private int[] entryLayers = new int[0];
    private float[] entryDepths = new float[0];
    private int[] entryFirsts = new int[0];
    private int[] entryCounts = new int[0];
    private long[] keys = new long[0];
    private IntBuffer sortedIndices;

    // Counters of the current frame of the view, and of the last one (rolled by endView)
    private final FrameCounters current = new FrameCounters();
    private final FrameCounters last = new FrameCounters();

    // While the draws are sent: a barrier asked by what the sending does is not one
    private boolean sending;

    /** A batch of a view (Scene, WindowView), with the mode and the targets of its engine. */
    @API(status = INTERNAL)
    public DrawBatch2D(IBatchBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    /** How the draws are sent now (the mode of the engine). */
    public EBatch2DMode mode() {
        return backend.mode();
    }

    /** False in mode OFF: every renderer draws alone. */
    public boolean isEnabled() {
        return backend.mode().isBatched();
    }

    // ---- Order ----

    /**
     * The order of the draws (see IDrawSortKey), null for the order they are asked (the default). The draws waiting
     * are sent first.
     */
    public DrawBatch2D setDrawSortKey(IDrawSortKey sortKey) {
        if (sortKey != this.sortKey) {
            barrier(CAUSE_SORT_KEY);
            this.sortKey = sortKey;
        }

        return this;
    }

    public IDrawSortKey getDrawSortKey() {
        return sortKey;
    }

    // ---- Draws ----

    /**
     * The table of the states, with the batch started: the states of its draws are asked after this call (a
     * started batch keeps the ids valid until its flush).
     */
    @API(status = INTERNAL)
    public BatchStateTable stateTable() {
        start();
        return table;
    }

    private void start() {
        if (target != null)
            return;

        IBatchTarget newTarget = backend.target();
        // Flushes the batch that had draws waiting in this context: done before this one is started
        newTarget.begin(this);
        target = newTarget;
        table = newTarget.table();
        table.batchStarted();
    }

    /** A draw of this state, layer 0 and depth 0: its vertices and indices follow, then endDraw. */
    public void beginDraw(int state) {
        beginDraw(state, 0, 0f);
    }

    /**
     * A draw of this state (asked to this batch, see stateTable): its vertices and indices follow, then endDraw.
     * Layer and depth are read by the sort key only.
     */
    public void beginDraw(int state, int layer, float depth) {
        if (inDraw)
            throw new IllegalStateException("beginDraw before the endDraw of the previous draw");
        if (drawCount == MAX_DRAWS) {
            // The batch stays started: the state asked by the caller stays valid
            current.countBarrier(CAUSE_FULL, sortKey != null);
            EngineStats.countBatchBarrier();
            send(false);
        }

        start();
        if (!table.isCurrent(state))
            throw new IllegalArgumentException("State " + state + " unknown to the table of the batch: asked before"
                    + " a reset of the table (keep a state only until the next barrier), or not a state");

        inDraw = true;
        drawState = state;
        drawLayer = layer;
        drawDepth = depth;
        drawFirstVertex = vertexCount;
        drawFirstIndex = indexCount;
    }

    /**
     * A vertex of the draw: position (transformed: world units of the camera of the state), texture coordinates, and
     * color as RGBA in an int (Color4f.toIntRGBA), multiplied with the texture by the batch shader.
     */
    public void vertex(float x, float y, float u, float v, int rgba) {
        if (!inDraw)
            throw new IllegalStateException("vertex outside of beginDraw and endDraw");

        putVertex(x, y, u, v, rgba);
    }

    private void putVertex(float x, float y, float u, float v, int rgba) {
        if ((vertexCount + 1) * VERTEX_BYTES > vertices.capacity())
            vertices = grow(vertices, vertexCount * VERTEX_BYTES, vertices.capacity() * 2);

        int at = vertexCount * VERTEX_BYTES;
        vertices.putFloat(at, x);
        vertices.putFloat(at + 4, y);
        vertices.putFloat(at + 8, u);
        vertices.putFloat(at + 12, v);
        // In memory R, G, B, A: read as 4 normalized bytes
        vertices.putInt(at + 16, LITTLE_ENDIAN ? Integer.reverseBytes(rgba) : rgba);
        ++vertexCount;
    }

    /** An index of a triangle of the draw: a vertex of this draw, 0 for its first one. */
    public void index(int vertexOfDraw) {
        if (!inDraw)
            throw new IllegalStateException("index outside of beginDraw and endDraw");

        int vertex = drawFirstVertex + vertexOfDraw;
        if (vertexOfDraw < 0 || vertex >= vertexCount)
            throw new IllegalArgumentException("Index " + vertexOfDraw + " of a draw of " + (vertexCount - drawFirstVertex)
                    + " vertices: the vertices come before the indices");

        putIndex(vertex);
    }

    private void putIndex(int vertex) {
        if (indexCount == indices.capacity())
            indices = grow(indices, indexCount, indices.capacity() * 2);

        indices.put(indexCount++, vertex);
    }

    /** Ends the draw: a draw without index draws nothing (its vertices are dropped). */
    public void endDraw() {
        if (!inDraw)
            throw new IllegalStateException("endDraw without beginDraw");

        inDraw = false;
        int count = indexCount - drawFirstIndex;
        if (count == 0) {
            vertexCount = drawFirstVertex;
            return;
        }

        if (sortKey == null) {
            // Same state as the previous draw: the same draw call
            if (runCount > 0 && runStates[runCount - 1] == drawState)
                runCounts[runCount - 1] += count;
            else
                addRun(drawState, drawFirstIndex, count);
        } else {
            addEntry(count);
        }

        ++drawCount;
        ++current.draws;
    }

    /**
     * A quad of this state: its corners in order around it (triangles 0 3 2 and 0 2 1, like RectangularFace), each
     * with its texture coordinates, one color.
     */
    public void quad(int state, int layer, float depth,
                     float x0, float y0, float u0, float v0,
                     float x1, float y1, float u1, float v1,
                     float x2, float y2, float u2, float v2,
                     float x3, float y3, float u3, float v3,
                     int rgba) {
        beginDraw(state, layer, depth);
        putVertex(x0, y0, u0, v0, rgba);
        putVertex(x1, y1, u1, v1, rgba);
        putVertex(x2, y2, u2, v2, rgba);
        putVertex(x3, y3, u3, v3, rgba);
        for (int index : QUAD_INDICES)
            putIndex(drawFirstVertex + index);
        endDraw();
    }

    private void addRun(int state, int first, int count) {
        if (runCount == runStates.length) {
            int size = runStates.length * 2;
            runStates = Arrays.copyOf(runStates, size);
            runFirsts = Arrays.copyOf(runFirsts, size);
            runCounts = Arrays.copyOf(runCounts, size);
        }

        runStates[runCount] = state;
        runFirsts[runCount] = first;
        runCounts[runCount] = count;
        ++runCount;
    }

    private void addEntry(int count) {
        if (drawCount == entryStates.length) {
            int size = Math.max(64, entryStates.length * 2);
            entryStates = Arrays.copyOf(entryStates, size);
            entryLayers = Arrays.copyOf(entryLayers, size);
            entryDepths = Arrays.copyOf(entryDepths, size);
            entryFirsts = Arrays.copyOf(entryFirsts, size);
            entryCounts = Arrays.copyOf(entryCounts, size);
            keys = Arrays.copyOf(keys, size);
        }

        entryStates[drawCount] = drawState;
        entryLayers[drawCount] = drawLayer;
        entryDepths[drawCount] = drawDepth;
        entryFirsts[drawCount] = drawFirstIndex;
        entryCounts[drawCount] = indexCount - drawFirstIndex;
        // Computed when the draw is asked: the function reads values that may change before the flush
        keys[drawCount] = (sortKey.key(drawLayer, drawDepth, drawState) & ~CALL_INDEX_MASK) | drawCount;
    }

    // ---- Drawing without renderer ----

    /** The state of draws of this material with the 2D camera of the current view (see state(Material, Camera)). */
    public int state(Material material) {
        return state(material, Views.current().camera2D());
    }

    /**
     * The state of draws of this material seen through this camera, in the color mode of the material and the
     * current view: -1 when the material can't be batched now (its shader or the batch shader still loading, or a
     * shader without batch equivalent, "batch" in shaders.json). Valid until the next barrier: asked again after.
     */
    public int state(Material material, Camera camera) {
        Objects.requireNonNull(material, "material");
        BatchStateTable stateTable = stateTable();
        int part = material.batchPart(stateTable);
        if (part < 0)
            return -1;

        // The matrices of the camera themselves (the getters of matrices give copies)
        Matrix4f projection = (Matrix4f) camera.getProjection().getObject();
        Matrix4f view = (Matrix4f) camera.getView().getObject();
        IView current = Views.current();
        boolean linear = current.engine().colors().resolve(material.getColorSpace(), current.colorSpace()).isLinear();
        return stateTable.state(part, stateTable.camera(projection, view), linear);
    }

    /**
     * A rectangle of the main texture of the material (the texture a texture shader reads; colorShape2D reads
     * none: a colored rectangle), like a RectangleRenderer: size in world units, its reference point (0 to 1 of its
     * size, 0 0 for the top left corner) placed at position, turned by rotation (radians) around it, the part of
     * the texture region (u0, v0, u1, v1 in its x, y, z, w; null for the whole texture), flipped like
     * RectangleRenderer.setTextureFlip, multiplied by color (null for white). False when the material can't be
     * batched now (nothing drawn).
     */
    public boolean drawRegion(Material material, Vector2fc position, Vector2fc size, Vector2fc reference,
                              float rotation, Vector4fc region, EFlip flip, Color4f color) {
        return drawRegion(material, position, size, reference, rotation, region, flip, color, 0, 0f);
    }

    // The same with the layer and the depth of the draw, for the sort key of the view
    public boolean drawRegion(Material material, Vector2fc position, Vector2fc size, Vector2fc reference,
                              float rotation, Vector4fc region, EFlip flip, Color4f color, int layer, float depth) {
        int state = state(material);
        if (state < 0)
            return false;

        float width = size.x(), height = size.y();
        float left = -(reference == null ? 0 : reference.x()) * width;
        float top = -(reference == null ? 0 : reference.y()) * height;
        float cos = (float) Math.cos(rotation), sin = (float) Math.sin(rotation);
        float px = position.x(), py = position.y();

        float u0 = region == null ? 0 : region.x(), v0 = region == null ? 0 : region.y();
        float u1 = region == null ? 1 : region.z(), v1 = region == null ? 1 : region.w();
        // Like RectangularFace: a flip is named by its axis, Horizontal mirrors the rows (v), Vertical the columns (u)
        if (flip == EFlip.Horizontal || flip == EFlip.HorizontalVertical) {
            float v = v0;
            v0 = v1;
            v1 = v;
        }
        if (flip == EFlip.Vertical || flip == EFlip.HorizontalVertical) {
            float u = u0;
            u0 = u1;
            u1 = u;
        }

        // Corners in the order of RectangleRenderer: (0, h), (0, 0), (w, 0), (w, h)
        float x0 = left, y0 = top + height, x1 = left, y1 = top;
        float x2 = left + width, y2 = top, x3 = left + width, y3 = top + height;
        quad(state, layer, depth,
                px + cos * x0 - sin * y0, py + sin * x0 + cos * y0, u0, v1,
                px + cos * x1 - sin * y1, py + sin * x1 + cos * y1, u0, v0,
                px + cos * x2 - sin * y2, py + sin * x2 + cos * y2, u1, v0,
                px + cos * x3 - sin * y3, py + sin * x3 + cos * y3, u1, v1,
                color == null ? -1 : color.toIntRGBA());
        drawnOutsideRenderer();
        return true;
    }

    /** A rectangle of the whole texture of the material (or of its color), its top left corner at position. */
    public boolean drawRectangle(Material material, Vector2fc position, Vector2fc size, Color4f color) {
        return drawRegion(material, position, size, null, 0f, null, EFlip.None, color, 0, 0f);
    }

    /**
     * Any quad of the material: its 4 corners (x, y pairs, in order around it: triangles 0 3 2 and 0 2 1), their
     * texture coordinates (u, v pairs), one color (null for white). False when the material can't be batched now.
     */
    public boolean drawQuad(Material material, float[] corners, float[] textureCoordinates, Color4f color,
                            int layer, float depth) {
        if (corners.length != 8 || textureCoordinates.length != 8)
            throw new IllegalArgumentException("A quad has 4 corners and 4 texture coordinates (8 floats each)");

        int state = state(material);
        if (state < 0)
            return false;

        quad(state, layer, depth,
                corners[0], corners[1], textureCoordinates[0], textureCoordinates[1],
                corners[2], corners[3], textureCoordinates[2], textureCoordinates[3],
                corners[4], corners[5], textureCoordinates[4], textureCoordinates[5],
                corners[6], corners[7], textureCoordinates[6], textureCoordinates[7],
                color == null ? -1 : color.toIntRGBA());
        drawnOutsideRenderer();
        return true;
    }

    // Mode OFF: a draw of this API is sent at once (one draw call, like a renderer drawn alone)
    private void drawnOutsideRenderer() {
        if (!isEnabled())
            send(true);
    }

    // ---- Barriers ----

    /**
     * Sends the draws asked so far: before OpenGL calls of the caller, or to keep them under what follows whatever
     * the sort key. A barrier (see IDrawSortKey).
     */
    public void flush() {
        barrier(CAUSE_FLUSH);
    }

    /**
     * Sends the draws asked so far because of cause (the class of a renderer drawn alone, or a name): counted as a
     * barrier of the frame when draws were waiting.
     */
    @API(status = INTERNAL)
    public void barrier(Object cause) {
        if (target == null || sending)
            return;

        if (drawCount > 0) {
            current.countBarrier(cause, sortKey != null);
            EngineStats.countBatchBarrier();
        }
        send(true);
    }

    /** The context of the waiting draws is destroyed: they are dropped, the batch is no longer started. */
    @API(status = INTERNAL)
    public void discard() {
        if (target == null)
            return;

        inDraw = false;
        drawCount = 0;
        send(true);
    }

    /** End of the display of the view: the draws are sent, the counters of the frame kept as the last frame's. */
    @API(status = INTERNAL)
    public void endView() {
        send(true);

        last.copyFrom(current);
        current.clear();
    }

    // Sends the waiting draws; end: the batch is no longer started (the ids of the table may change)
    private void send(boolean end) {
        if (inDraw)
            throw new IllegalStateException("Flush of a batch during a draw: endDraw first");
        if (target == null || sending)
            return;

        sending = true;
        try {
            if (drawCount > 0)
                drawRuns();
        } finally {
            sending = false;
            vertexCount = 0;
            indexCount = 0;
            drawCount = 0;
            runCount = 0;

            if (end) {
                IBatchTarget ended = target;
                BatchStateTable endedTable = table;
                target = null;
                table = null;
                ended.ended(this);
                endedTable.batchEnded();
            }
        }
    }

    private void drawRuns() {
        IntBuffer drawn = indices;
        if (sortKey != null) {
            sortRuns();
            drawn = sortedIndices;
            ++current.sorts;
        }

        IBatchUploader uploader = target.uploader();
        vertices.position(0).limit(vertexCount * VERTEX_BYTES);
        drawn.position(0).limit(indexCount);
        try {
            uploader.upload(vertices, vertexCount, drawn, indexCount);
        } finally {
            vertices.clear();
            drawn.clear();
        }

        for (int run = 0; run < runCount; ++run) {
            table.apply(runStates[run]);
            uploader.draw(runFirsts[run], runCounts[run]);
        }
        uploader.flushEnded();

        current.drawCalls += runCount;
        ++current.flushes;
        EngineStats.countBatchFlushes(runCount);
        EngineStats.countBatchedDraws(drawCount);
    }

    // The indices in the order of the keys, the runs of the same state in it
    private void sortRuns() {
        Arrays.sort(keys, 0, drawCount);

        if (sortedIndices == null || sortedIndices.capacity() < indexCount)
            sortedIndices = BufferUtils.createIntBuffer(Math.max(indexCount, indices.capacity()));

        runCount = 0;
        int out = 0;
        for (int k = 0; k < drawCount; ++k) {
            int entry = (int) (keys[k] & CALL_INDEX_MASK);
            int first = entryFirsts[entry];
            int count = entryCounts[entry];
            for (int i = 0; i < count; ++i)
                sortedIndices.put(out + i, indices.get(first + i));

            int state = entryStates[entry];
            if (runCount > 0 && runStates[runCount - 1] == state)
                runCounts[runCount - 1] += count;
            else
                addRun(state, out, count);
            out += count;
        }
    }

    private static ByteBuffer grow(ByteBuffer buffer, int used, int capacity) {
        ByteBuffer grown = BufferUtils.createByteBuffer(capacity);
        buffer.position(0).limit(used);
        grown.put(buffer);
        buffer.clear();
        grown.clear();
        return grown;
    }

    private static IntBuffer grow(IntBuffer buffer, int used, int capacity) {
        IntBuffer grown = BufferUtils.createIntBuffer(capacity);
        buffer.position(0).limit(used);
        grown.put(buffer);
        buffer.clear();
        grown.clear();
        return grown;
    }

    // ---- State of the batch ----

    // True from the first draw (or state) since the last flush to the next flush
    public boolean isStarted() {
        return target != null;
    }

    // Draws waiting to be sent
    public int pendingDraws() {
        return drawCount;
    }

    /** Batched draws of the last frame of the view. */
    public int lastFrameDraws() {
        return last.draws;
    }

    /** Draw calls sent by the batch in the last frame of the view. */
    public int lastFrameDrawCalls() {
        return last.drawCalls;
    }

    /** Flushes of the last frame of the view that sent draws (barriers and end of the view). */
    public int lastFrameFlushes() {
        return last.flushes;
    }

    /** Barriers of the last frame that sent waiting draws. */
    public int lastFrameBarriers() {
        return last.barriers;
    }

    /** Barriers of the last frame that cut a sorted segment (a sort key was set). */
    public int lastFrameBarriersWhileSorted() {
        return last.barriersWhileSorted;
    }

    /** Segments sorted in the last frame. */
    public int lastFrameSorts() {
        return last.sorts;
    }

    /**
     * Causes of the barriers of the last frame (class of a renderer drawn alone, or a name) with their count, and
     * the count of those in a sorted segment: {count, countWhileSorted}. A new map.
     */
    public Map<String, int[]> lastFrameBarrierCauses() {
        Map<String, int[]> causes = new LinkedHashMap<>();
        for (int i = 0; i < last.causeCount; ++i) {
            Object cause = last.causes[i];
            String name = cause instanceof Class ? ((Class<?>) cause).getName() : String.valueOf(cause);
            causes.put(name, new int[]{last.causeCounts[i], last.causeSortedCounts[i]});
        }

        return causes;
    }

    // Capacities of the arrays (tests: nothing grows once the frames are stable)
    int vertexCapacity() { return vertices.capacity() / VERTEX_BYTES; }
    int indexCapacity() { return indices.capacity(); }
    int entryCapacity() { return entryStates.length; }
    int runCapacity() { return runStates.length; }
    int sortedIndexCapacity() { return sortedIndices == null ? 0 : sortedIndices.capacity(); }

    // Counters of a frame of the view
    private static final class FrameCounters {
        int draws;
        int drawCalls;
        int flushes;
        int barriers;
        int barriersWhileSorted;
        int sorts;
        Object[] causes = new Object[4];
        int[] causeCounts = new int[4];
        int[] causeSortedCounts = new int[4];
        int causeCount;

        void countBarrier(Object cause, boolean sorted) {
            ++barriers;
            if (sorted)
                ++barriersWhileSorted;

            int i = 0;
            while (i < causeCount && !causes[i].equals(cause))
                ++i;
            if (i == causeCount) {
                if (causeCount == causes.length) {
                    causes = Arrays.copyOf(causes, causeCount * 2);
                    causeCounts = Arrays.copyOf(causeCounts, causeCount * 2);
                    causeSortedCounts = Arrays.copyOf(causeSortedCounts, causeCount * 2);
                }
                causes[i] = cause;
                causeCounts[i] = 0;
                causeSortedCounts[i] = 0;
                ++causeCount;
            }

            ++causeCounts[i];
            if (sorted)
                ++causeSortedCounts[i];
        }

        void copyFrom(FrameCounters other) {
            draws = other.draws;
            drawCalls = other.drawCalls;
            flushes = other.flushes;
            barriers = other.barriers;
            barriersWhileSorted = other.barriersWhileSorted;
            sorts = other.sorts;
            if (causes.length < other.causeCount) {
                causes = new Object[other.causes.length];
                causeCounts = new int[other.causes.length];
                causeSortedCounts = new int[other.causes.length];
            }
            System.arraycopy(other.causes, 0, causes, 0, other.causeCount);
            System.arraycopy(other.causeCounts, 0, causeCounts, 0, other.causeCount);
            System.arraycopy(other.causeSortedCounts, 0, causeSortedCounts, 0, other.causeCount);
            Arrays.fill(causes, other.causeCount, causes.length, null);
            causeCount = other.causeCount;
        }

        void clear() {
            draws = drawCalls = flushes = barriers = barriersWhileSorted = sorts = 0;
            Arrays.fill(causes, 0, causeCount, null);
            causeCount = 0;
        }
    }
}
