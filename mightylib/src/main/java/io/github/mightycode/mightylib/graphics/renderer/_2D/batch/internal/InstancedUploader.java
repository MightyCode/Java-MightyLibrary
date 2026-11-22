package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormats;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.utils.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL31C.glDrawElementsInstanced;

/**
 * Mode INSTANCED: the runs of quads drawn by instances of a unit quad (its corners and its two triangles in buffers
 * made once, the instances orphaned then filled at each flush), the other runs by the uploader ORPHAN of the context.
 * OpenGL 3.3 has no base instance: the instance attributes are pointed at the first instance of each run.
 */
final class InstancedUploader implements IBatchUploader {
    private static final int INITIAL_INSTANCES = 1024;
    private static final BatchFormat FORMAT = BatchFormats.SPRITE_INSTANCED;
    private static final int TRIANGLE_COUNT = FORMAT.templateIndices().length;

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
        glBufferData(GL_ARRAY_BUFFER, FORMAT.templateVertices(), GL_STATIC_DRAW);
        BatchLayouts.PointAttributes(FORMAT, false, 0);
        BatchLayouts.EnableAttributes(FORMAT, false);

        glBindBuffer(GL_ARRAY_BUFFER, instanceBuffer);
        glBufferData(GL_ARRAY_BUFFER, instanceCapacity, GL_STREAM_DRAW);
        pointInstancesAt(0);
        BatchLayouts.EnableAttributes(FORMAT, true);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, triangleBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, FORMAT.templateIndices(), GL_STATIC_DRAW);
        glBindVertexArray(0);
        Logger.CheckOpenGLError("Create the instance buffers of the 2D batch");
    }

    // The instance attributes from this instance, the instance buffer bound to GL_ARRAY_BUFFER
    private static void pointInstancesAt(int firstInstance) {
        BatchLayouts.PointAttributes(FORMAT, true, (long) firstInstance * FORMAT.instanceBytes());
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
        glDrawElementsInstanced(GL_TRIANGLES, TRIANGLE_COUNT, GL_UNSIGNED_INT, 0, count);
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
