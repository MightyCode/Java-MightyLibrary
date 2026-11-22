package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/*
 * The vertices of a geometric shape for stroke2DV.glsl: fills, fringes of fills and strokes gathered in one mesh,
 * drawn in the order they were added (one draw call, the stroke over the fill). Base positions and indices go
 * through the hooks of Shape2DRenderer, the other attributes through the vertex buffers created here.
 */
final class StrokeMesh {
    static final float KIND_FILL = 0f;
    static final float KIND_FILL_FRINGE = 1f;
    static final float KIND_STROKE = 2f;
    static final float FILL_COLOR = 0f;
    static final float STROKE_COLOR = 1f;

    private static final int EXTRUSION_CHANNEL = 2;
    private static final int COVERAGE_CHANNEL = 3;
    private static final int PAINT_CHANNEL = 4;

    private final int extrusionIndex;
    private final int coverageIndex;
    private final int paintIndex;

    private final List<float[]> bases = new ArrayList<>();
    private final List<float[]> extrusions = new ArrayList<>();
    private final List<float[]> coverages = new ArrayList<>();
    private final List<float[]> paints = new ArrayList<>();
    private final List<int[]> indexParts = new ArrayList<>();
    private int vertexCount;
    // The attributes sent by the last upload, kept for the batch
    private float[] uploadedExtrusions = new float[0];
    private float[] uploadedCoverages = new float[0];
    private float[] uploadedPaints = new float[0];

    // Bounding box of the bases: min x, min y, width, height
    private float minX, minY, maxX, maxY;

    StrokeMesh(Mesh mesh) {
        extrusionIndex = mesh.addVboFloat(EXTRUSION_CHANNEL, new float[]{}, 4, Mesh.STATIC_STORE);
        coverageIndex = mesh.addVboFloat(COVERAGE_CHANNEL, new float[]{}, 4, Mesh.STATIC_STORE);
        paintIndex = mesh.addVboFloat(PAINT_CHANNEL, new float[]{}, 2, Mesh.STATIC_STORE);
    }

    void clear() {
        bases.clear();
        extrusions.clear();
        coverages.clear();
        paints.clear();
        indexParts.clear();
        vertexCount = 0;
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
    }

    // Triangles fully covered: positions (2 floats per vertex) and their indices
    void addFill(float[] positions, int[] indices, float color) {
        int count = positions.length / 2;
        float[] extrusion = new float[count * 4];
        float[] coverage = new float[count * 4];
        for (int i = 0; i < count; ++i) {
            // Inside along, no limit
            coverage[i * 4 + 2] = -1f;
            coverage[i * 4 + 3] = StrokeGeometry.NO_LIMIT;
        }
        add(positions, extrusion, coverage, count, indices, KIND_FILL, color);
    }

    // The stroke of a line
    void addStroke(StrokeGeometry geometry, float color) {
        add(geometry.bases(), geometry.packedExtrusions(), geometry.packedCoverage(), geometry.vertexCount(),
                geometry.indices(), KIND_STROKE, color);
    }

    /*
     * The fringe of a fill from the stroke of its closed outline: the vertices on the inner side (and the centers)
     * are kept on the edge, the outer ones extruded by the fringe; outerSide: the side (1 or -1) of the outside.
     */
    void addFillFringe(StrokeGeometry outline, float outerSide, float color) {
        float[] extrusion = outline.packedExtrusions();
        float[] coverage = outline.packedCoverage();
        for (int i = 0; i < outline.vertexCount(); ++i) {
            if (coverage[i * 4] * outerSide <= 0f) {
                extrusion[i * 4] = extrusion[i * 4 + 1] = extrusion[i * 4 + 2] = extrusion[i * 4 + 3] = 0f;
                coverage[i * 4] = 0f;
            } else {
                coverage[i * 4] = 1f;
            }
        }
        add(outline.bases(), extrusion, coverage, outline.vertexCount(), outline.indices(), KIND_FILL_FRINGE, color);
    }

    private void add(float[] base, float[] extrusion, float[] coverage, int count, int[] indices, float kind,
                     float color) {
        if (count == 0)
            return;

        float[] paint = new float[count * 2];
        for (int i = 0; i < count; ++i) {
            paint[i * 2] = kind;
            paint[i * 2 + 1] = color;
            minX = Math.min(minX, base[i * 2]);
            maxX = Math.max(maxX, base[i * 2]);
            minY = Math.min(minY, base[i * 2 + 1]);
            maxY = Math.max(maxY, base[i * 2 + 1]);
        }

        int[] shifted = new int[indices.length];
        for (int i = 0; i < indices.length; ++i)
            shifted[i] = indices[i] + vertexCount;

        bases.add(base);
        extrusions.add(extrusion);
        coverages.add(coverage);
        paints.add(paint);
        indexParts.add(shifted);
        vertexCount += count;
    }

    int vertexCount() {
        return vertexCount;
    }

    float[] bases() {
        return concat(bases, vertexCount * 2);
    }

    int[] indices() {
        int total = 0;
        for (int[] part : indexParts)
            total += part.length;

        int[] all = new int[total];
        int offset = 0;
        for (int[] part : indexParts) {
            System.arraycopy(part, 0, all, offset, part.length);
            offset += part.length;
        }
        return all;
    }

    // min x, min y, width, height of the bases (0 when empty)
    void writeBounds(Vector4f bounds) {
        if (vertexCount == 0)
            bounds.set(0f, 0f, 0f, 0f);
        else
            bounds.set(minX, minY, maxX - minX, maxY - minY);
    }

    // The attributes other than the bases, sent to the vertex buffers of this mesh
    void upload(Mesh mesh) {
        uploadedExtrusions = concat(extrusions, vertexCount * 4);
        uploadedCoverages = concat(coverages, vertexCount * 4);
        uploadedPaints = concat(paints, vertexCount * 2);
        mesh.updateVbo(uploadedExtrusions, extrusionIndex);
        mesh.updateVbo(uploadedCoverages, coverageIndex);
        mesh.updateVbo(uploadedPaints, paintIndex);
    }

    // The attributes of the last upload (not copies: read only)
    float[] uploadedExtrusions() {
        return uploadedExtrusions;
    }

    float[] uploadedCoverages() {
        return uploadedCoverages;
    }

    float[] uploadedPaints() {
        return uploadedPaints;
    }

    private static float[] concat(List<float[]> parts, int total) {
        float[] all = new float[total];
        int offset = 0;
        for (float[] part : parts) {
            System.arraycopy(part, 0, all, offset, part.length);
            offset += part.length;
        }
        return all;
    }
}
