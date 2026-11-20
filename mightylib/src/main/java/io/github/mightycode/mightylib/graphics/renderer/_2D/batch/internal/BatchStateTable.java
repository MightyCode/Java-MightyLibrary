package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.internal.IGLSharedState;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The states of the batched draws of one engine, as ints: comparing two draws is comparing two ints.
 * <p>
 * A state packs four parts, high to low: the generation of the table (4 low bits, to refuse a state kept across a
 * reset), the material part (an IBatchMaterial interned by its content, 16 bits), the camera slot (10 bits) and the
 * color mode (1 bit). A camera slot is the camera of the draws whose matrices are the
 * same objects (Camera.getProjection, getView): its content is copied, and updated in place when it changed while no
 * draw of the slot waits in a batch; changed while draws of it wait (a camera moved between two draws of a view), the
 * slot is retired and a new one made, so the waiting draws keep the camera they were asked with.
 * <p>
 * Ids stay valid while a batch has draws waiting: the table is emptied (generation incremented) only when no batch
 * of the engine has any, once it holds more than its thresholds. A batch is started (batchStarted) before any state of
 * its draws is asked. Main thread only.
 */
public final class BatchStateTable implements IGLSharedState {
    public static final int CAMERA_BITS = 10;
    public static final int MAX_CAMERAS = 1 << CAMERA_BITS;
    public static final int MATERIAL_BITS = 16;
    public static final int MAX_MATERIAL_PARTS = 1 << MATERIAL_BITS;
    private static final int MATERIAL_SHIFT = CAMERA_BITS + 1;
    private static final int GENERATION_SHIFT = MATERIAL_SHIFT + MATERIAL_BITS;
    private static final int GENERATION_MASK = 0xF;

    // Above these, the table is emptied at the next moment no batch has draws waiting
    private static final int RESET_MATERIAL_PARTS = 4096;
    private static final int RESET_CAMERAS = 256;

    private final int resetMaterialParts;
    private final int resetCameras;

    // Material parts by content, and by id
    private final Map<IBatchMaterial, Integer> partIds = new HashMap<>();
    private IBatchMaterial[] parts = new IBatchMaterial[64];
    private int partCount;

    // Camera slots: the matrices of the camera (identity), their copies, the epoch of the last draw, retired or not
    private Matrix4f[] projectionSources = new Matrix4f[8];
    private Matrix4f[] viewSources = new Matrix4f[8];
    private Matrix4f[] projections = new Matrix4f[8];
    private Matrix4f[] views = new Matrix4f[8];
    private long[] lastUse = new long[8];
    private boolean[] retired = new boolean[8];
    private int cameraCount;
    private int lastCamera = -1;

    // Batches started and not flushed; the epoch changes each time none is left
    private int pendingBatches;
    private long epoch;
    private int generation;
    private int resets;

    public BatchStateTable() {
        this(RESET_MATERIAL_PARTS, RESET_CAMERAS);
    }

    // Thresholds of the reset (tests)
    BatchStateTable(int resetMaterialParts, int resetCameras) {
        this.resetMaterialParts = resetMaterialParts;
        this.resetCameras = resetCameras;
    }

    // ---- States ----

    // A state of the current generation
    public int state(int materialPart, int camera, boolean linear) {
        return (generation & GENERATION_MASK) << GENERATION_SHIFT | materialPart << MATERIAL_SHIFT | camera << 1
                | (linear ? 1 : 0);
    }

    // False for a state asked before a reset of the table (its ids may now name other things)
    public boolean isCurrent(int state) {
        return state >>> GENERATION_SHIFT == (generation & GENERATION_MASK) && state >= 0
                && materialPartOf(state) < partCount && cameraOf(state) < cameraCount;
    }

    public static int materialPartOf(int state) {
        return (state >>> MATERIAL_SHIFT) & (MAX_MATERIAL_PARTS - 1);
    }

    public static int cameraOf(int state) {
        return (state >>> 1) & (MAX_CAMERAS - 1);
    }

    public static boolean isLinear(int state) {
        return (state & 1) != 0;
    }

    /** Before the draws of a run: applies the state (material part, then camera and color mode). */
    public void apply(int state) {
        int camera = cameraOf(state);
        parts[materialPartOf(state)].apply(isLinear(state), projections[camera], views[camera]);
    }

    // The quads of the state can be drawn by instances now
    public boolean hasInstanced(int state) {
        return parts[materialPartOf(state)].hasInstanced();
    }

    /** Before the instanced draws of a run: the state with the instanced batch program. */
    public void applyInstanced(int state) {
        int camera = cameraOf(state);
        parts[materialPartOf(state)].applyInstanced(isLinear(state), projections[camera], views[camera]);
    }

    // ---- Material parts ----

    /**
     * The id of this content: the one of an equal part already interned, or a new one.
     * @throws IllegalStateException when MAX_MATERIAL_PARTS parts are used while draws wait
     */
    public int materialPart(IBatchMaterial part) {
        Integer id = partIds.get(part);
        if (id != null)
            return id;

        if (partCount == MAX_MATERIAL_PARTS)
            throw new IllegalStateException(MAX_MATERIAL_PARTS + " different materials in the batches without a"
                    + " flush: the table of the states can't be emptied while draws wait");
        if (partCount == parts.length)
            parts = Arrays.copyOf(parts, parts.length * 2);

        parts[partCount] = part;
        partIds.put(part, partCount);
        return partCount++;
    }

    public IBatchMaterial materialPartById(int id) {
        if (id < 0 || id >= partCount)
            throw new IllegalArgumentException("No material part " + id + " (" + partCount + " interned)");
        return parts[id];
    }

    // ---- Cameras ----

    /**
     * The slot of the camera whose matrices are these objects, with their current content.
     * @throws IllegalStateException when MAX_CAMERAS slots are used while draws wait
     */
    public int camera(Matrix4f projection, Matrix4f view) {
        int slot = lastCamera;
        if (slot < 0 || projectionSources[slot] != projection || viewSources[slot] != view || retired[slot])
            slot = findCamera(projection, view);

        if (slot < 0) {
            slot = newCamera(projection, view);
        } else if (!projections[slot].equals(projection) || !views[slot].equals(view)) {
            if (pendingBatches > 0 && lastUse[slot] == epoch) {
                // Draws of the slot wait with the previous content
                retired[slot] = true;
                slot = newCamera(projection, view);
            } else {
                projections[slot].set(projection);
                views[slot].set(view);
            }
        }

        lastUse[slot] = epoch;
        lastCamera = slot;
        return slot;
    }

    private int findCamera(Matrix4f projection, Matrix4f view) {
        for (int i = 0; i < cameraCount; ++i) {
            if (!retired[i] && projectionSources[i] == projection && viewSources[i] == view)
                return i;
        }

        return -1;
    }

    private int newCamera(Matrix4f projection, Matrix4f view) {
        if (cameraCount == MAX_CAMERAS)
            throw new IllegalStateException(MAX_CAMERAS + " cameras in the batches without a flush: the table of"
                    + " the states can't be emptied while draws wait");
        if (cameraCount == projections.length) {
            int size = projections.length * 2;
            projectionSources = Arrays.copyOf(projectionSources, size);
            viewSources = Arrays.copyOf(viewSources, size);
            projections = Arrays.copyOf(projections, size);
            views = Arrays.copyOf(views, size);
            lastUse = Arrays.copyOf(lastUse, size);
            retired = Arrays.copyOf(retired, size);
        }

        int slot = cameraCount++;
        projectionSources[slot] = projection;
        viewSources[slot] = view;
        projections[slot] = new Matrix4f(projection);
        views[slot] = new Matrix4f(view);
        retired[slot] = false;
        return slot;
    }

    // The copies of the camera of a state
    public Matrix4f projectionOf(int camera) {
        return projections[camera];
    }

    public Matrix4f viewOf(int camera) {
        return views[camera];
    }

    // ---- Lifetime of the ids ----

    // A batch gets its first draw since its last flush (before the states of its draws are asked)
    public void batchStarted() {
        ++pendingBatches;
    }

    // A started batch was flushed: when none is left, the ids of the table may change
    public void batchEnded() {
        if (pendingBatches == 0)
            throw new IllegalStateException("A batch ended without being started");

        if (--pendingBatches > 0)
            return;

        ++epoch;
        if (partCount > resetMaterialParts || cameraCount > resetCameras)
            reset();
    }

    private void reset() {
        partIds.clear();
        Arrays.fill(parts, 0, partCount, null);
        partCount = 0;

        Arrays.fill(projectionSources, 0, cameraCount, null);
        Arrays.fill(viewSources, 0, cameraCount, null);
        Arrays.fill(projections, 0, cameraCount, null);
        Arrays.fill(views, 0, cameraCount, null);
        cameraCount = 0;
        lastCamera = -1;

        ++generation;
        ++resets;
    }

    /** Changes when the ids are given again from 0: an id kept by a cache is valid only for its generation. */
    public int generation() {
        return generation;
    }

    public int materialParts() {
        return partCount;
    }

    public int cameras() {
        return cameraCount;
    }

    public int resets() {
        return resets;
    }

    public int pendingBatches() {
        return pendingBatches;
    }

    // The GL objects are the ones of the parts (freed by their users): only the references are dropped
    @Override
    public void delete() {
        reset();
    }
}
