package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.internal.IGLSharedState;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The states of the batched draws of one engine, as ints: comparing two draws is comparing two ints.
 * <p>
 * A state packs four parts, high to low: the generation of the table (4 low bits, to refuse a state kept across a
 * reset), the material part (an IBatchMaterial interned by its content, its format included, 16 bits), the camera
 * slot (10 bits) and the color mode (1 bit). A camera slot is the camera of the draws whose matrices are the same
 * objects (Camera.getProjection, getView): its content is copied, and updated in place when it changed while no
 * draw of the slot waits in a batch; changed while draws of it wait (a camera moved between two draws of a view), the
 * slot is retired and a new one made, so the waiting draws keep the camera they were asked with. Given by their
 * ShaderValues (camera(ShaderValue, ShaderValue)), matrices whose values are tracked (ShaderValue.trackChanges, the
 * values of a Camera) are known as changed by their versions; the others by comparing their content to the copies.
 * <p>
 * Ids stay valid while a batch has draws waiting: the table is emptied (generation incremented) only when no batch
 * of the engine has any, once it holds more than its thresholds. Above the threshold of the cameras alone, only the
 * cameras are emptied (the material parts keep their ids: partsGeneration unchanged, nothing rebuilt). Above the
 * threshold of the material parts, the table is emptied, unless more than half of it was used in the current frame (a
 * game that draws that many materials each frame): the threshold doubles instead, up to MAX_MATERIAL_PARTS / 2. When
 * no state fits (hasRoom false), the batch sends its draws first, then the table is emptied (DrawBatch2D.stateTable).
 * A batch is started (batchStarted) before any state of its draws is asked.
 * <p>
 * Each camera slot keeps the box of its view in world units (view culling, DrawBatch2D.isCulled), computed when the
 * slot gets its content: the corners of the clip space through the inverse of projection * view, their bounding box
 * (larger than the view for a turned camera). A camera that is not an affine 2D transform (a perspective) has none.
 * Main thread only.
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

    private int resetMaterialParts;
    private final int resetCameras;

    // Material parts by content, and by id; the frame each one was last drawn in
    private final Map<IBatchMaterial, Integer> partIds = new HashMap<>();
    private IBatchMaterial[] parts = new IBatchMaterial[64];
    private long[] partFrames = new long[64];
    private int partCount;

    // Camera slots: the matrices of the camera (identity), their copies, the epoch of the last draw, retired or not
    private Matrix4f[] projectionSources = new Matrix4f[8];
    private Matrix4f[] viewSources = new Matrix4f[8];
    private Matrix4f[] projections = new Matrix4f[8];
    private Matrix4f[] views = new Matrix4f[8];
    // The tracked values of the copies of a slot (null when given by matrices or not tracked), and their versions
    private ShaderValue[] projectionValues = new ShaderValue[8];
    private ShaderValue[] viewValues = new ShaderValue[8];
    private int[] projectionVersions = new int[8];
    private int[] viewVersions = new int[8];
    private long[] lastUse = new long[8];
    private boolean[] retired = new boolean[8];
    private int cameraCount;
    private int lastCamera = -1;
    // Box of the view of each slot (minX, minY, maxX, maxY in world units), and the linear part of the inverse of
    // its transform (world per clip unit: x from clip x, x from clip y, y from clip x, y from clip y)
    private float[] viewBoxes = new float[8 * 4];
    private float[] clipToWorld = new float[8 * 4];
    private boolean[] hasViewBox = new boolean[8];
    private final Matrix4f clip = new Matrix4f();

    // Batches started and not flushed; the epoch changes each time none is left
    private int pendingBatches;
    private long epoch;
    // Frames of the engine (frameStarted), for the material parts used each frame
    private long frame;
    private int generation;
    private int partsGeneration;
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

    // A state of the current generation (its material part counted as drawn in this frame)
    public int state(int materialPart, int camera, boolean linear) {
        if (materialPart >= 0 && materialPart < partCount)
            partFrames[materialPart] = frame;
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

    // The same state in this color mode
    public static int withLinear(int state, boolean linear) {
        return (state & ~1) | (linear ? 1 : 0);
    }

    /** Before the draws of a run: applies the state (material part, then camera and color mode). */
    public void apply(int state) {
        int camera = cameraOf(state);
        parts[materialPartOf(state)].apply(isLinear(state), projections[camera], views[camera]);
    }

    // The format of the draws of the state (the one of its batch program)
    public BatchFormat formatOf(int state) {
        return parts[materialPartOf(state)].format();
    }

    // Margin of the view culling of the draws of the state in world units (of its material part)
    public float cullMarginOf(int state) {
        return parts[materialPartOf(state)].cullMargin();
    }

    // The format of the instanced program of the state (mode INSTANCED), null when none
    public BatchFormat instancedFormatOf(int state) {
        return parts[materialPartOf(state)].instancedFormat();
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
        if (partCount == parts.length) {
            parts = Arrays.copyOf(parts, parts.length * 2);
            partFrames = Arrays.copyOf(partFrames, parts.length);
        }

        parts[partCount] = part;
        // Not drawn yet
        partFrames[partCount] = -1;
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
        return camera(projection, view, null, null);
    }

    /**
     * The slot of the camera of these values of matrices (their objects, Matrix4f), with their current content: for
     * tracked values (ShaderValue.isTracked) a change is known by their versions, the content is compared otherwise.
     * @throws ClassCastException when a value doesn't hold a Matrix4f
     * @throws IllegalStateException when MAX_CAMERAS slots are used while draws wait
     */
    public int camera(ShaderValue projectionValue, ShaderValue viewValue) {
        return camera((Matrix4f) projectionValue.getObject(), (Matrix4f) viewValue.getObject(),
                projectionValue.isTracked() ? projectionValue : null, viewValue.isTracked() ? viewValue : null);
    }

    // Values null: compared by content
    private int camera(Matrix4f projection, Matrix4f view, ShaderValue projectionValue, ShaderValue viewValue) {
        int slot = lastCamera;
        if (slot < 0 || projectionSources[slot] != projection || viewSources[slot] != view || retired[slot])
            slot = findCamera(projection, view);

        if (slot < 0) {
            slot = newCamera(projection, view);
            track(slot, projectionValue, viewValue);
        } else if (changed(slot, projection, view, projectionValue, viewValue)) {
            if (pendingBatches > 0 && lastUse[slot] == epoch) {
                // Draws of the slot wait with the previous content
                retired[slot] = true;
                slot = newCamera(projection, view);
            } else {
                projections[slot].set(projection);
                views[slot].set(view);
                updateViewBox(slot);
            }
            track(slot, projectionValue, viewValue);
        } else if (projectionValues[slot] != projectionValue || viewValues[slot] != viewValue) {
            // Same content, given by other values (or no longer tracked): the versions of these from now
            track(slot, projectionValue, viewValue);
        }

        lastUse[slot] = epoch;
        lastCamera = slot;
        return slot;
    }

    // The content of the matrices differs from the copies of the slot: by the versions when both values are the
    // tracked ones the copies were made from, by the content otherwise
    private boolean changed(int slot, Matrix4f projection, Matrix4f view, ShaderValue projectionValue,
                            ShaderValue viewValue) {
        if (projectionValue != null && viewValue != null && projectionValues[slot] == projectionValue
                && viewValues[slot] == viewValue)
            return projectionValue.version() != projectionVersions[slot] || viewValue.version() != viewVersions[slot];

        return !projections[slot].equals(projection) || !views[slot].equals(view);
    }

    // The copies of the slot are the content of these values now (both tracked, or compared by content after)
    private void track(int slot, ShaderValue projectionValue, ShaderValue viewValue) {
        boolean bothTracked = projectionValue != null && viewValue != null;
        projectionValues[slot] = bothTracked ? projectionValue : null;
        viewValues[slot] = bothTracked ? viewValue : null;
        projectionVersions[slot] = bothTracked ? projectionValue.version() : 0;
        viewVersions[slot] = bothTracked ? viewValue.version() : 0;
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
            projectionValues = Arrays.copyOf(projectionValues, size);
            viewValues = Arrays.copyOf(viewValues, size);
            projectionVersions = Arrays.copyOf(projectionVersions, size);
            viewVersions = Arrays.copyOf(viewVersions, size);
            lastUse = Arrays.copyOf(lastUse, size);
            retired = Arrays.copyOf(retired, size);
            viewBoxes = Arrays.copyOf(viewBoxes, size * 4);
            clipToWorld = Arrays.copyOf(clipToWorld, size * 4);
            hasViewBox = Arrays.copyOf(hasViewBox, size);
        }

        int slot = cameraCount++;
        projectionSources[slot] = projection;
        viewSources[slot] = view;
        // The copies of a slot emptied by a reset are reused: a camera moved between draws allocates nothing
        if (projections[slot] == null) {
            projections[slot] = new Matrix4f(projection);
            views[slot] = new Matrix4f(view);
        } else {
            projections[slot].set(projection);
            views[slot].set(view);
        }
        retired[slot] = false;
        updateViewBox(slot);
        return slot;
    }

    private void updateViewBox(int slot) {
        clip.set(projections[slot]).mul(views[slot]);
        hasViewBox[slot] = ViewBox(clip, viewBoxes, clipToWorld, slot * 4);
    }

    /**
     * The box of the clip space (-1..1) in world units, for draws at z 0: written at box[at..at + 3] (minX, minY,
     * maxX, maxY), and the linear part of the inverse transform at inverse[at..at + 3]. False when clip is not an
     * invertible affine 2D transform (a perspective, a flat scale): nothing is culled.
     */
    static boolean ViewBox(Matrix4fc clip, float[] box, float[] inverse, int at) {
        if (clip.m03() != 0f || clip.m13() != 0f || clip.m23() != 0f || clip.m33() != 1f)
            return false;

        // clip = A * world + t, A = [a c; b d] (columns: world x, world y)
        float a = clip.m00(), b = clip.m01(), c = clip.m10(), d = clip.m11();
        float tx = clip.m30(), ty = clip.m31();
        float determinant = a * d - b * c;
        if (determinant == 0f || !Float.isFinite(determinant))
            return false;

        // world = A^-1 * (clip - t), A^-1 = [p q; r s]
        float p = d / determinant, q = -c / determinant, r = -b / determinant, s = a / determinant;
        float centerX = -(p * tx + q * ty), centerY = -(r * tx + s * ty);
        // Half extents of the image of the square -1..1: the sum of the absolute columns
        float halfX = Math.abs(p) + Math.abs(q), halfY = Math.abs(r) + Math.abs(s);
        if (!Float.isFinite(centerX) || !Float.isFinite(centerY) || !Float.isFinite(halfX) || !Float.isFinite(halfY))
            return false;

        // A little wider than the view: the rounding of floats never culls a draw that reaches a pixel of its edge
        float slackX = 1e-5f * (Math.abs(centerX) + halfX), slackY = 1e-5f * (Math.abs(centerY) + halfY);
        box[at] = centerX - halfX - slackX;
        box[at + 1] = centerY - halfY - slackY;
        box[at + 2] = centerX + halfX + slackX;
        box[at + 3] = centerY + halfY + slackY;
        inverse[at] = p;
        inverse[at + 1] = q;
        inverse[at + 2] = r;
        inverse[at + 3] = s;
        return true;
    }

    /**
     * Room for one more state: a new material part and a new camera fit. When not, the draws waiting are sent and the
     * table emptied before a state is asked (DrawBatch2D.stateTable).
     */
    public boolean hasRoom() {
        return partCount < MAX_MATERIAL_PARTS && cameraCount < MAX_CAMERAS;
    }

    // The copies of the camera of a state
    public Matrix4f projectionOf(int camera) {
        return projections[camera];
    }

    public Matrix4f viewOf(int camera) {
        return views[camera];
    }

    // The object of the matrix of view of the slot (what the camera hands out: the identity of the camera)
    public Matrix4f viewSourceOf(int camera) {
        return viewSources[camera];
    }

    // The boxes of the views of the slots in use, by the object of their matrix of view (the tools of the culling)
    public void snapshotViewBoxes(java.util.Map<Matrix4f, float[]> out) {
        for (int i = 0; i < cameraCount; ++i) {
            if (hasViewBox[i] && !retired[i] && viewSources[i] != null)
                out.put(viewSources[i], new float[]{viewBoxes[i * 4], viewBoxes[i * 4 + 1], viewBoxes[i * 4 + 2],
                        viewBoxes[i * 4 + 3]});
        }
    }

    // The box of the view of a camera slot in world units, when it has one (an affine 2D camera)
    public boolean hasViewBox(int camera) {
        return hasViewBox[camera];
    }

    public float viewMinX(int camera) { return viewBoxes[camera * 4]; }
    public float viewMinY(int camera) { return viewBoxes[camera * 4 + 1]; }
    public float viewMaxX(int camera) { return viewBoxes[camera * 4 + 2]; }
    public float viewMaxY(int camera) { return viewBoxes[camera * 4 + 3]; }

    /**
     * World units of one clip unit along each axis of the clip space for a camera slot with a box: 0 world x per
     * clip x, 1 world x per clip y, 2 world y per clip x, 3 world y per clip y.
     */
    public float clipToWorld(int camera, int component) {
        return clipToWorld[camera * 4 + component];
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
        if (partCount > resetMaterialParts || partCount == MAX_MATERIAL_PARTS) {
            if (partCount < MAX_MATERIAL_PARTS && keepsUsedParts())
                resetMaterialParts *= 2;
            else
                reset();
        }

        if (cameraCount > resetCameras || cameraCount == MAX_CAMERAS)
            resetCameras();
    }

    // More than half of the threshold drawn in this frame: emptying the table would build them all again each frame
    private boolean keepsUsedParts() {
        if (resetMaterialParts * 2 > MAX_MATERIAL_PARTS / 2)
            return false;

        int used = 0;
        for (int i = 0; i < partCount; ++i) {
            if (partFrames[i] == frame)
                ++used;
        }

        return used * 2 > resetMaterialParts;
    }

    // A new frame of the engine (before its views draw)
    public void frameStarted() {
        ++frame;
    }

    private void reset() {
        partIds.clear();
        Arrays.fill(parts, 0, partCount, null);
        partCount = 0;
        ++partsGeneration;
        resetCameras();
    }

    // The ids of the states change (generation), those of the material parts stay
    private void resetCameras() {
        Arrays.fill(projectionSources, 0, cameraCount, null);
        Arrays.fill(viewSources, 0, cameraCount, null);
        Arrays.fill(projectionValues, 0, cameraCount, null);
        Arrays.fill(viewValues, 0, cameraCount, null);
        cameraCount = 0;
        lastCamera = -1;

        ++generation;
        ++resets;
    }

    /** Changes when the ids of the states change (a state kept by a cache is valid only for its generation). */
    public int generation() {
        return generation;
    }

    /** Changes when the ids of the material parts are given again from 0 (an id kept by a material). */
    public int partsGeneration() {
        return partsGeneration;
    }

    // Material parts above which the table is emptied (doubles when that many are drawn each frame)
    public int materialPartsThreshold() {
        return resetMaterialParts;
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
