package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Sends the vertices and the indices of a flush of a DrawBatch2D to the GPU, then draws its runs: one way of filling
 * the buffers per mode (orphaning, ring of regions). Main thread, OpenGL context of the batch current.
 */
public interface IBatchUploader {
    /**
     * The data of one flush, before its draws: vertexCount vertices of DrawBatch2D.VERTEX_BYTES bytes from the start
     * of vertices, and indexCount indices from the start of indices (indices of these vertices, from 0).
     */
    void upload(ByteBuffer vertices, int vertexCount, IntBuffer indices, int indexCount);

    // One draw call: indexCount indices from firstIndex of the last upload, with the state of the run applied
    void draw(int firstIndex, int indexCount);

    // The flush is done: what the uploader needs to know before the next one (a fence of its region)
    default void flushEnded() {}

    // Draws quads by instances (mode INSTANCED): the batch gives the instances of its runs of quads
    default boolean drawsInstances() {
        return false;
    }

    /** The instances of one flush (count of DrawBatch2D.INSTANCE_BYTES from the start), before their draws. */
    default void uploadInstances(ByteBuffer instances, int count) {
        throw new UnsupportedOperationException("No instances in this mode");
    }

    // One instanced draw call: count quads from firstInstance of the last upload, the instanced state applied
    default void drawInstances(int firstInstance, int count) {
        throw new UnsupportedOperationException("No instances in this mode");
    }
}
