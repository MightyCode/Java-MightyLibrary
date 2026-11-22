package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatchAttributeType;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_INT;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glVertexAttribIPointer;
import static org.lwjgl.opengl.GL33C.glVertexAttribDivisor;

/** The attributes of a BatchFormat in the bound vertex array. Main thread, a context current. */
final class BatchLayouts {
    private BatchLayouts() {}

    /**
     * Points the vertex attributes (instance false) or the instance attributes (instance true) of the format at the
     * buffer bound to GL_ARRAY_BUFFER, from offset.
     */
    static void PointAttributes(BatchFormat format, boolean instance, long offset) {
        int stride = instance ? format.instanceBytes() : format.vertexBytes();
        for (BatchFormat.Attribute attribute : format.attributes()) {
            if (attribute.perInstance() != instance)
                continue;

            long at = offset + attribute.offset();
            if (attribute.type() == EBatchAttributeType.FLOAT)
                glVertexAttribPointer(attribute.location(), attribute.components(), GL_FLOAT, false, stride, at);
            else if (attribute.type() == EBatchAttributeType.INT)
                glVertexAttribIPointer(attribute.location(), attribute.components(), GL_INT, stride, at);
            else
                glVertexAttribPointer(attribute.location(), attribute.components(), GL_UNSIGNED_BYTE, true, stride, at);
        }
    }

    // Enables the vertex attributes (instance false) or the instance attributes, those read per instance
    static void EnableAttributes(BatchFormat format, boolean instance) {
        for (BatchFormat.Attribute attribute : format.attributes()) {
            if (attribute.perInstance() != instance)
                continue;

            glEnableVertexAttribArray(attribute.location());
            if (instance)
                glVertexAttribDivisor(attribute.location(), 1);
        }
    }
}
