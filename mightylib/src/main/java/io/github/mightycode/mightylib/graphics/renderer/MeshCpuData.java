package io.github.mightycode.mightylib.graphics.renderer;

import java.util.ArrayList;
import java.util.List;

/**
 * The copy on the CPU of what a Mesh sent to its float vbos and its ebo (Mesh.keepCpuData), kept up to date by its
 * updates, read by the batched draws of MeshRenderer. Without OpenGL. A vbo added before the copy was asked, or of
 * integers, is unknown (null) until updated.
 */
final class MeshCpuData {
    private static final int[] NO_INDICES = new int[0];

    private final List<float[]> vbos = new ArrayList<>();
    private int[] indices = NO_INDICES;
    private int indexCount;
    // -1 without index; Integer.MAX_VALUE when an index is unknown (part of the ebo set before the copy)
    private int maxIndex = -1;
    private boolean indicesKnown = true;

    // A new vbo at this index (rank of the vbo in the mesh), its data copied, null when not kept (integers)
    void addVbo(int vbo, float[] data) {
        while (vbos.size() <= vbo)
            vbos.add(null);
        vbos.set(vbo, data == null ? null : data.clone());
    }

    // The whole vbo replaced (the array is copied: the caller may change it after)
    void setVbo(int vbo, float[] data) {
        float[] kept = vbos.get(vbo);
        if (kept != null && kept.length == data.length)
            System.arraycopy(data, 0, kept, 0, data.length);
        else
            vbos.set(vbo, data.clone());
    }

    // A part of the vbo replaced from this offset in bytes (like glBufferSubData), what is past its end ignored
    void setSubVbo(int vbo, float[] data, int byteOffset) {
        float[] kept = vbos.get(vbo);
        if (kept == null)
            return;

        int start = byteOffset / Float.BYTES;
        int count = Math.max(0, Math.min(data.length, kept.length - start));
        System.arraycopy(data, 0, kept, start, count);
    }

    // The vbo, null when unknown; read only
    float[] vbo(int vbo) {
        return vbo < vbos.size() ? vbos.get(vbo) : null;
    }

    // The ebo replaced (the array copied), count of its indices drawn
    void setIndices(int[] data, int count) {
        if (indices.length == data.length)
            System.arraycopy(data, 0, indices, 0, data.length);
        else
            indices = data.clone();
        indicesKnown = true;
        setIndexCount(count);
    }

    // A part of the ebo replaced from this offset in bytes (like glBufferSubData), what is past its end ignored
    void updateIndices(int[] data, int byteOffset) {
        int start = byteOffset / Integer.BYTES;
        int count = Math.max(0, Math.min(data.length, indices.length - start));
        System.arraycopy(data, 0, indices, start, count);
        setIndexCount(indexCount);
    }

    // The count of indices drawn (setEboNumberIndex)
    void setIndexCount(int count) {
        indexCount = count;
        if (!indicesKnown || count > indices.length) {
            maxIndex = Integer.MAX_VALUE;
            return;
        }

        int max = -1;
        for (int i = 0; i < count; ++i)
            max = Math.max(max, indices[i]);
        maxIndex = max;
    }

    // The ebo set before the copy was asked: unknown until set again
    void forgetIndices() {
        indicesKnown = false;
        setIndexCount(indexCount);
    }

    int[] indices() {
        return indices;
    }

    int indexCount() {
        return indexCount;
    }

    // The largest index drawn (-1 without index, Integer.MAX_VALUE when unknown)
    int maxIndex() {
        return maxIndex;
    }
}
