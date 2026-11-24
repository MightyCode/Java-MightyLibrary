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
    // World units of one pixel of the viewport (columns: a pixel along x, a pixel along y), NaN when unknown
    private float pixelXX = Float.NaN, pixelXY = Float.NaN, pixelYX = Float.NaN, pixelYY = Float.NaN;

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
        pixelXX = pixelXY = pixelYX = pixelYY = Float.NaN;
        return this;
    }

    /**
     * The world vectors of one pixel of the viewport of the draw, along the x of the screen (xx, xy) and its y (yx,
     * yy): what worldPerPixel and localPerPixel are computed from.
     */
    @API(status = INTERNAL)
    public ViewCullBounds pixelToWorld(float xx, float xy, float yx, float yy) {
        pixelXX = xx;
        pixelXY = xy;
        pixelYX = yx;
        pixelYY = yy;
        return this;
    }

    /**
     * The largest distance in world units covered by one pixel of the viewport of the draw, over the directions
     * (shapeWorldPerPixel of the shaders): NaN when unknown (a camera that is not an affine 2D transform, no
     * viewport).
     */
    public float worldPerPixel() {
        return LargestStretch(pixelXX, pixelXY, pixelYX, pixelYY);
    }

    /**
     * The same in units of the space of the model (the model matrix inverted: a scale of 2 halves it): NaN when
     * unknown or for a flat model.
     */
    public float localPerPixel() {
        if (model == null)
            return worldPerPixel();

        // The linear part of the model in the plane, inverted: [a c; b d]^-1
        float a = model.m00(), b = model.m01(), c = model.m10(), d = model.m11();
        float determinant = a * d - b * c;
        if (determinant == 0f || !Float.isFinite(determinant))
            return Float.NaN;

        float ia = d / determinant, ib = -b / determinant, ic = -c / determinant, id = a / determinant;
        return LargestStretch(ia * pixelXX + ic * pixelXY, ib * pixelXX + id * pixelXY,
                ia * pixelYX + ic * pixelYY, ib * pixelYX + id * pixelYY);
    }

    // The largest singular value of the 2D linear map of columns (xx, xy) and (yx, yy)
    private static float LargestStretch(float xx, float xy, float yx, float yy) {
        double a = (double) xx * xx + (double) xy * xy;
        double c = (double) yx * yx + (double) yy * yy;
        double b = (double) xx * yx + (double) xy * yy;
        double halfTrace = 0.5 * (a + c);
        double largest = halfTrace + Math.sqrt(Math.max(halfTrace * halfTrace - (a * c - b * b), 0.0));
        return (float) Math.sqrt(largest);
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
