package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import org.apiguardian.api.API;
import org.joml.Vector2f;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * The triangles of a stroked line made by Stroker, independent of its width: the vertex shader places each vertex at
 * base + extrusion * min(halfWidth + fringe, limit) + fringeExtrusion * fringe, the fringe being one pixel of
 * anti-aliasing (0 without it). For the coverage, each vertex also gives:
 * - side: -1 or 1 on the edges of the line, 0 on its center line; |side| * (halfWidth + fringe) - halfWidth is the
 *   distance beyond the edge across the line, linear in each triangle;
 * - along: the distance beyond the end of a cap, along * (halfWidth, fringe) (dot product), -fringe elsewhere.
 */
@API(status = EXPERIMENTAL)
public final class StrokeGeometry {
    // Scale of an extrusion that has no limit
    public static final float NO_LIMIT = Float.MAX_VALUE;

    private final float[] bases;
    private final float[] extrusions;
    private final float[] fringes;
    private final float[] sides;
    private final float[] alongs;
    private final float[] limits;
    private final int[] indices;

    StrokeGeometry(float[] bases, float[] extrusions, float[] fringes, float[] sides, float[] alongs, float[] limits,
                   int[] indices) {
        this.bases = bases;
        this.extrusions = extrusions;
        this.fringes = fringes;
        this.sides = sides;
        this.alongs = alongs;
        this.limits = limits;
        this.indices = indices;
    }

    public int vertexCount() {
        return sides.length;
    }

    public int indexCount() {
        return indices.length;
    }

    public boolean isEmpty() {
        return indices.length == 0;
    }

    // Copies of the attributes: 2 floats per vertex (bases, extrusions, fringes, alongs) or 1 (sides, limits)
    public float[] bases() { return bases.clone(); }

    public float[] extrusions() { return extrusions.clone(); }

    public float[] fringeExtrusions() { return fringes.clone(); }

    public float[] sides() { return sides.clone(); }

    public float[] alongs() { return alongs.clone(); }

    public float[] limits() { return limits.clone(); }

    public int[] indices() { return indices.clone(); }

    // The attributes packed for the vertex shader: extrusion and fringe extrusion (4 floats per vertex)
    public float[] packedExtrusions() {
        float[] packed = new float[vertexCount() * 4];
        for (int i = 0; i < vertexCount(); ++i) {
            packed[i * 4] = extrusions[i * 2];
            packed[i * 4 + 1] = extrusions[i * 2 + 1];
            packed[i * 4 + 2] = fringes[i * 2];
            packed[i * 4 + 3] = fringes[i * 2 + 1];
        }

        return packed;
    }

    // The attributes packed for the vertex shader: side, along (2), limit (4 floats per vertex)
    public float[] packedCoverage() {
        float[] packed = new float[vertexCount() * 4];
        for (int i = 0; i < vertexCount(); ++i) {
            packed[i * 4] = sides[i];
            packed[i * 4 + 1] = alongs[i * 2];
            packed[i * 4 + 2] = alongs[i * 2 + 1];
            packed[i * 4 + 3] = limits[i];
        }

        return packed;
    }

    // Where the vertex shader places the vertex for this half width and fringe (the same formula)
    public Vector2f position(int vertex, float halfWidth, float fringe) {
        float scale = Math.min(halfWidth + fringe, limits[vertex]);
        return new Vector2f(
                bases[vertex * 2] + extrusions[vertex * 2] * scale + fringes[vertex * 2] * fringe,
                bases[vertex * 2 + 1] + extrusions[vertex * 2 + 1] * scale + fringes[vertex * 2 + 1] * fringe);
    }
}
