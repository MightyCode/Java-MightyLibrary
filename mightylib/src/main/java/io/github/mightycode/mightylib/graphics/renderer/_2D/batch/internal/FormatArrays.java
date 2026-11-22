package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.utils.Logger;

import java.util.IdentityHashMap;
import java.util.List;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL31C.glDrawElementsInstanced;

/**
 * The vertex arrays of an uploader, one per format (created at its first draw, in the context of the uploader): for
 * a format without instances, its vertex attributes on the vertex buffer of the uploader and its element buffer; for
 * a format with instances, its template (vertex and element buffers made once) and its instance attributes, pointed
 * at each draw at the instances of the uploader.
 */
final class FormatArrays {
    private static final class Arrays {
        final int vao;
        final int templateVertices;
        final int templateIndices;
        final int templateIndexCount;
        // Offset the vertex attributes point at (formats without instances), -1 before
        long pointedAt = -1;

        Arrays(int vao, int templateVertices, int templateIndices, int templateIndexCount) {
            this.vao = vao;
            this.templateVertices = templateVertices;
            this.templateIndices = templateIndices;
            this.templateIndexCount = templateIndexCount;
        }
    }

    private final GLContext owner = GLContext.current();
    // By identity: one set of vertex arrays per format object
    private final IdentityHashMap<BatchFormat, Arrays> arrays = new IdentityHashMap<>();
    // The buffers of the uploader the vertex arrays read (the vertices, the indices, the instances)
    private final int vertexBuffer;
    private final int elementBuffer;
    private final int instanceBuffer;
    private Arrays bound;

    FormatArrays(int vertexBuffer, int elementBuffer, int instanceBuffer) {
        this.vertexBuffer = vertexBuffer;
        this.elementBuffer = elementBuffer;
        this.instanceBuffer = instanceBuffer;
    }

    // The vertex arrays of these formats made now: a draw of them later creates no GL object
    void prepare(List<BatchFormat> formats) {
        for (BatchFormat format : formats)
            of(format);
        unbind();
    }

    private Arrays of(BatchFormat format) {
        Arrays found = arrays.get(format);
        if (found == null) {
            found = create(format);
            arrays.put(format, found);
        }

        return found;
    }

    private Arrays create(BatchFormat format) {
        int vao = GenDeleteResources.GenVertexArrays();
        glBindVertexArray(vao);
        Arrays created;
        if (!format.hasInstances()) {
            glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
            BatchLayouts.EnableAttributes(format, false);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
            created = new Arrays(vao, -1, -1, 0);
        } else {
            int templateVertices = -1;
            if (format.vertexBytes() > 0) {
                templateVertices = GenDeleteResources.GenBuffers();
                glBindBuffer(GL_ARRAY_BUFFER, templateVertices);
                glBufferData(GL_ARRAY_BUFFER, format.templateVertices(), GL_STATIC_DRAW);
                BatchLayouts.PointAttributes(format, false, 0);
                BatchLayouts.EnableAttributes(format, false);
            }
            int templateIndices = GenDeleteResources.GenBuffersEBO();
            int[] indices = format.templateIndices();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, templateIndices);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, instanceBuffer);
            BatchLayouts.EnableAttributes(format, true);
            created = new Arrays(vao, templateVertices, templateIndices, indices.length);
        }

        bound = created;
        Logger.CheckOpenGLError("Create the vertex array of the batch format " + format.name());
        return created;
    }

    private Arrays bind(BatchFormat format) {
        Arrays found = of(format);
        if (bound != found) {
            glBindVertexArray(found.vao);
            bound = found;
        }

        return found;
    }

    // Draws indexCount indices from byte indexOffset of the element buffer, the vertices of format from vertexOffset
    void draw(BatchFormat format, long vertexOffset, long indexOffset, int indexCount) {
        Arrays found = bind(format);
        if (found.pointedAt != vertexOffset) {
            glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
            BatchLayouts.PointAttributes(format, false, vertexOffset);
            found.pointedAt = vertexOffset;
        }

        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, indexOffset);
    }

    // Draws count instances of format from byte instanceOffset of the instance buffer (no base instance in 3.3)
    void drawInstances(BatchFormat format, long instanceOffset, int count) {
        Arrays found = bind(format);
        glBindBuffer(GL_ARRAY_BUFFER, instanceBuffer);
        BatchLayouts.PointAttributes(format, true, instanceOffset);
        glDrawElementsInstanced(GL_TRIANGLES, found.templateIndexCount, GL_UNSIGNED_INT, 0, count);
    }

    // Unbound: an element buffer bound later by other code can't land in these vertex arrays
    void unbind() {
        glBindVertexArray(0);
        bound = null;
    }

    // The vertex arrays in their context (later when another one is current), the template buffers now
    void delete(boolean contextAlive) {
        for (Arrays found : arrays.values()) {
            if (contextAlive)
                GenDeleteResources.DeleteVertexArrays(owner, found.vao);
            if (found.templateVertices != -1)
                GenDeleteResources.DeleteBuffers(found.templateVertices);
            if (found.templateIndices != -1)
                GenDeleteResources.DeleteBuffersEBO(found.templateIndices);
        }

        arrays.clear();
        bound = null;
    }
}
