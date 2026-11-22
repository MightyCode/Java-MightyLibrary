package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Sends the vertices, the indices and the instances of a flush of a DrawBatch2D to the GPU, then draws its runs: one
 * way of filling the buffers per mode (orphaning, ring of regions). Main thread, OpenGL context of the batch current.
 */
public interface IBatchUploader {
    /**
     * The data of one flush, before its draws, each from position 0 to its limit (any may be empty): the vertices of
     * every format (the vertices of a format start on a multiple of its size: index i of a run of that format is the
     * vertex at i times its size), the indices of the triangles, the instances.
     */
    void upload(ByteBuffer vertices, IntBuffer indices, ByteBuffer instances);

    // One draw call: indexCount indices from firstIndex of the last upload, vertices read in format
    void draw(BatchFormat format, int firstIndex, int indexCount);

    // One instanced draw call: count instances of format from byte firstByte of the instances of the last upload
    void drawInstances(BatchFormat format, int firstByte, int count);

    // The flush is done: what the uploader needs to know before the next one (a fence of its region)
    default void flushEnded() {}
}
