package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.utils.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL15C.*;

/**
 * Modes ORPHAN and INSTANCED: a vertex buffer, an index buffer and an instance buffer, orphaned (glBufferData without
 * data: the driver gives new storage when the previous one is still read by the GPU) then filled (glBufferSubData) at
 * each flush. Its vertex arrays (one per format) belong to the context of the uploader, the buffers are shared.
 */
final class OrphanUploader implements IBatchUploader {
    private static final int INITIAL_BYTES = 64 * 1024;

    private final int vbo;
    private final int ebo;
    private final int instanceBuffer;
    private final FormatArrays arrays;
    private long vertexCapacity = INITIAL_BYTES;
    private long indexCapacity = INITIAL_BYTES;
    private long instanceCapacity = INITIAL_BYTES;

    // Created in the current context, which owns the vertex arrays
    OrphanUploader() {
        vbo = GenDeleteResources.GenBuffers();
        ebo = GenDeleteResources.GenBuffersEBO();
        instanceBuffer = GenDeleteResources.GenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexCapacity, GL_STREAM_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, instanceBuffer);
        glBufferData(GL_ARRAY_BUFFER, instanceCapacity, GL_STREAM_DRAW);
        // The element buffer is a state of the bound vertex array: filled through GL_ARRAY_BUFFER
        glBindBuffer(GL_ARRAY_BUFFER, ebo);
        glBufferData(GL_ARRAY_BUFFER, indexCapacity, GL_STREAM_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        arrays = new FormatArrays(vbo, ebo, instanceBuffer);
        Logger.CheckOpenGLError("Create the buffers of the 2D batch");
    }

    @Override
    public void upload(ByteBuffer vertices, IntBuffer indices, ByteBuffer instances) {
        long vertexBytes = vertices.remaining();
        long indexBytes = (long) indices.remaining() * Integer.BYTES;
        long instanceBytes = instances.remaining();

        if (vertexBytes > 0) {
            vertexCapacity = fill(vbo, vertexBytes, vertexCapacity);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        }
        if (indexBytes > 0) {
            indexCapacity = fill(ebo, indexBytes, indexCapacity);
            glBufferSubData(GL_ARRAY_BUFFER, 0, indices);
        }
        if (instanceBytes > 0) {
            instanceCapacity = fill(instanceBuffer, instanceBytes, instanceCapacity);
            glBufferSubData(GL_ARRAY_BUFFER, 0, instances);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        EngineStats.countBufferUpload(vertexBytes + indexBytes + instanceBytes);
        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Upload of a 2D batch");
    }

    // Bound to GL_ARRAY_BUFFER (the binding of the elements belongs to the vertex arrays) and orphaned: its capacity
    private static long fill(int buffer, long bytes, long capacity) {
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        long size = bytes > capacity ? Math.max(bytes, capacity * 2) : capacity;
        glBufferData(GL_ARRAY_BUFFER, size, GL_STREAM_DRAW);
        return size;
    }

    @Override
    public void draw(BatchFormat format, int firstIndex, int indexCount) {
        EngineStats.countDrawCall();
        arrays.draw(format, 0, (long) firstIndex * Integer.BYTES, indexCount);
    }

    @Override
    public void drawInstances(BatchFormat format, int firstByte, int count) {
        EngineStats.countDrawCall();
        arrays.drawInstances(format, firstByte, count);
    }

    // The vertex arrays of the registered formats, made at the creation of the uploader
    void prepare(List<BatchFormat> formats) {
        arrays.prepare(formats);
    }

    @Override
    public void flushEnded() {
        arrays.unbind();
    }

    // The vertex arrays in their context (later when another one is current), the buffers now (a context is current)
    void delete() {
        arrays.delete(true);
        deleteBuffers();
    }

    // Its context is destroyed: the vertex arrays died with it
    void deleteBuffers() {
        arrays.delete(false);
        GenDeleteResources.DeleteBuffers(vbo);
        GenDeleteResources.DeleteBuffersEBO(ebo);
        GenDeleteResources.DeleteBuffers(instanceBuffer);
    }
}
