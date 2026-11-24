package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import org.joml.Matrix4f;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The tools to check the view culling by eye (command batch cull), shared by the batches of an engine: the test zone
 * reduced by a number of pixels, the box of each tested draw shown, the boxes of the views frozen (the camera moves,
 * the test stays), the draws culled of a frame listed. All off by default, nothing costs while they are. Main thread
 * only.
 */
public final class ViewCullTools {
    /** Most draws kept in the list of a frame. */
    public static final int MAX_LISTED = 1000;

    private int inset;
    private boolean show;
    private boolean recording;
    private boolean active;
    // The boxes frozen by the object of the matrix of view of their camera slot (its content changes, not its identity)
    private final Map<Matrix4f, float[]> frozen = new IdentityHashMap<>();
    private boolean frozenOn;

    // The draws culled in the current frame of the engine, and in the last one
    private JSONArray current = new JSONArray();
    private JSONArray last = new JSONArray();
    private boolean currentTruncated;
    private boolean lastTruncated;
    private boolean hasLast;

    /** True when a tool is on: the batches then ask the others (the rest costs one boolean per test). */
    public boolean active() {
        return active;
    }

    private void refresh() {
        active = inset > 0 || show || recording || frozenOn;
    }

    // ---- Zone of test ----

    /** The test zone is the view reduced by this many pixels of the viewport on each side (0: the view). */
    public void setInset(int pixels) {
        inset = Math.max(0, pixels);
        refresh();
    }

    public int inset() {
        return inset;
    }

    // ---- Boxes shown ----

    /** The box of each tested draw is drawn by the debug drawings of the view (green drawn, red culled). */
    public void setShow(boolean show) {
        this.show = show;
        refresh();
    }

    public boolean isShow() {
        return show;
    }

    // ---- Frozen boxes ----

    /**
     * Keeps the box of the view of each camera slot of the table: the cameras of those matrices of view are tested
     * against it from now on, whatever they see. A camera new after this call keeps its box.
     */
    public void freeze(BatchStateTable table) {
        frozen.clear();
        table.snapshotViewBoxes(frozen);
        frozenOn = true;
        refresh();
    }

    public void unfreeze() {
        frozen.clear();
        frozenOn = false;
        refresh();
    }

    public boolean isFrozen() {
        return frozenOn;
    }

    /** The frozen box (minX, minY, maxX, maxY) of the camera whose matrix of view is this object, null when none. */
    public float[] frozenBox(Matrix4f viewSource) {
        return frozenOn ? frozen.get(viewSource) : null;
    }

    // ---- List of the culled ----

    /** The draws culled of each frame are kept (the last complete frame is listed): costs while on. */
    public void setRecording(boolean recording) {
        if (this.recording == recording)
            return;

        this.recording = recording;
        current = new JSONArray();
        last = new JSONArray();
        currentTruncated = lastTruncated = false;
        hasLast = false;
        refresh();
    }

    public boolean isRecording() {
        return recording;
    }

    /** A draw culled (class of what drew it, its box in world units, its camera slot). */
    public void recordCulled(String owner, float minX, float minY, float maxX, float maxY, int camera) {
        if (!recording)
            return;
        if (current.length() >= MAX_LISTED) {
            currentTruncated = true;
            return;
        }

        current.put(new JSONObject().put("class", owner).put("camera", camera)
                .put("box", new JSONArray().put(minX).put(minY).put(maxX).put(maxY)));
    }

    /** A new frame of the engine: what was recorded since the previous one is the last complete frame. */
    public void frameStarted() {
        if (!recording)
            return;

        last = current;
        lastTruncated = currentTruncated;
        hasLast = true;
        current = new JSONArray();
        currentTruncated = false;
    }

    /** The draws culled of the last complete frame (null before one): "culled" array and "truncated". */
    public JSONObject listLast() {
        if (!hasLast)
            return null;

        return new JSONObject().put("culled", last).put("count", last.length()).put("truncated", lastTruncated)
                .put("limit", MAX_LISTED);
    }

    /** The state of the tools, for the data of the command. */
    public JSONObject describe() {
        return new JSONObject().put("inset", inset).put("show", show).put("frozen", frozenOn)
                .put("frozenCameras", frozen.size()).put("recording", recording);
    }
}
