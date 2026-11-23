package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import org.apiguardian.api.API;
import org.joml.Matrix4fc;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * The box of a batched draw for the view culling (DrawBatch2D.isCulled): a box in the space of the model (local,
 * transformed by the model matrix of the draw: the box of its 4 corners) or in world units (world), widened by a
 * margin in world units and a margin in pixels of the viewport. No box: the draw is never culled. One object reused
 * for every draw (nothing allocated). Main thread only.
 */
public final class ViewCullBounds {
    private boolean hasBox;
    private boolean local;
    private float minX, minY, maxX, maxY;
    private float worldMargin;
    private float pixelMargin;
    // The model matrix a local box is transformed by (null: the identity)
    private Matrix4fc model;
    // The class of what draws (counters and tools of the command batch cull)
    private Class<?> owner;

    /** A box in the space of the model: transformed by the model matrix of the draw (the box of its 4 corners). */
    public ViewCullBounds local(float minX, float minY, float maxX, float maxY) {
        return box(true, minX, minY, maxX, maxY);
    }

    /** A box in world units (the space the camera of the draw looks at), whatever the model matrix. */
    public ViewCullBounds world(float minX, float minY, float maxX, float maxY) {
        return box(false, minX, minY, maxX, maxY);
    }

    private ViewCullBounds box(boolean local, float minX, float minY, float maxX, float maxY) {
        this.hasBox = true;
        this.local = local;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        return this;
    }

    /**
     * Widens the box (added to the margins already given): world units on each side (vertices moved by a shader, a
     * stroke in world units), and pixels of the viewport on each side (anti-aliasing, a stroke in pixels).
     */
    public ViewCullBounds margin(float world, float pixels) {
        worldMargin += Math.max(0f, world);
        pixelMargin += Math.max(0f, pixels);
        return this;
    }

    /**
     * Before the box of a draw is asked: no box, no margin, the model matrix of the draw (null: the identity), the
     * class of what draws.
     */
    @API(status = INTERNAL)
    public ViewCullBounds clear(Matrix4fc model, Class<?> owner) {
        hasBox = false;
        local = false;
        minX = minY = maxX = maxY = 0f;
        worldMargin = 0f;
        pixelMargin = 0f;
        this.model = model;
        this.owner = owner;
        return this;
    }

    public boolean hasBox() { return hasBox; }
    public boolean isLocal() { return local; }
    public float minX() { return minX; }
    public float minY() { return minY; }
    public float maxX() { return maxX; }
    public float maxY() { return maxY; }
    public float worldMargin() { return worldMargin; }
    public float pixelMargin() { return pixelMargin; }
    public Matrix4fc model() { return model; }
    public Class<?> owner() { return owner; }
}
