package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.graphics.renderer.utils.EDrawMode;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.math.MightyMath;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.GL_PATCHES;

public class Mesh {
    public static final int STATIC_STORE = GL_STATIC_DRAW;
    public static final int DYNAMIC_STORE = GL_DYNAMIC_DRAW;
    public static final int STREAM_STORE = GL_STREAM_DRAW;

    public static final int COMMON_VERTEX_POSITION_CHANNEL = 0;
    public static final int COMMON_TEXTURE_POSITION_CHANNEL = 1;
    public static final int COMMON_NORMAL_POSITION_CHANNEL = 2;

    protected int eboStorage;

    protected int ebo;

    // A vertex array of one OpenGL context, set up for the layout of the given version
    private static final class VaoEntry {
        final GLContext context;
        final int id;
        int version = -1;

        VaoEntry(GLContext context, int id) {
            this.context = context;
            this.id = id;
        }
    }

    // One vertex array per context that draws this mesh, created when it first does
    private final ArrayList<VaoEntry> vaoEntries = new ArrayList<>(1);
    private VaoEntry lastEntry;
    // Changes with each change of the buffers and attributes: a vertex array of another version is rebuilt
    private int layoutVersion;
    // Size and kind of each attribute, to set a new vertex array up
    protected ArrayList<Integer> vbosVertexSize;
    protected ArrayList<Boolean> vbosInteger;

    protected ArrayList<Integer> vbos;
    protected ArrayList<Integer> vbosStorage;
    // Number of elements currently allocated for each vbo, to update without reallocating when it doesn't change
    protected ArrayList<Integer> vbosSize;
    protected ArrayList<Boolean> vbosEnable;
    protected int vboCount;
    protected int indicesSize;

    protected boolean useEbo;
    protected int verticesToDraw;

    protected EDrawMode drawMode;
    private int glDrawMode;

    public Mesh(boolean useEbo) {
        setDrawMode(EDrawMode.Triangles);

        verticesToDraw = 0;
        indicesSize = 0;

        vbos = new ArrayList<>();
        vbosStorage = new ArrayList<>();
        vbosSize = new ArrayList<>();
        vbosEnable = new ArrayList<>();
        vbosVertexSize = new ArrayList<>();
        vbosInteger = new ArrayList<>();
        vboCount = 0;
        ebo = 0;
        eboStorage = STATIC_STORE;
        bind();
        setUseEbo(useEbo);
    }

    public int addVboFloat(float[] vertices, int vertexSize, int storage) {
        bind();
        int vbo = GenDeleteResources.GenBuffers();
        Logger.CheckOpenGLError("Gen vbo buffer : " + vbo);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        Logger.CheckOpenGLError("bind vbo buffer : " + vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();

        glBufferData(GL_ARRAY_BUFFER, buffer, storage);
        Logger.CheckOpenGLError("Buffer vbo buffer : " + storage + " of size : " + vertices.length);

        glVertexAttribPointer(vboCount, vertexSize, GL_FLOAT, false, 0, 0);
        Logger.CheckOpenGLError("Vbo attrib pointer : " + vbo);

        return endAddVbo(vbo, vertices.length, vertexSize, storage, false);
    }

    public int addVboInt(int[] vertices, int vertexSize, int storage){
        bind();
        int vbo = GenDeleteResources.GenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, storage);
        glVertexAttribIPointer(vboCount, vertexSize, GL_INT,0, 0);

        return endAddVbo(vbo, vertices.length, vertexSize, storage, true);
    }

    private int endAddVbo(int vbo, int arrayLength, int vertexSize, int storage, boolean integer){
        vbosEnable.add(false);
        enableVbo(vboCount);

        verticesToDraw = arrayLength / vertexSize;
        vbos.add(vbo);
        vbosStorage.add(storage);
        vbosSize.add(arrayLength);
        vbosVertexSize.add(vertexSize);
        vbosInteger.add(integer);
        ++vboCount;
        layoutChanged();

        return vbos.size() - 1;
    }


    public void updateVbo(float[] vertices, int vboPosition){
        EngineStats.countBufferUpload((long) vertices.length * Float.BYTES);
        bind();
        glBindBuffer(GL_ARRAY_BUFFER, vbos.get(vboPosition));

        // Same size: overwrite the existing storage instead of reallocating it
        if (vbosSize.get(vboPosition) == vertices.length) {
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        } else {
            glBufferData(GL_ARRAY_BUFFER, vertices, vbosStorage.get(vboPosition));
            vbosSize.set(vboPosition, vertices.length);
        }
    }

    public void updateVbo(int[] vertices, int vboPosition){
        EngineStats.countBufferUpload((long) vertices.length * Integer.BYTES);
        bind();
        glBindBuffer(GL_ARRAY_BUFFER, vbos.get(vboPosition));

        // Same size: overwrite the existing storage instead of reallocating it
        if (vbosSize.get(vboPosition) == vertices.length) {
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        } else {
            glBufferData(GL_ARRAY_BUFFER, vertices, vbosStorage.get(vboPosition));
            vbosSize.set(vboPosition, vertices.length);
        }
    }


    public void updateSubVbo(float[] vertices, int vboPosition, int startIndex){
        bind();
        glBindBuffer(GL_ARRAY_BUFFER, vbos.get(vboPosition));
        glBufferSubData(GL_ARRAY_BUFFER, startIndex, vertices);
    }

    public void updateSubVbo(int[] vertices, int vboPosition, int startIndex){
        bind();
        glBindBuffer(GL_ARRAY_BUFFER, vbos.get(vboPosition));
        glBufferSubData(GL_ARRAY_BUFFER, startIndex, vertices);
    }


    public void disableVbo(int pos){
        if(vbosEnable.size() <= pos)
            return;

        if (vbosEnable.get(pos)) {
            bind();
            glDisableVertexAttribArray(pos);
            vbosEnable.set(pos, false);
            layoutChanged();
        }
    }

    public void enableVbo(int pos){
        if(vbosEnable.size() <= pos) return;

        if (!vbosEnable.get(pos)) {
            bind();
            glEnableVertexAttribArray(pos);
            Logger.CheckOpenGLError("Vbo enable attrib : " + pos);
            vbosEnable.set(pos, true);
            layoutChanged();
        }
    }

    public boolean isEnableVbo(int pos) {
        if (vbosEnable.size() <= pos)
            return false;

        return vbosEnable.get(pos);
    }

    public void addAllVbo(float[] vertices, int[] vertexSizes, int ... storage){
        int oneLineSize = MightyMath.Sum(vertexSizes);
        int currentSum = 0;

        for (int i = 0; i < vertexSizes.length; ++i){
            float[] coordinatesInfo = new float[vertices.length / oneLineSize * vertexSizes[i]];
            for (int line = 0; line < vertices.length / oneLineSize; ++line){
                for (int inLine = 0; inLine < vertexSizes[i]; ++inLine){
                    coordinatesInfo[line * vertexSizes[i] + inLine]
                            = vertices[line * oneLineSize + currentSum + inLine];
                }
            }

            addVboFloat(coordinatesInfo, vertexSizes[i], storage[i]);
            currentSum += vertexSizes[i];
        }
    }


    public void setUseEbo(boolean state){
        if (this.useEbo && ebo != 0)
            GenDeleteResources.DeleteBuffersEBO(this.ebo);

        this.useEbo = state;

        if (this.useEbo)
            ebo = GenDeleteResources.GenBuffersEBO();

        layoutChanged();
    }


    public void setEboStorage(int eboStorage){
        this.eboStorage = eboStorage;
    }

    public void setEbo(int[] indices){
        setEbo(indices, indices.length);
    }

    public void setEbo(int[] indices, int indicesSize){
        bind();
        this.indicesSize = indicesSize;

        if (!useEbo)
            System.err.print(">(Mesh.java) Providing EBO without using EBO !");

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        Logger.CheckOpenGLError("Bind ebo : " + ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, eboStorage);
        Logger.CheckOpenGLError("Buffer ebo data");
    }

    public void setEboNumberIndex(int newIndexSize) {
        this.indicesSize = newIndexSize;
    }

    public void updateEbo(int[] newArray, int startIndex) {
        bind();
        if (!useEbo) {
            System.err.print(">(Mesh.java) Trying to update array without using EBO !");
            return;
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, startIndex, newArray);
    }

    /**
     * Bind the vertex array of the current OpenGL context. Vertex arrays are not shared between contexts: each
     * context has its own, created at the first bind and rebuilt from the layout (buffers, attributes, element
     * buffer) when it changed. The buffers themselves are shared.
     */
    public void bind(){
        GLContext context = GLContext.current();
        VaoEntry entry = lastEntry;
        if (entry == null || entry.context != context)
            entry = entryFor(context);

        lastEntry = entry;
        if (entry.version != layoutVersion)
            rebuild(entry);
        else
            glBindVertexArray(entry.id);

        // Called for each draw: avoid building the message when checks are disabled
        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Bind vao buffer : " + entry.id);
    }

    private VaoEntry entryFor(GLContext context) {
        // A destroyed context took its vertex arrays with it
        vaoEntries.removeIf(entry -> entry.context.isDestroyed());

        for (VaoEntry entry : vaoEntries) {
            if (entry.context == context)
                return entry;
        }

        // Generated in the current context, which is the one asked for
        VaoEntry entry = new VaoEntry(context, GenDeleteResources.GenVertexArrays());
        vaoEntries.add(entry);
        return entry;
    }

    // Bind the vertex array and set it up like the first one
    private void rebuild(VaoEntry entry) {
        glBindVertexArray(entry.id);

        for (int i = 0; i < vbos.size(); ++i) {
            glBindBuffer(GL_ARRAY_BUFFER, vbos.get(i));

            if (vbosInteger.get(i))
                glVertexAttribIPointer(i, vbosVertexSize.get(i), GL_INT, 0, 0);
            else
                glVertexAttribPointer(i, vbosVertexSize.get(i), GL_FLOAT, false, 0, 0);

            if (vbosEnable.get(i))
                glEnableVertexAttribArray(i);
            else
                glDisableVertexAttribArray(i);
        }

        if (useEbo && ebo != 0)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        entry.version = layoutVersion;
        Logger.CheckOpenGLError("Rebuild vao : " + entry.id);
    }

    // Called after the layout changed through the vertex array of the current context: the others are rebuilt at their next bind
    private void layoutChanged() {
        ++layoutVersion;

        GLContext context = GLContext.current();
        if (lastEntry != null && lastEntry.context == context)
            lastEntry.version = layoutVersion;
    }


    public void unbind(){
        glBindVertexArray(0);
    }


    public void draw(){
        EngineStats.countDrawCall();

        if (useEbo)
            glDrawElements(glDrawMode, indicesSize, GL_UNSIGNED_INT, 0);
        else
            glDrawArrays(glDrawMode, 0, verticesToDraw);
    }


    public void printStatus(){
        System.out.println("ebo" +  ebo + " vaos  " + vaoEntries.size() + " size " + indicesSize);
    }


    public void setDrawMode(EDrawMode drawMode) {
        this.drawMode = drawMode;

        switch (drawMode){
            case Triangles:
                glDrawMode = GL_TRIANGLES;
                break;
            case Lines:
                glDrawMode = GL_LINES;
                break;
            case LineStrip:
                glDrawMode = GL_LINE_STRIP;
                break;
            case LineLoop:
                glDrawMode = GL_LINE_LOOP;
                break;
            case LineAdjacency:
                glDrawMode = GL_LINES_ADJACENCY;
                break;
            case LineStripAdjacency:
                glDrawMode = GL_LINE_STRIP_ADJACENCY;
                break;
            case TriangleStrip:
                glDrawMode = GL_TRIANGLE_STRIP;
                break;
            case TriangleFan:
                glDrawMode = GL_TRIANGLE_FAN;
                break;
            case TrianglesAdjacency:
                glDrawMode = GL_TRIANGLES_ADJACENCY;
                break;
            case TriangleStripAdjacency:
                glDrawMode = GL_TRIANGLE_STRIP_ADJACENCY;
                break;
            case Patches:
                glDrawMode = GL_PATCHES;
                break;
        }
    }

    public EDrawMode getDrawMode(){
        return drawMode;
    }

    public void unloadVBO() {
        for (int vbo : vbos){
            GenDeleteResources.DeleteBuffers(vbo);
        }

        vbos.clear();
        vbosEnable.clear();
        vbosStorage.clear();
        vbosSize.clear();
        vbosVertexSize.clear();
        vbosInteger.clear();
        vboCount = 0;
        layoutChanged();
    }

    public void unload(){
        unloadVBO();

        // Each vertex array is deleted in its own context
        for (VaoEntry entry : vaoEntries)
            GenDeleteResources.DeleteVertexArrays(entry.context, entry.id);
        vaoEntries.clear();
        lastEntry = null;

        if (useEbo)
            GenDeleteResources.DeleteBuffersEBO(ebo);
    }
}
