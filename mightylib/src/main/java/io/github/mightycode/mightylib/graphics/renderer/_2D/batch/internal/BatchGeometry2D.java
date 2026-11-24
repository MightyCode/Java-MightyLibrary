package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer.utils.EDrawMode;
import org.joml.Matrix4fc;

/**
 * The geometry of the renderers written in the 2D batch: which can be, and their vertices transformed on the CPU by
 * their model like their vertex shader does (model * (x, y, 0, 1), affine). Shared by the 2D renderers.
 */
public final class BatchGeometry2D {
    private BatchGeometry2D() {}

    // The batch keeps no depth, and draws its indices as a list of triangles (not lines, strips or fans)
    public static boolean CanBatch(boolean depthTest, EDrawMode drawMode) {
        return !depthTest && drawMode == EDrawMode.Triangles;
    }

    /**
     * The model keeps the plane z = 0 (a translation, rotation and scale in it, any z translation): what the batch
     * programs of instances rebuild from its two rows (WriteModelRows).
     */
    public static boolean IsAffine2D(Matrix4fc model) {
        return model.m02() == 0f && model.m12() == 0f && model.m03() == 0f && model.m13() == 0f
                && model.m23() == 0f && model.m33() == 1f;
    }

    // The two rows of the 2D affine model: m00 m10 m30, then m01 m11 m31 (instance attributes of 3 floats each)
    public static void WriteModelRows(DrawBatch2D batch, Matrix4fc model) {
        batch.put(model.m00(), model.m10(), model.m30());
        batch.put(model.m01(), model.m11(), model.m31());
    }

    // The x of the 2D position (x, y) by the model, like the vertex shaders of the 2D renderers
    public static float TransformX(Matrix4fc model, float x, float y) {
        return model.m00() * x + model.m10() * y + model.m30();
    }

    // The y of the 2D position (x, y) by the model
    public static float TransformY(Matrix4fc model, float x, float y) {
        return model.m01() * x + model.m11() * y + model.m31();
    }

    /**
     * One draw of triangles: vertexCount vertices (positions by pairs, transformed by the model; texture coordinates
     * by pairs, or null for (0, 0): the white texture), all of one color, then indexCount indices.
     */
    public static void WriteTriangles(DrawBatch2D batch, int state, int layer, float depth, Matrix4fc model,
                                      float[] positions, float[] textures, int vertexCount, int color,
                                      int[] indices, int indexCount) {
        WriteTriangles(batch, state, layer, depth, model, positions, textures, null, vertexCount, color, indices,
                indexCount);
    }

    /**
     * Same with a color per vertex when colors is not null (RGBA, 4 floats in 0..1 each, multiplied by the batch
     * shader like a texture of white), the color given otherwise.
     */
    public static void WriteTriangles(DrawBatch2D batch, int state, int layer, float depth, Matrix4fc model,
                                      float[] positions, float[] textures, float[] colors, int vertexCount,
                                      int color, int[] indices, int indexCount) {
        batch.beginDraw(state, layer, depth);
        for (int i = 0; i < vertexCount; ++i) {
            float x = positions[2 * i];
            float y = positions[2 * i + 1];
            float u = textures == null ? 0f : textures[2 * i];
            float v = textures == null ? 0f : textures[2 * i + 1];
            int rgba = colors == null ? color : PackRGBA(colors, 4 * i);
            batch.vertex(TransformX(model, x, y), TransformY(model, x, y), u, v, rgba);
        }
        for (int i = 0; i < indexCount; ++i)
            batch.index(indices[i]);
        batch.endDraw();
    }

    /**
     * The box of the first vertexCount positions (pairs x, y) written at box[0..3] (minX, minY, maxX, maxY): false
     * without vertex or with a position that is not finite (no box: never culled).
     */
    public static boolean Box(float[] positions, int vertexCount, float[] box) {
        if (positions == null || vertexCount <= 0 || positions.length < vertexCount * 2)
            return false;

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vertexCount; ++i) {
            float x = positions[2 * i], y = positions[2 * i + 1];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(maxX) || !Float.isFinite(maxY))
            return false;

        box[0] = minX;
        box[1] = minY;
        box[2] = maxX;
        box[3] = maxY;
        return true;
    }

    // RGBA floats in 0..1 (clamped, rounded like the GPU converts a float to a normalized byte) to RGBA8 in an int
    public static int PackRGBA(float[] colors, int at) {
        return Channel(colors[at]) << 24 | Channel(colors[at + 1]) << 16 | Channel(colors[at + 2]) << 8
                | Channel(colors[at + 3]);
    }

    private static int Channel(float value) {
        return Math.round(Math.max(0f, Math.min(1f, value)) * 255f);
    }
}
