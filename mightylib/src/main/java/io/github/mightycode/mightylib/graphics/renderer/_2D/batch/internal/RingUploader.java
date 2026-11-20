package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.utils.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL32C.*;

/**
 * Mode RING: one buffer for the vertices and the indices, written in turn like a ring without waiting for the GPU
 * (glMapBufferRange unsynchronized). Each flush writes a block (its vertices, then its indices) after the previous
 * one, wrapping to the start when the end is reached; a fence is put after the draws of each block, and a block is
 * written again only once the fences of what it covers are passed. A flush bigger than the ring makes it grow.
 */
final class RingUploader implements IBatchUploader {
    private static final long INITIAL_BYTES = 1024 * 1024;
    // Signaled fences are checked when this many blocks wait (keeps the queue short with many small flushes)
    private static final int CHECK_SIGNALED_FROM = 16;
    private static final long WAIT_NANOS = 1_000_000_000L;

    private final GLContext owner;
    private final int vao;
    private final int buffer;
    private long capacity = INITIAL_BYTES;
    private long head;

    // The block of the current flush
    private long vertexStart;
    private long indexStart;

    // Blocks still maybe read by the GPU, oldest first
    private final RingBlocks blocks = new RingBlocks();

    private boolean unmapFailureReported;

    RingUploader() {
        owner = GLContext.current();
        vao = GenDeleteResources.GenVertexArrays();
        buffer = GenDeleteResources.GenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, capacity, GL_STREAM_DRAW);
        OrphanUploader.SetVertexLayout(0);
        // The same buffer holds the indices
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer);
        glBindVertexArray(0);
        Logger.CheckOpenGLError("Create the ring buffer of the 2D batch");
    }

    @Override
    public void upload(ByteBuffer vertices, int vertexCount, IntBuffer indices, int indexCount) {
        long vertexBytes = (long) vertexCount * DrawBatch2D.VERTEX_BYTES;
        long indexBytes = (long) indexCount * Integer.BYTES;
        long size = vertexBytes + indexBytes;

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        // The vertices start on a whole vertex (the base vertex of the draws), the indices follow on 4 bytes
        long start = roundUp(head, DrawBatch2D.VERTEX_BYTES);
        if (size > capacity) {
            grow(size);
            start = 0;
        } else if (start + size > capacity) {
            start = 0;
        }

        if (blocks.count() >= CHECK_SIGNALED_FROM)
            dropSignaledBlocks();
        waitForBlocksOver(start, start + size);

        ByteBuffer mapped = glMapBufferRange(GL_ARRAY_BUFFER, start, size,
                GL_MAP_WRITE_BIT | GL_MAP_UNSYNCHRONIZED_BIT | GL_MAP_INVALIDATE_RANGE_BIT);
        if (mapped == null) {
            // Not expected: written the slow way
            Logger.CheckOpenGLError("Map the ring buffer of the 2D batch");
            glBufferSubData(GL_ARRAY_BUFFER, start, vertices);
            glBufferSubData(GL_ARRAY_BUFFER, start + vertexBytes, indices);
        } else {
            long address = MemoryUtil.memAddress(mapped);
            MemoryUtil.memCopy(MemoryUtil.memAddress(vertices), address, vertexBytes);
            MemoryUtil.memCopy(MemoryUtil.memAddress(indices), address + vertexBytes, indexBytes);
            if (!glUnmapBuffer(GL_ARRAY_BUFFER) && !unmapFailureReported) {
                // The content was lost (display mode change...): the next flushes write it again
                unmapFailureReported = true;
                System.err.println("--Ring buffer of the 2D batch: content lost while mapped, drawn once wrong");
            }
        }

        vertexStart = start;
        indexStart = start + vertexBytes;
        head = start + size;
        EngineStats.countBufferUpload(size);
        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Upload of a 2D batch in the ring");
    }

    private static long roundUp(long value, long multiple) {
        return (value + multiple - 1) / multiple * multiple;
    }

    // A new storage: what the GPU still reads keeps the previous one (orphaned), the fences are no longer needed
    private void grow(long size) {
        while (blocks.count() > 0)
            popBlock(false);

        capacity = Math.max(size, capacity * 2);
        glBufferData(GL_ARRAY_BUFFER, capacity, GL_STREAM_DRAW);
        head = 0;
    }

    // The blocks are done in order: waiting for the last one over the range waits for those before it
    private void waitForBlocksOver(long start, long end) {
        for (int over = blocks.blocksOver(start, end); over > 0; --over)
            popBlock(true);
    }

    private void dropSignaledBlocks() {
        while (blocks.count() > 0) {
            int status = glClientWaitSync(blocks.firstFence(), 0, 0);
            if (status != GL_ALREADY_SIGNALED && status != GL_CONDITION_SATISFIED)
                return;

            popBlock(false);
        }
    }

    private void popBlock(boolean wait) {
        long fence = blocks.firstFence();
        if (wait) {
            int status;
            do {
                status = glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, WAIT_NANOS);
            } while (status == GL_TIMEOUT_EXPIRED);

            if (status == GL_WAIT_FAILED)
                Logger.CheckOpenGLError("Wait for a block of the ring buffer of the 2D batch");
        }

        glDeleteSync(fence);
        blocks.removeFirst();
    }

    @Override
    public void draw(int firstIndex, int indexCount) {
        EngineStats.countDrawCall();
        glDrawElementsBaseVertex(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT,
                indexStart + (long) firstIndex * Integer.BYTES, (int) (vertexStart / DrawBatch2D.VERTEX_BYTES));
    }

    // The block is read by the draws just sent: a fence after them
    @Override
    public void flushEnded() {
        blocks.add(vertexStart, head, glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
        glBindVertexArray(0);
    }

    // Blocks waiting for the GPU (tests, command batch)
    int pendingBlocks() {
        return blocks.count();
    }

    long capacity() {
        return capacity;
    }

    void delete() {
        GenDeleteResources.DeleteVertexArrays(owner, vao);
        deleteBuffers();
    }

    // The fences and the buffer (shared objects), with a context current
    void deleteBuffers() {
        while (blocks.count() > 0)
            popBlock(false);
        GenDeleteResources.DeleteBuffers(buffer);
    }
}
