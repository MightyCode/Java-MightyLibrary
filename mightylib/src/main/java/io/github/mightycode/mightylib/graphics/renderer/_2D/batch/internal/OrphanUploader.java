package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.utils.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;

/**
 * Mode ORPHAN: a vertex buffer and an index buffer, orphaned (glBufferData without data: the driver gives new
 * storage when the previous one is still read by the GPU) then filled (glBufferSubData) at each flush.
 * Its vertex array belongs to the context of the uploader, the buffers are shared.
 */
final class OrphanUploader implements IBatchUploader {
    private static final int INITIAL_BYTES = 64 * 1024;

    private final GLContext owner;
    private final int vao;
    private final int vbo;
    private final int ebo;
    private long vertexCapacity = INITIAL_BYTES;
    private long indexCapacity = INITIAL_BYTES;

    // Created in the current context, which owns the vertex array
    OrphanUploader() {
        owner = GLContext.current();
        vao = GenDeleteResources.GenVertexArrays();
        vbo = GenDeleteResources.GenBuffers();
        ebo = GenDeleteResources.GenBuffersEBO();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexCapacity, GL_STREAM_DRAW);
        SetVertexLayout(0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexCapacity, GL_STREAM_DRAW);
        glBindVertexArray(0);
        Logger.CheckOpenGLError("Create the buffers of the 2D batch");
    }

    // The fixed format of DrawBatch2D on the buffer bound to GL_ARRAY_BUFFER, from offset, in the bound vertex array
    static void SetVertexLayout(long offset) {
        int stride = DrawBatch2D.VERTEX_BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, offset);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, offset + 8);
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, stride, offset + 16);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
    }

    @Override
    public void upload(ByteBuffer vertices, int vertexCount, IntBuffer indices, int indexCount) {
        long vertexBytes = (long) vertexCount * DrawBatch2D.VERTEX_BYTES;
        long indexBytes = (long) indexCount * Integer.BYTES;

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        if (vertexBytes > vertexCapacity)
            vertexCapacity = Math.max(vertexBytes, vertexCapacity * 2);
        glBufferData(GL_ARRAY_BUFFER, vertexCapacity, GL_STREAM_DRAW);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        // Bound again: the element buffer is a state of the vertex array
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        if (indexBytes > indexCapacity)
            indexCapacity = Math.max(indexBytes, indexCapacity * 2);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexCapacity, GL_STREAM_DRAW);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indices);

        EngineStats.countBufferUpload(vertexBytes + indexBytes);
        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Upload of a 2D batch");
    }

    // Its vertex array bound again (after the instanced draws of the same flush)
    void bind() {
        glBindVertexArray(vao);
    }

    @Override
    public void draw(int firstIndex, int indexCount) {
        EngineStats.countDrawCall();
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, (long) firstIndex * Integer.BYTES);
    }

    // Unbound: an element buffer bound later by other code can't land in this vertex array
    @Override
    public void flushEnded() {
        glBindVertexArray(0);
    }

    // The vertex array in its context (later when another one is current), the buffers now (a context is current)
    void delete() {
        GenDeleteResources.DeleteVertexArrays(owner, vao);
        deleteBuffers();
    }

    // Its context is destroyed: the vertex array died with it
    void deleteBuffers() {
        GenDeleteResources.DeleteBuffers(vbo);
        GenDeleteResources.DeleteBuffersEBO(ebo);
    }
}
