package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchGeometry2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The copy on the CPU of what a Mesh sent to its float vbos and its ebo (Mesh.keepCpuData), kept up to date by its
 * updates, read by the batched draws of MeshRenderer. Without OpenGL. A vbo added before the copy was asked, or of
 * integers, is unknown (null) until updated.
 * <p>
 * The updates of a known vbo are only written here: the vbo is marked dirty (the range of floats changed since its
 * last upload) and the Mesh sends it to the GPU before a draw that reads it (Mesh.bind), once for all the updates
 * in between.
 */
final class MeshCpuData {
    private static final int[] NO_INDICES = new int[0];

    private final List<float[]> vbos = new ArrayList<>();
    // Per vbo, the range of floats [dirtyStart, dirtyEnd[ changed since the last upload; dirtyStart -1 when clean
    private int[] dirtyStart = new int[4];
    private int[] dirtyEnd = new int[4];
    private boolean anyDirty;
    private int[] indices = NO_INDICES;
    private int indexCount;
    // -1 without index; Integer.MAX_VALUE when an index is unknown (part of the ebo set before the copy)
    private int maxIndex = -1;
    private boolean indicesKnown = true;
    // Changes with the content of the vbos; the box of the positions of a vbo computed at a version (view culling)
    private int version;
    private int boxVbo = -1;
    private int boxVersion;
    private boolean hasBox;
    private final float[] box = new float[4];

    // A new vbo at this index (rank of the vbo in the mesh), its data copied, null when not kept (integers)
    void addVbo(int vbo, float[] data) {
        ++version;
        if (dirtyStart.length <= vbo) {
            dirtyStart = Arrays.copyOf(dirtyStart, Math.max(vbo + 1, dirtyStart.length * 2));
            dirtyEnd = Arrays.copyOf(dirtyEnd, dirtyStart.length);
        }
        // Sent with its creation, like the unknown ones before it
        while (vbos.size() <= vbo) {
            dirtyStart[vbos.size()] = -1;
            vbos.add(null);
        }
        vbos.set(vbo, data == null ? null : data.clone());
        dirtyStart[vbo] = -1;
    }

    // The whole vbo replaced (the array is copied: the caller may change it after)
    void setVbo(int vbo, float[] data) {
        ++version;
        float[] kept = vbos.get(vbo);
        if (kept != null && kept.length == data.length)
            System.arraycopy(data, 0, kept, 0, data.length);
        else
            vbos.set(vbo, data.clone());
        markDirty(vbo, 0, data.length);
    }

    // A part of the vbo replaced from this offset in bytes (like glBufferSubData), what is past its end ignored
    void setSubVbo(int vbo, float[] data, int byteOffset) {
        ++version;
        float[] kept = vbos.get(vbo);
        if (kept == null)
            return;

        int start = byteOffset / Float.BYTES;
        int count = Math.max(0, Math.min(data.length, kept.length - start));
        System.arraycopy(data, 0, kept, start, count);
        if (count > 0)
            markDirty(vbo, start, start + count);
    }

    private void markDirty(int vbo, int start, int end) {
        if (dirtyStart[vbo] < 0) {
            dirtyStart[vbo] = start;
            dirtyEnd[vbo] = end;
        } else {
            dirtyStart[vbo] = Math.min(dirtyStart[vbo], start);
            dirtyEnd[vbo] = Math.max(dirtyEnd[vbo], end);
        }
        anyDirty = true;
    }

    // A vbo changed since its last upload
    boolean hasDirty() {
        return anyDirty;
    }

    // First float of the vbo changed since its last upload, -1 when it is up to date on the GPU
    int dirtyStart(int vbo) {
        return vbo < vbos.size() ? dirtyStart[vbo] : -1;
    }

    // End (exclusive) of the floats of the vbo changed since its last upload, meaningful when dirtyStart is not -1
    int dirtyEnd(int vbo) {
        return dirtyEnd[vbo];
    }

    // Every vbo sent to the GPU
    void clearDirty() {
        for (int i = 0; i < vbos.size(); ++i)
            dirtyStart[i] = -1;
        anyDirty = false;
    }

    // All the vbos removed (Mesh.unloadVBO): the next one added is the first again
    void clearVbos() {
        ++version;
        vbos.clear();
        anyDirty = false;
    }

    // The vbo, null when unknown; read only
    float[] vbo(int vbo) {
        return vbo < vbos.size() ? vbos.get(vbo) : null;
    }

    /**
     * The box of the positions (pairs x, y) of this vbo written at out[0..3] (minX, minY, maxX, maxY), computed
     * again only when a vbo changed: false when the vbo is unknown, empty or not finite.
     */
    boolean positionBox(int vbo, float[] out) {
        if (vbo != boxVbo || version != boxVersion) {
            float[] positions = vbo(vbo);
            hasBox = positions != null && BatchGeometry2D.Box(positions, positions.length / 2, box);
            boxVbo = vbo;
            boxVersion = version;
        }

        if (hasBox)
            System.arraycopy(box, 0, out, 0, 4);
        return hasBox;
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
