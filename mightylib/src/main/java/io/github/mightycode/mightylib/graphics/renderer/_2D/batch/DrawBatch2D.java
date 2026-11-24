package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchStateTable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchBackend;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchTarget;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchUploader;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.ViewCullTools;
import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.scenes.camera.Camera;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EFlip;
import org.apiguardian.api.API;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector4fc;
import org.json.JSONObject;
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
 * The 2D draws of a view, sent together: each batched draw writes its vertices (already transformed) or its instances
 * in the format of the batch program of its state (BatchFormat) when it is asked, then the draws are sent at the next
 * barrier, neighbors of the same state in a single draw call. By default in the order they were asked (nothing sorted, a constant cost per draw); with a
 * sort key (setDrawSortKey), in the order of their keys inside each segment between two barriers.
 * <p>
 * Barriers (the draws asked before are sent): a renderer drawn alone, a clear of the view, a scissor, a framebuffer
 * bound, flush(), the end of the display of the view, too many draws (MAX_DRAWS), a full table of states. Code that calls OpenGL itself
 * between draws calls flush() first (like RenderState.invalidateContextState after).
 * <p>
 * Low level, for a renderer of its own: beginDraw with a state, then its values in the order of the attributes of its
 * format (put, putInt, putColor: each vertex, or each instance for a format with instances, is complete once its
 * last attribute is written; vertex writes one of BatchFormats.SPRITE), the indices of its triangles (index, formats
 * without instances), endDraw (or quad).
 * <p>
 * View culling: a draw whose box is wholly outside the view of the camera of its state is not written (isCulled,
 * asked by the renderers and the drawing without renderer before they write). Main thread only.
 */
public final class DrawBatch2D {
    /** Bytes of a vertex of BatchFormats.SPRITE: position (2 floats, transformed), texture coordinates, color. */
    public static final int VERTEX_BYTES = BatchFormats.SPRITE.vertexBytes();
    /** Bytes of an instance of BatchFormats.SPRITE_INSTANCED (mode INSTANCED: quads of sprites as instances). */
    public static final int INSTANCE_BYTES = BatchFormats.SPRITE_INSTANCED.instanceBytes();
    /** Low bits of a sort key replaced by the index of the draw in its segment (IDrawSortKey). */
    public static final int CALL_INDEX_BITS = 24;
    public static final long CALL_INDEX_MASK = (1L << CALL_INDEX_BITS) - 1;
    /** Most draws of a segment: one more flushes the batch first (a barrier). */
    public static final int MAX_DRAWS = 1 << CALL_INDEX_BITS;

    // Causes of the barriers that are not a renderer
    public static final String CAUSE_FLUSH = "flush";
    public static final String CAUSE_FULL = "full";
    public static final String CAUSE_SORT_KEY = "sortKey";
    public static final String CAUSE_TABLE_FULL = "tableFull";

    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private static final int[] QUAD_INDICES = {0, 3, 2, 0, 2, 1};

    // The mode, and the target of the context current when the batch starts
    private final IBatchBackend backend;
    // Set while the batch is started: from its first draw (or state) to its flush
    private IBatchTarget target;
    private BatchStateTable table;
    // Size of the viewport of the context when the batch started (margins in pixels of the view culling)
    private int viewportWidth;
    private int viewportHeight;

    // Vertices of every format: those of a draw start on a multiple of the size of its vertex (its index base)
    private ByteBuffer vertices = BufferUtils.createByteBuffer(256 * VERTEX_BYTES);
    private int vertexBytes;
    // Indices of the vertices (in the vertices of their format), in the order of the draws
    private IntBuffer indices = BufferUtils.createIntBuffer(384);
    private int indexCount;
    // Instances of the formats with instances, in the order of the draws
    private ByteBuffer instances = BufferUtils.createByteBuffer(64 * INSTANCE_BYTES);
    private int instanceBytes;

    // The draw being asked
    private boolean inDraw;
    private int drawState;
    private int drawLayer;
    private float drawDepth;
    private BatchFormat drawFormat;
    private boolean drawInstanced;
    // Bytes of a vertex (or an instance) of the format, its values (kinds and offsets), the next one to write
    private int drawStride;
    private byte[] drawSlots;
    private int[] drawSlotOffsets;
    private int slot;
    // Bytes used when the draw began (before the padding of its first vertex), the vertex (or instance) written
    private int drawStartBytes;
    private int elementAt;
    // Formats without instances: its first vertex (index base), vertices and first index
    private int drawFirstVertex;
    private int drawVertexCount;
    private int drawFirstIndex;
    // Formats with instances: the byte of its first instance, its instances
    private int drawFirstInstanceByte;
    private int drawInstanceCount;

    // Draws of the segment
    private int drawCount;

    /*
     * Draw calls of the segment: in the order of the draws, built while they are asked; sorted, built at the flush. A
     * run of a format without instances: its first index and index count; with instances: the byte of its first
     * instance and its instance count.
     */
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
    private ByteBuffer sortedInstances;
    private int sortedInstanceBytes;

    // Mode INSTANCED: the byte of the first instance of each run of sprite quads drawn as instances (-1: by vertices)
    private int[] runInstanceFirsts = new int[0];

    // Counters of the current frame of the view, and of the last one (rolled by endView)
    private final FrameCounters current = new FrameCounters();
    private final FrameCounters last = new FrameCounters();
    // The frame of the engine the view was last displayed in (endView), -1 before
    private long shownFrame = -1;

    // While the draws are sent: a barrier asked by what the sending does is not one
    private boolean sending;

    // The box of the draw tested by the view culling, reused
    private final ViewCullBounds cullBounds = new ViewCullBounds();

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
     * started batch keeps the ids valid until its flush). A table without room for one more state (BatchStateTable
     * .hasRoom) is emptied first: the draws waiting are sent (a barrier), then the batch starts again.
     */
    @API(status = INTERNAL)
    public BatchStateTable stateTable() {
        start();
        if (!table.hasRoom()) {
            barrier(CAUSE_TABLE_FULL);
            start();
            if (!table.hasRoom())
                throw new IllegalStateException("The table of the 2D batch states is full and another batch has"
                        + " draws waiting: it can't be emptied");
        }

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
        viewportWidth = newTarget.viewportWidth();
        viewportHeight = newTarget.viewportHeight();
        table.batchStarted();
    }

    /** A draw of this state, layer 0 and depth 0: its values and indices follow, then endDraw. */
    public void beginDraw(int state) {
        beginDraw(state, 0, 0f);
    }

    /**
     * A draw of this state (asked to this batch, see stateTable): its values (in the format of the state) and indices
     * follow, then endDraw. Layer and depth are read by the sort key only.
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
        BatchFormat format = table.formatOf(state);
        if (format != drawFormat) {
            drawFormat = format;
            drawInstanced = format.hasInstances();
            drawStride = drawInstanced ? format.instanceBytes() : format.vertexBytes();
            drawSlots = format.slots(drawInstanced);
            drawSlotOffsets = format.slotOffsets(drawInstanced);
        }
        slot = 0;
        drawFirstIndex = indexCount;
        if (drawInstanced) {
            drawStartBytes = instanceBytes;
            drawFirstInstanceByte = instanceBytes;
            drawInstanceCount = 0;
        } else {
            drawStartBytes = vertexBytes;
            // The vertices of the draw start on a whole vertex of its format: their indices count in its size
            vertexBytes = (vertexBytes + drawStride - 1) / drawStride * drawStride;
            drawFirstVertex = vertexBytes / drawStride;
            drawVertexCount = 0;
        }
    }

    /** The format the draws of this state are written in (the one of its batch program), for a state of this batch. */
    public BatchFormat formatOf(int state) {
        start();
        return table.formatOf(state);
    }

    // The format of the draw being asked (the one of its state), null outside of a draw
    public BatchFormat drawFormat() {
        return inDraw ? drawFormat : null;
    }

    /** The next float value of the draw (an attribute FLOAT of its format, component by component). */
    public void put(float value) {
        int at = slotAt(BatchFormat.SLOT_FLOAT);
        (drawInstanced ? instances : vertices).putFloat(at, value);
        slotDone();
    }

    public void put(float first, float second) {
        put(first);
        put(second);
    }

    public void put(float first, float second, float third) {
        put(first);
        put(second);
        put(third);
    }

    public void put(float first, float second, float third, float fourth) {
        put(first);
        put(second);
        put(third);
        put(fourth);
    }

    /** The next int value of the draw (an attribute INT of its format, component by component). */
    public void putInt(int value) {
        int at = slotAt(BatchFormat.SLOT_INT);
        (drawInstanced ? instances : vertices).putInt(at, value);
        slotDone();
    }

    /**
     * The next attribute NORMALIZED_UBYTE of the draw, at once: RGBA in an int (Color4f.toIntRGBA), R in the first
     * byte read.
     */
    public void putColor(int rgba) {
        int at = slotAt(BatchFormat.SLOT_COLOR);
        // In memory R, G, B, A: read as 4 normalized bytes
        (drawInstanced ? instances : vertices).putInt(at, LITTLE_ENDIAN ? Integer.reverseBytes(rgba) : rgba);
        slotDone();
    }

    // The byte of the next value, checked against the kind of the format; room made for its vertex (or instance)
    private int slotAt(byte kind) {
        if (!inDraw)
            throw new IllegalStateException("Value written outside of beginDraw and endDraw");
        if (drawSlots[slot] != kind)
            throw new IllegalStateException("Value " + slot + " of a " + (drawInstanced ? "instance" : "vertex")
                    + " of the batch format " + drawFormat.name() + " is not a " + SlotName(kind) + " but a "
                    + SlotName(drawSlots[slot]));

        if (slot == 0) {
            if (drawInstanced) {
                if (instanceBytes + drawStride > instances.capacity())
                    instances = grow(instances, instanceBytes, Math.max(instances.capacity() * 2,
                            instanceBytes + drawStride));
                elementAt = instanceBytes;
            } else {
                if (vertexBytes + drawStride > vertices.capacity())
                    vertices = grow(vertices, vertexBytes, Math.max(vertices.capacity() * 2, vertexBytes + drawStride));
                elementAt = vertexBytes;
            }
        }

        return elementAt + drawSlotOffsets[slot];
    }

    private static String SlotName(byte kind) {
        return kind == BatchFormat.SLOT_FLOAT ? "float (put)" : kind == BatchFormat.SLOT_INT ? "int (putInt)"
                : "color (putColor)";
    }

    // The value is written: the last one of a vertex (or an instance) completes it
    private void slotDone() {
        if (++slot < drawSlots.length)
            return;

        slot = 0;
        if (drawInstanced) {
            instanceBytes += drawStride;
            ++drawInstanceCount;
        } else {
            vertexBytes += drawStride;
            ++drawVertexCount;
        }
    }

    /**
     * A vertex of a draw in BatchFormats.SPRITE (or a format with the same values): position (transformed: world
     * units of the camera of the state), texture coordinates, and color as RGBA in an int (Color4f.toIntRGBA),
     * multiplied with the texture by the batch shader.
     */
    public void vertex(float x, float y, float u, float v, int rgba) {
        if (!inDraw)
            throw new IllegalStateException("vertex outside of beginDraw and endDraw");

        if (drawFormat == BatchFormats.SPRITE && slot == 0) {
            if (vertexBytes + VERTEX_BYTES > vertices.capacity())
                vertices = grow(vertices, vertexBytes, vertices.capacity() * 2);

            int at = vertexBytes;
            vertices.putFloat(at, x);
            vertices.putFloat(at + 4, y);
            vertices.putFloat(at + 8, u);
            vertices.putFloat(at + 12, v);
            // In memory R, G, B, A: read as 4 normalized bytes
            vertices.putInt(at + 16, LITTLE_ENDIAN ? Integer.reverseBytes(rgba) : rgba);
            vertexBytes += VERTEX_BYTES;
            ++drawVertexCount;
        } else {
            put(x, y, u, v);
            putColor(rgba);
        }
    }

    /** An index of a triangle of the draw: a vertex of this draw, 0 for its first one. */
    public void index(int vertexOfDraw) {
        if (!inDraw)
            throw new IllegalStateException("index outside of beginDraw and endDraw");
        if (drawInstanced)
            throw new IllegalStateException("index in a draw of the batch format " + drawFormat.name()
                    + ": its instances are drawn as its template");
        if (slot != 0)
            throw new IllegalStateException("index in the middle of a vertex of the batch format " + drawFormat.name());
        if (vertexOfDraw < 0 || vertexOfDraw >= drawVertexCount)
            throw new IllegalArgumentException("Index " + vertexOfDraw + " of a draw of " + drawVertexCount
                    + " vertices: the vertices come before the indices");

        putIndex(drawFirstVertex + vertexOfDraw);
    }

    private void putIndex(int vertex) {
        if (indexCount == indices.capacity())
            indices = grow(indices, indexCount, indices.capacity() * 2);

        indices.put(indexCount++, vertex);
    }

    /**
     * Ends the draw: a draw without index (without instance for a format with instances) draws nothing (its values
     * are dropped). Ended in the middle of a vertex (or an instance): dropped, IllegalStateException.
     */
    public void endDraw() {
        if (!inDraw)
            throw new IllegalStateException("endDraw without beginDraw");

        inDraw = false;
        if (slot != 0) {
            slot = 0;
            dropDraw();
            throw new IllegalStateException("endDraw in the middle of a " + (drawInstanced ? "instance" : "vertex")
                    + " of the batch format " + drawFormat.name() + " (" + drawSlots.length + " values each)");
        }

        int count = drawInstanced ? drawInstanceCount : indexCount - drawFirstIndex;
        if (count == 0) {
            dropDraw();
            return;
        }

        int first = drawInstanced ? drawFirstInstanceByte : drawFirstIndex;
        if (sortKey == null) {
            // Same state as the previous draw, and its data right after: the same draw call
            int last = runCount - 1;
            if (last >= 0 && runStates[last] == drawState
                    && (!drawInstanced || runFirsts[last] + runCounts[last] * drawStride == first))
                runCounts[last] += count;
            else
                addRun(drawState, first, count);
        } else {
            addEntry(first, count);
        }

        // What the draw wrote: its vertices and indices, or its instances
        current.countWritten(drawFormat, drawInstanced ? (long) drawInstanceCount * drawStride
                : (long) drawVertexCount * drawStride + (long) (indexCount - drawFirstIndex) * Integer.BYTES);
        ++drawCount;
        ++current.draws;
    }

    // The values and indices of the draw being ended are dropped
    private void dropDraw() {
        if (drawInstanced)
            instanceBytes = drawStartBytes;
        else
            vertexBytes = drawStartBytes;
        indexCount = drawFirstIndex;
    }

    /**
     * A quad of this state (BatchFormats.SPRITE): its corners in order around it (triangles 0 3 2 and 0 2 1, like
     * RectangularFace), each with its texture coordinates, one color.
     */
    public void quad(int state, int layer, float depth,
                     float x0, float y0, float u0, float v0,
                     float x1, float y1, float u1, float v1,
                     float x2, float y2, float u2, float v2,
                     float x3, float y3, float u3, float v3,
                     int rgba) {
        beginDraw(state, layer, depth);
        vertex(x0, y0, u0, v0, rgba);
        vertex(x1, y1, u1, v1, rgba);
        vertex(x2, y2, u2, v2, rgba);
        vertex(x3, y3, u3, v3, rgba);
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

    private void addEntry(int first, int count) {
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
        entryFirsts[drawCount] = first;
        entryCounts[drawCount] = count;
        // Computed when the draw is asked: the function reads values that may change before the flush
        keys[drawCount] = (sortKey.key(drawLayer, drawDepth, drawState) & ~CALL_INDEX_MASK) | drawCount;
    }

    // ---- View culling ----

    /** The view culling of the engine is on (EngineConfig.setViewCulling2D, Engine.setViewCulling2D). */
    public boolean isViewCulling() {
        return backend.viewCulling();
    }

    /**
     * The box reused by the draws of this batch, cleared for a draw of this state (asked to this batch): its model
     * matrix, the class of what draws, the size of a pixel of the camera of the state in the viewport.
     */
    @API(status = INTERNAL)
    public ViewCullBounds cullBounds(int state, Matrix4fc model, Class<?> owner) {
        cullBounds.clear(model, owner);
        if (table == null || !table.isCurrent(state))
            return cullBounds;

        int camera = BatchStateTable.cameraOf(state);
        if (table.hasViewBox(camera) && viewportWidth > 0 && viewportHeight > 0) {
            float clipX = 2f / viewportWidth, clipY = 2f / viewportHeight;
            cullBounds.pixelToWorld(table.clipToWorld(camera, 0) * clipX, table.clipToWorld(camera, 2) * clipX,
                    table.clipToWorld(camera, 1) * clipY, table.clipToWorld(camera, 3) * clipY);
        }
        return cullBounds;
    }

    /**
     * The view culling of a draw of this state, before it is written: true when its box, margins included, is wholly
     * outside the box of the view of the camera of the state (a box that touches it is not), the draw is then not
     * written. False without box (counted as such), for a camera that is not an affine 2D transform (a perspective)
     * or a local box with such a model matrix, and when the view culling of the engine is off
     * (EngineConfig.setViewCulling2D; nothing counted then). The margin of the material part of the state
     * ("cullMargin" of its shaders) is added; margins in pixels use the viewport of the context when the batch
     * started.
     */
    public boolean isCulled(int state, ViewCullBounds bounds) {
        if (!backend.viewCulling())
            return false;

        start();
        if (!table.isCurrent(state))
            throw new IllegalArgumentException("State " + state + " unknown to the table of the batch");
        if (bounds == null || !bounds.hasBox()) {
            ++current.cullNoBounds;
            current.countNoBounds(bounds == null ? null : bounds.owner());
            return false;
        }

        ++current.cullTested;
        int camera = BatchStateTable.cameraOf(state);
        if (!table.hasViewBox(camera))
            return false;

        float minX = bounds.minX(), minY = bounds.minY(), maxX = bounds.maxX(), maxY = bounds.maxY();
        Matrix4fc model = bounds.model();
        if (bounds.isLocal() && model != null) {
            // Draws at z 0: an affine transform of the plane when w stays 1
            if (model.m03() != 0f || model.m13() != 0f || model.m33() != 1f)
                return false;

            // The box of the 4 transformed corners: center transformed, half extents by the absolute linear part
            float centerX = (minX + maxX) * 0.5f, centerY = (minY + maxY) * 0.5f;
            float halfX = (maxX - minX) * 0.5f, halfY = (maxY - minY) * 0.5f;
            float worldX = model.m00() * centerX + model.m10() * centerY + model.m30();
            float worldY = model.m01() * centerX + model.m11() * centerY + model.m31();
            float extentX = Math.abs(model.m00()) * halfX + Math.abs(model.m10()) * halfY;
            float extentY = Math.abs(model.m01()) * halfX + Math.abs(model.m11()) * halfY;
            minX = worldX - extentX;
            maxX = worldX + extentX;
            minY = worldY - extentY;
            maxY = worldY + extentY;
        }

        float marginX = bounds.worldMargin() + table.cullMarginOf(state), marginY = marginX;
        float pixels = bounds.pixelMargin();
        // World units of a pixel of the viewport along x and y (the box of a pixel, for a turned camera)
        float pixelX = 0f, pixelY = 0f;
        if (viewportWidth > 0 && viewportHeight > 0) {
            float clipX = 2f / viewportWidth, clipY = 2f / viewportHeight;
            pixelX = Math.abs(table.clipToWorld(camera, 0)) * clipX + Math.abs(table.clipToWorld(camera, 1)) * clipY;
            pixelY = Math.abs(table.clipToWorld(camera, 2)) * clipX + Math.abs(table.clipToWorld(camera, 3)) * clipY;
        } else if (pixels > 0f) {
            return false;
        }

        marginX += pixels * pixelX;
        marginY += pixels * pixelY;

        float viewMinX = table.viewMinX(camera), viewMinY = table.viewMinY(camera);
        float viewMaxX = table.viewMaxX(camera), viewMaxY = table.viewMaxY(camera);
        ViewCullTools tools = backend.cullTools();
        boolean tooled = tools != null && tools.active();
        if (tooled) {
            float[] frozen = tools.frozenBox(table.viewSourceOf(camera));
            if (frozen != null) {
                viewMinX = frozen[0];
                viewMinY = frozen[1];
                viewMaxX = frozen[2];
                viewMaxY = frozen[3];
            }
            // The test zone reduced on each side
            viewMinX += tools.inset() * pixelX;
            viewMaxX -= tools.inset() * pixelX;
            viewMinY += tools.inset() * pixelY;
            viewMaxY -= tools.inset() * pixelY;
        }

        boolean culled = maxX + marginX < viewMinX || minX - marginX > viewMaxX
                || maxY + marginY < viewMinY || minY - marginY > viewMaxY;
        if (culled)
            ++current.culled;
        if (tooled)
            reportCulling(tools, state, bounds.owner(), minX - marginX, minY - marginY, maxX + marginX,
                    maxY + marginY, culled);
        return culled;
    }

    // The tools of the culling see the draw: listed when culled, its box drawn by the debug drawings of the view
    private void reportCulling(ViewCullTools tools, int state, Class<?> owner, float minX, float minY, float maxX,
                               float maxY, boolean culled) {
        int camera = BatchStateTable.cameraOf(state);
        if (culled)
            tools.recordCulled(owner == null ? "?" : owner.getName(), minX, minY, maxX, maxY, camera);

        if (tools.isShow() && table.viewSourceOf(camera) == Views.current().camera2D().getView().getObject())
            Views.current().debugDraw().rect(minX, minY, maxX - minX, maxY - minY,
                    culled ? SHOWN_CULLED : SHOWN_DRAWN);
    }

    private static final Color4f SHOWN_DRAWN = new Color4f(0.1f, 1f, 0.2f, 1f);
    private static final Color4f SHOWN_CULLED = new Color4f(1f, 0.15f, 0.1f, 1f);

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
     * batched now (nothing drawn); true when drawn, or culled (outside the view: nothing written).
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
        float wx0 = px + cos * x0 - sin * y0, wy0 = py + sin * x0 + cos * y0;
        float wx1 = px + cos * x1 - sin * y1, wy1 = py + sin * x1 + cos * y1;
        float wx2 = px + cos * x2 - sin * y2, wy2 = py + sin * x2 + cos * y2;
        float wx3 = px + cos * x3 - sin * y3, wy3 = py + sin * x3 + cos * y3;
        if (cornersCulled(state, wx0, wy0, wx1, wy1, wx2, wy2, wx3, wy3))
            return true;

        quad(state, layer, depth,
                wx0, wy0, u0, v1,
                wx1, wy1, u0, v0,
                wx2, wy2, u1, v0,
                wx3, wy3, u1, v1,
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
     * texture coordinates (u, v pairs), one color (null for white). False when the material can't be batched now,
     * true when drawn or culled.
     */
    public boolean drawQuad(Material material, float[] corners, float[] textureCoordinates, Color4f color,
                            int layer, float depth) {
        if (corners.length != 8 || textureCoordinates.length != 8)
            throw new IllegalArgumentException("A quad has 4 corners and 4 texture coordinates (8 floats each)");

        int state = state(material);
        if (state < 0)
            return false;
        if (cornersCulled(state, corners[0], corners[1], corners[2], corners[3], corners[4], corners[5], corners[6],
                corners[7]))
            return true;

        quad(state, layer, depth,
                corners[0], corners[1], textureCoordinates[0], textureCoordinates[1],
                corners[2], corners[3], textureCoordinates[2], textureCoordinates[3],
                corners[4], corners[5], textureCoordinates[4], textureCoordinates[5],
                corners[6], corners[7], textureCoordinates[6], textureCoordinates[7],
                color == null ? -1 : color.toIntRGBA());
        drawnOutsideRenderer();
        return true;
    }

    // A quad of this API outside the view: not written (batched draws only, like those of the renderers)
    private boolean cornersCulled(int state, float x0, float y0, float x1, float y1, float x2, float y2, float x3,
                                  float y3) {
        if (!isEnabled() || !backend.viewCulling())
            return false;

        cullBounds.clear(null, DrawBatch2D.class).world(Math.min(Math.min(x0, x1), Math.min(x2, x3)),
                Math.min(Math.min(y0, y1), Math.min(y2, y3)), Math.max(Math.max(x0, x1), Math.max(x2, x3)),
                Math.max(Math.max(y0, y1), Math.max(y2, y3)));
        return isCulled(state, cullBounds);
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

        EngineStats.countViewCull(current.cullTested, current.culled, current.cullNoBounds);
        last.copyFrom(current);
        current.clear();
        shownFrame = backend.frame();
    }

    /** The view of this batch was displayed in the last frame of the engine (its counters are not old ones). */
    public boolean shownLastFrame() {
        return shownFrame == backend.frame();
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
            vertexBytes = 0;
            indexCount = 0;
            instanceBytes = 0;
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
        IntBuffer drawnIndices = indices;
        int drawnInstanceBytes = instanceBytes;
        boolean sorted = sortKey != null;
        if (sorted) {
            sortRuns();
            drawnIndices = sortedIndices;
            drawnInstanceBytes = sortedInstanceBytes;
            ++current.sorts;
        }

        // Mode INSTANCED: the runs of sprite quads become instances, after those of the formats with instances
        int convertedFrom = drawnInstanceBytes;
        if (backend.mode() == EBatch2DMode.INSTANCED)
            drawnInstanceBytes = buildInstances(drawnIndices, sorted, drawnInstanceBytes);
        boolean converted = drawnInstanceBytes > convertedFrom;
        ByteBuffer drawnInstances = sorted ? sortedInstances : instances;

        // Every run of vertices drawn by instances: the vertices are not sent
        boolean regularRuns = !converted || hasRegularRun();
        IBatchUploader uploader = target.uploader();
        vertices.position(0).limit(regularRuns ? vertexBytes : 0);
        drawnIndices.position(0).limit(regularRuns ? indexCount : 0);
        drawnInstances.position(0).limit(drawnInstanceBytes);
        long sentBytes = vertices.limit() + (long) drawnIndices.limit() * Integer.BYTES + drawnInstanceBytes;
        current.bytesSent += sentBytes;
        EngineStats.countBatchBytes(sentBytes);
        try {
            uploader.upload(vertices, drawnIndices, drawnInstances);
        } finally {
            vertices.clear();
            drawnIndices.clear();
            drawnInstances.clear();
        }

        for (int run = 0; run < runCount; ++run) {
            int state = runStates[run];
            BatchFormat format = table.formatOf(state);
            if (converted && runInstanceFirsts[run] >= 0) {
                table.applyInstanced(state);
                uploader.drawInstances(BatchFormats.SPRITE_INSTANCED, runInstanceFirsts[run], runCounts[run] / 6);
                ++current.instancedDrawCalls;
                current.countFormat(BatchFormats.SPRITE_INSTANCED);
            } else if (format.hasInstances()) {
                table.apply(state);
                uploader.drawInstances(format, runFirsts[run], runCounts[run]);
                ++current.instancedDrawCalls;
                current.countFormat(format);
            } else {
                table.apply(state);
                uploader.draw(format, runFirsts[run], runCounts[run]);
                current.countFormat(format);
            }
        }
        uploader.flushEnded();

        current.drawCalls += runCount;
        ++current.flushes;
        EngineStats.countBatchFlushes(runCount);
        EngineStats.countBatchedDraws(drawCount);
    }

    private boolean hasRegularRun() {
        for (int run = 0; run < runCount; ++run) {
            if (runInstanceFirsts[run] < 0 && !table.formatOf(runStates[run]).hasInstances())
                return true;
        }
        return false;
    }

    /*
     * Mode INSTANCED: the runs of BatchFormats.SPRITE whose state has an instanced program of
     * BatchFormats.SPRITE_INSTANCED and whose triangles are all quads (groups of 6 indices b+0 3 2 0 2 1, like quad:
     * whatever draws they come from, they draw the quad of the vertices b to b+3) that are parallelograms of one color
     * get their instances, written from byte at of the drawn instances (runInstanceFirsts, -1 for the others).
     * Returns the bytes of the drawn instances after them.
     */
    private int buildInstances(IntBuffer drawn, boolean sorted, int at) {
        if (runInstanceFirsts.length < runCount)
            runInstanceFirsts = new int[runStates.length];

        for (int run = 0; run < runCount; ++run) {
            runInstanceFirsts[run] = -1;
            int state = runStates[run];
            int indexTotal = runCounts[run];
            if (indexTotal % 6 != 0 || table.formatOf(state) != BatchFormats.SPRITE || !table.hasInstanced(state)
                    || table.instancedFormatOf(state) != BatchFormats.SPRITE_INSTANCED)
                continue;

            int quads = indexTotal / 6;
            ByteBuffer target = drawnInstances(sorted, at, at + quads * INSTANCE_BYTES);

            boolean all = true;
            int first = runFirsts[run];
            for (int quad = 0; quad < quads && all; ++quad)
                all = writeInstance(drawn, first + quad * 6, target, at + quad * INSTANCE_BYTES);

            if (all) {
                runInstanceFirsts[run] = at;
                at += quads * INSTANCE_BYTES;
            }
        }

        return at;
    }

    // The instances drawn by this flush (used bytes written), with room for bytes
    private ByteBuffer drawnInstances(boolean sorted, int used, int bytes) {
        if (sorted) {
            if (sortedInstances.capacity() < bytes)
                sortedInstances = grow(sortedInstances, used, Math.max(sortedInstances.capacity() * 2, bytes));
            return sortedInstances;
        }

        if (instances.capacity() < bytes)
            instances = grow(instances, used, Math.max(instances.capacity() * 2, bytes));
        return instances;
    }

    // The quad of the 6 indices at index as an instance at byte at (affine position, affine texture coordinates, color)
    private boolean writeInstance(IntBuffer drawn, int index, ByteBuffer target, int at) {
        int base = drawn.get(index);
        for (int i = 0; i < 6; ++i) {
            if (drawn.get(index + i) != base + QUAD_INDICES[i])
                return false;
        }

        int a0 = base * VERTEX_BYTES, a1 = a0 + VERTEX_BYTES, a2 = a1 + VERTEX_BYTES, a3 = a2 + VERTEX_BYTES;
        int color = vertices.getInt(a1 + 16);
        if (vertices.getInt(a0 + 16) != color || vertices.getInt(a2 + 16) != color
                || vertices.getInt(a3 + 16) != color)
            return false;

        // Corner (0, 0) of the unit quad is vertex 1, its axes go to vertex 2 (x) and vertex 0 (y)
        float x1 = vertices.getFloat(a1), y1 = vertices.getFloat(a1 + 4);
        float xx = vertices.getFloat(a2) - x1, xy = vertices.getFloat(a2 + 4) - y1;
        float yx = vertices.getFloat(a0) - x1, yy = vertices.getFloat(a0 + 4) - y1;
        float u1 = vertices.getFloat(a1 + 8), v1 = vertices.getFloat(a1 + 12);
        float ux = vertices.getFloat(a2 + 8) - u1, vx = vertices.getFloat(a2 + 12) - v1;
        float uy = vertices.getFloat(a0 + 8) - u1, vy = vertices.getFloat(a0 + 12) - v1;

        // Vertex 3 is the fourth corner of the parallelogram (an affine shape), within the rounding of floats
        float positionTolerance = 1e-4f * (1 + Math.abs(xx) + Math.abs(xy) + Math.abs(yx) + Math.abs(yy)
                + Math.abs(x1) + Math.abs(y1));
        float textureTolerance = 1e-5f * (1 + Math.abs(ux) + Math.abs(vx) + Math.abs(uy) + Math.abs(vy)
                + Math.abs(u1) + Math.abs(v1));
        if (Math.abs(vertices.getFloat(a3) - (x1 + xx + yx)) > positionTolerance
                || Math.abs(vertices.getFloat(a3 + 4) - (y1 + xy + yy)) > positionTolerance
                || Math.abs(vertices.getFloat(a3 + 8) - (u1 + ux + uy)) > textureTolerance
                || Math.abs(vertices.getFloat(a3 + 12) - (v1 + vx + vy)) > textureTolerance)
            return false;

        target.putFloat(at, xx).putFloat(at + 4, yx).putFloat(at + 8, x1);
        target.putFloat(at + 12, xy).putFloat(at + 16, yy).putFloat(at + 20, y1);
        target.putFloat(at + 24, u1).putFloat(at + 28, v1);
        target.putFloat(at + 32, ux).putFloat(at + 36, vx).putFloat(at + 40, uy).putFloat(at + 44, vy);
        target.putInt(at + 48, color);
        return true;
    }

    // The indices and the instances in the order of the keys, the runs of the same state in it
    private void sortRuns() {
        Arrays.sort(keys, 0, drawCount);

        if (sortedIndices == null || sortedIndices.capacity() < indexCount)
            sortedIndices = BufferUtils.createIntBuffer(Math.max(indexCount, indices.capacity()));
        if (sortedInstances == null || sortedInstances.capacity() < instanceBytes)
            sortedInstances = BufferUtils.createByteBuffer(Math.max(instanceBytes, instances.capacity()));

        runCount = 0;
        int out = 0;
        int outBytes = 0;
        for (int k = 0; k < drawCount; ++k) {
            int entry = (int) (keys[k] & CALL_INDEX_MASK);
            int first = entryFirsts[entry];
            int count = entryCounts[entry];
            int state = entryStates[entry];
            BatchFormat format = table.formatOf(state);
            int last = runCount - 1;
            if (format.hasInstances()) {
                int bytes = count * format.instanceBytes();
                instances.position(first).limit(first + bytes);
                sortedInstances.position(outBytes);
                sortedInstances.put(instances);
                instances.clear();
                sortedInstances.clear();

                if (last >= 0 && runStates[last] == state)
                    runCounts[last] += count;
                else
                    addRun(state, outBytes, count);
                outBytes += bytes;
            } else {
                for (int i = 0; i < count; ++i)
                    sortedIndices.put(out + i, indices.get(first + i));

                if (last >= 0 && runStates[last] == state)
                    runCounts[last] += count;
                else
                    addRun(state, out, count);
                out += count;
            }
        }
        sortedInstanceBytes = outBytes;
    }

    // used may go past the buffer: the padding before the first vertex of a draw (nothing written there yet)
    private static ByteBuffer grow(ByteBuffer buffer, int used, int capacity) {
        ByteBuffer grown = BufferUtils.createByteBuffer(capacity);
        buffer.position(0).limit(Math.min(used, buffer.capacity()));
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

    /** Draw calls of the last frame that drew instances (formats with instances, sprite quads in mode INSTANCED). */
    public int lastFrameInstancedDrawCalls() {
        return last.instancedDrawCalls;
    }

    /** Draws of the last frame whose box was tested by the view culling (isCulled). */
    public int lastFrameCullTested() {
        return last.cullTested;
    }

    /** Draws of the last frame culled (outside the view: not written). */
    public int lastFrameCulled() {
        return last.culled;
    }

    /** Draws of the last frame asked to the view culling without box (never culled). */
    public int lastFrameCullNoBounds() {
        return last.cullNoBounds;
    }

    /** Bytes of vertices, indices and instances sent to the GPU in the last frame of the view. */
    public long lastFrameBytes() {
        return last.bytesSent;
    }

    /** Draws gathered by each draw call of the last frame (0 without draw call). */
    public double lastFrameDrawsPerCall() {
        return last.drawCalls == 0 ? 0 : (double) last.draws / last.drawCalls;
    }

    /** Bytes sent by each draw call of the last frame (0 without draw call). */
    public double lastFrameBytesPerCall() {
        return last.drawCalls == 0 ? 0 : (double) last.bytesSent / last.drawCalls;
    }

    /**
     * Bytes the draws of the last frame wrote, by the name of their batch format (a draw culled wrote none; the
     * quads drawn by instances in mode INSTANCED are counted as written, not as sent). A new map.
     */
    public Map<String, Long> lastFrameFormatBytes() {
        Map<String, Long> bytes = new LinkedHashMap<>();
        for (int i = 0; i < last.writtenCount; ++i)
            bytes.merge(last.writtenFormats[i].name(), last.writtenBytes[i], Long::sum);

        return bytes;
    }

    /** Draws without box of the last frame by the name of the class that drew them (the 10 most). A new map. */
    public Map<String, Integer> lastFrameNoBoundsClasses() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        int[] order = new int[last.noBoundsCount];
        int ordered = 0;
        for (int i = 0; i < last.noBoundsCount; ++i) {
            int at = ordered++;
            while (at > 0 && last.noBoundsCounts[order[at - 1]] < last.noBoundsCounts[i]) {
                order[at] = order[at - 1];
                --at;
            }
            order[at] = i;
        }

        for (int i = 0; i < Math.min(10, ordered); ++i) {
            Class<?> owner = last.noBoundsClasses[order[i]];
            counts.put(owner == null ? "?" : owner.getName(), last.noBoundsCounts[order[i]]);
        }
        return counts;
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

    /** Draw calls of the last frame of the view by the name of their batch format. A new map. */
    public Map<String, Integer> lastFrameFormatCalls() {
        Map<String, Integer> calls = new LinkedHashMap<>();
        for (int i = 0; i < last.formatCount; ++i)
            calls.merge(last.formats[i].name(), last.formatCalls[i], Integer::sum);

        return calls;
    }

    /**
     * The batch for the command batch: started or not, its sort key (its toString, null for the order of the calls),
     * the counters of the last frame of its view, the causes of its barriers and its draw calls by format.
     */
    @API(status = INTERNAL)
    public JSONObject describe() {
        JSONObject causes = new JSONObject();
        for (Map.Entry<String, int[]> cause : lastFrameBarrierCauses().entrySet())
            causes.put(cause.getKey(), new JSONObject().put("count", cause.getValue()[0])
                    .put("whileSorted", cause.getValue()[1]));

        return new JSONObject()
                .put("sortKey", sortKey == null ? JSONObject.NULL : sortKey.toString())
                .put("started", isStarted())
                .put("shownLastFrame", shownLastFrame())
                .put("draws", last.draws)
                .put("drawCalls", last.drawCalls)
                .put("flushes", last.flushes)
                .put("barriers", last.barriers)
                .put("barriersWhileSorted", last.barriersWhileSorted)
                .put("sorts", last.sorts)
                .put("instancedDrawCalls", last.instancedDrawCalls)
                .put("barrierCauses", causes)
                .put("formats", new JSONObject(lastFrameFormatCalls()))
                .put("formatBytes", new JSONObject(lastFrameFormatBytes()))
                .put("bytes", last.bytesSent)
                .put("drawsPerCall", lastFrameDrawsPerCall())
                .put("bytesPerCall", lastFrameBytesPerCall())
                .put("viewCull", new JSONObject().put("tested", last.cullTested).put("culled", last.culled)
                        .put("noBounds", last.cullNoBounds)
                        .put("noBoundsClasses", new JSONObject(lastFrameNoBoundsClasses())));
    }

    // Capacities of the arrays (tests: nothing grows once the frames are stable)
    int vertexCapacity() { return vertices.capacity() / VERTEX_BYTES; }
    int instanceCapacity() { return instances.capacity(); }
    int indexCapacity() { return indices.capacity(); }
    int entryCapacity() { return entryStates.length; }
    int runCapacity() { return runStates.length; }
    int sortedIndexCapacity() { return sortedIndices == null ? 0 : sortedIndices.capacity(); }
    int sortedInstanceCapacity() { return sortedInstances == null ? 0 : sortedInstances.capacity(); }

    // Counters of a frame of the view
    private static final class FrameCounters {
        int draws;
        int drawCalls;
        int flushes;
        int barriers;
        int barriersWhileSorted;
        int sorts;
        int instancedDrawCalls;
        // View culling: draws tested, culled, without box
        int cullTested;
        int culled;
        int cullNoBounds;
        // Draws without box by class
        Class<?>[] noBoundsClasses = new Class<?>[4];
        int[] noBoundsCounts = new int[4];
        int noBoundsCount;
        // Bytes sent to the GPU, and bytes written by the draws by format
        long bytesSent;
        BatchFormat[] writtenFormats = new BatchFormat[4];
        long[] writtenBytes = new long[4];
        int writtenCount;
        // Draw calls by format
        BatchFormat[] formats = new BatchFormat[4];
        int[] formatCalls = new int[4];
        int formatCount;
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

        void countNoBounds(Class<?> owner) {
            int i = 0;
            while (i < noBoundsCount && noBoundsClasses[i] != owner)
                ++i;
            if (i == noBoundsCount) {
                if (noBoundsCount == noBoundsClasses.length) {
                    noBoundsClasses = Arrays.copyOf(noBoundsClasses, noBoundsCount * 2);
                    noBoundsCounts = Arrays.copyOf(noBoundsCounts, noBoundsCount * 2);
                }
                noBoundsClasses[i] = owner;
                noBoundsCounts[i] = 0;
                ++noBoundsCount;
            }

            ++noBoundsCounts[i];
        }

        void countWritten(BatchFormat format, long bytes) {
            int i = 0;
            while (i < writtenCount && writtenFormats[i] != format)
                ++i;
            if (i == writtenCount) {
                if (writtenCount == writtenFormats.length) {
                    writtenFormats = Arrays.copyOf(writtenFormats, writtenCount * 2);
                    writtenBytes = Arrays.copyOf(writtenBytes, writtenCount * 2);
                }
                writtenFormats[i] = format;
                writtenBytes[i] = 0;
                ++writtenCount;
            }

            writtenBytes[i] += bytes;
        }

        void countFormat(BatchFormat format) {
            int i = 0;
            while (i < formatCount && formats[i] != format)
                ++i;
            if (i == formatCount) {
                if (formatCount == formats.length) {
                    formats = Arrays.copyOf(formats, formatCount * 2);
                    formatCalls = Arrays.copyOf(formatCalls, formatCount * 2);
                }
                formats[i] = format;
                formatCalls[i] = 0;
                ++formatCount;
            }

            ++formatCalls[i];
        }

        void copyFrom(FrameCounters other) {
            draws = other.draws;
            drawCalls = other.drawCalls;
            flushes = other.flushes;
            barriers = other.barriers;
            barriersWhileSorted = other.barriersWhileSorted;
            sorts = other.sorts;
            instancedDrawCalls = other.instancedDrawCalls;
            cullTested = other.cullTested;
            culled = other.culled;
            cullNoBounds = other.cullNoBounds;
            bytesSent = other.bytesSent;
            if (noBoundsClasses.length < other.noBoundsCount) {
                noBoundsClasses = new Class<?>[other.noBoundsClasses.length];
                noBoundsCounts = new int[other.noBoundsClasses.length];
            }
            System.arraycopy(other.noBoundsClasses, 0, noBoundsClasses, 0, other.noBoundsCount);
            System.arraycopy(other.noBoundsCounts, 0, noBoundsCounts, 0, other.noBoundsCount);
            Arrays.fill(noBoundsClasses, other.noBoundsCount, noBoundsClasses.length, null);
            noBoundsCount = other.noBoundsCount;
            if (writtenFormats.length < other.writtenCount) {
                writtenFormats = new BatchFormat[other.writtenFormats.length];
                writtenBytes = new long[other.writtenFormats.length];
            }
            System.arraycopy(other.writtenFormats, 0, writtenFormats, 0, other.writtenCount);
            System.arraycopy(other.writtenBytes, 0, writtenBytes, 0, other.writtenCount);
            writtenCount = other.writtenCount;
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

            if (formats.length < other.formatCount) {
                formats = new BatchFormat[other.formats.length];
                formatCalls = new int[other.formats.length];
            }
            System.arraycopy(other.formats, 0, formats, 0, other.formatCount);
            System.arraycopy(other.formatCalls, 0, formatCalls, 0, other.formatCount);
            formatCount = other.formatCount;
        }

        void clear() {
            draws = drawCalls = flushes = barriers = barriersWhileSorted = sorts = instancedDrawCalls = 0;
            cullTested = culled = cullNoBounds = 0;
            bytesSent = 0;
            Arrays.fill(noBoundsClasses, 0, noBoundsCount, null);
            noBoundsCount = 0;
            writtenCount = 0;
            Arrays.fill(causes, 0, causeCount, null);
            causeCount = 0;
            formatCount = 0;
        }
    }
}
