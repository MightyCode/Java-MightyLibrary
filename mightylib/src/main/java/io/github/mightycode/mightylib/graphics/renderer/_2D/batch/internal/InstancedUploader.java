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
import static org.lwjgl.opengl.GL31C.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33C.glVertexAttribDivisor;

/**
 * Mode INSTANCED: the runs of quads drawn by instances of a unit quad (its corners and its two triangles in buffers
 * made once, the instances orphaned then filled at each flush), the other runs by the uploader ORPHAN of the context.
 * OpenGL 3.3 has no base instance: the instance attributes are pointed at the first instance of each run.
 */
final class InstancedUploader implements IBatchUploader {
    private static final int INITIAL_INSTANCES = 1024;
    // The corners of the unit quad in the order of the corners of a quad of the batch, and its triangles
    private static final float[] CORNERS = {0, 1, 0, 0, 1, 0, 1, 1};
    private static final int[] TRIANGLES = {0, 3, 2, 0, 2, 1};

    private final OrphanUploader vertices;
    private final GLContext owner;
    private final int vao;
    private final int cornerBuffer;
    private final int triangleBuffer;
    private final int instanceBuffer;
    private long instanceCapacity = (long) INITIAL_INSTANCES * DrawBatch2D.INSTANCE_BYTES;

    InstancedUploader(OrphanUploader vertices) {
        this.vertices = vertices;
        owner = GLContext.current();
        vao = GenDeleteResources.GenVertexArrays();
        cornerBuffer = GenDeleteResources.GenBuffers();
        triangleBuffer = GenDeleteResources.GenBuffersEBO();
        instanceBuffer = GenDeleteResources.GenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, cornerBuffer);
        glBufferData(GL_ARRAY_BUFFER, CORNERS, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, instanceBuffer);
        glBufferData(GL_ARRAY_BUFFER, instanceCapacity, GL_STREAM_DRAW);
        pointInstancesAt(0);
        for (int location = 1; location <= 5; ++location) {
            glEnableVertexAttribArray(location);
            glVertexAttribDivisor(location, 1);
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, triangleBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, TRIANGLES, GL_STATIC_DRAW);
        glBindVertexArray(0);
        Logger.CheckOpenGLError("Create the instance buffers of the 2D batch");
    }

    // The instance attributes from this instance, the instance buffer bound to GL_ARRAY_BUFFER
    private static void pointInstancesAt(int firstInstance) {
        int stride = DrawBatch2D.INSTANCE_BYTES;
        long offset = (long) firstInstance * stride;
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, offset);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, offset + 12);
        glVertexAttribPointer(3, 2, GL_FLOAT, false, stride, offset + 24);
        glVertexAttribPointer(4, 4, GL_FLOAT, false, stride, offset + 32);
        glVertexAttribPointer(5, 4, GL_UNSIGNED_BYTE, true, stride, offset + 48);
    }

    @Override
    public void upload(ByteBuffer vertexData, int vertexCount, IntBuffer indices, int indexCount) {
        vertices.upload(vertexData, vertexCount, indices, indexCount);
    }

    @Override
    public void draw(int firstIndex, int indexCount) {
        vertices.bind();
        vertices.draw(firstIndex, indexCount);
    }

    @Override
    public boolean drawsInstances() {
        return true;
    }

    @Override
    public void uploadInstances(ByteBuffer instances, int count) {
        long bytes = (long) count * DrawBatch2D.INSTANCE_BYTES;
        glBindBuffer(GL_ARRAY_BUFFER, instanceBuffer);
        if (bytes > instanceCapacity)
            instanceCapacity = Math.max(bytes, instanceCapacity * 2);
        glBufferData(GL_ARRAY_BUFFER, instanceCapacity, GL_STREAM_DRAW);
        glBufferSubData(GL_ARRAY_BUFFER, 0, instances);

        EngineStats.countBufferUpload(bytes);
        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Upload of the instances of a 2D batch");
    }

    @Override
    public void drawInstances(int firstInstance, int count) {
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, instanceBuffer);
        pointInstancesAt(firstInstance);

        EngineStats.countDrawCall();
        glDrawElementsInstanced(GL_TRIANGLES, TRIANGLES.length, GL_UNSIGNED_INT, 0, count);
    }

    @Override
    public void flushEnded() {
        glBindVertexArray(0);
    }

    void delete() {
        GenDeleteResources.DeleteVertexArrays(owner, vao);
        deleteBuffers();
    }

    // Its context is destroyed: the vertex array died with it
    void deleteBuffers() {
        GenDeleteResources.DeleteBuffers(cornerBuffer);
        GenDeleteResources.DeleteBuffersEBO(triangleBuffer);
        GenDeleteResources.DeleteBuffers(instanceBuffer);
    }
}
