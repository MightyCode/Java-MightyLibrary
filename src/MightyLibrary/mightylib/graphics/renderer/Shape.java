package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.graphics.renderer.utils.EDrawMode;
import MightyLibrary.mightylib.graphics.shader.Shader;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.utils.math.ID;
import MightyLibrary.mightylib.utils.math.MightyMath;

import java.util.ArrayList;

import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.GL_PATCHES;

public class Shape {
    public static final int STATIC_STORE = GL_STATIC_DRAW;
    public static final int DYNAMIC_STORE = GL_DYNAMIC_DRAW;
    public static final int STREAM_STORE = GL_STREAM_DRAW;

    protected int eboStorage;

    protected int vao, ebo;
    protected ArrayList<Integer> vbos;
    protected ArrayList<Integer> vbosStorage;
    protected ArrayList<Boolean> vbosEnable;
    protected int vboCount;
    protected int indicesSize;

    protected int[] info;

    protected boolean useEbo;
    protected int verticesToDraw;

    protected boolean in2D;

    protected EDrawMode drawMode;
    private int glDrawMode;

    protected Shader shader;
    public Shader getShader() { return shader; }

    public Shape(String shaderName, boolean useEbo) {
        setDrawMode(EDrawMode.Triangles);

        shader = Resources.getInstance().getResource(Shader.class, shaderName);
        setDimensionTo2D(this.shader.isDimension2DShader());

        verticesToDraw = 0;
        info = new int[0];
        indicesSize = 0;

        vao = glGenVertexArrays();
        bind();
        vbos = new ArrayList<>();
        vbosStorage = new ArrayList<>();
        vbosEnable = new ArrayList<>();
        vboCount = 0;
        ebo = 0;
        eboStorage = STATIC_STORE;
        setUseEbo(useEbo);
    }

    public int addVboFloat(float[] vertices, int vertexSize, int storage){
        bind();
        int vbo = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, storage);
        glVertexAttribPointer(vboCount, vertexSize, GL_FLOAT, false, 0, 0);

        return endAddVbo(vbo, vertices.length, vertexSize, storage);
    }

    public int addVboInt(int[] vertices, int vertexSize, int storage){
        bind();
        int vbo = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, storage);
        glVertexAttribPointer(vboCount, vertexSize, GL_INT, false, 0, 0);

        return endAddVbo(vbo, vertices.length, vertexSize, storage);
    }

    private int endAddVbo(int vbo, int arrayLength, int vertexSize, int storage){
        vbosEnable.add(false);
        enableVbo(vboCount);

        verticesToDraw = arrayLength / vertexSize;
        vbos.add(vbo);
        vbosStorage.add(storage);
        ++vboCount;

        return vbos.size() - 1;
    }


    public void updateVbo(float[] vertices, int vboPosition){
        bind();
        glBindBuffer(GL_ARRAY_BUFFER, vbos.get(vboPosition));
        glBufferData(GL_ARRAY_BUFFER, vertices, vbosStorage.get(vboPosition));
    }

    public void updateVbo(int[] vertices, int vboPosition){
        bind();
        glBindBuffer(GL_ARRAY_BUFFER, vbos.get(vboPosition));
        glBufferData(GL_ARRAY_BUFFER, vertices, vbosStorage.get(vboPosition));
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
        }
    }

    public void enableVbo(int pos){
        if(vbosEnable.size() <= pos) return;

        if (!vbosEnable.get(pos)) {
            bind();
            glEnableVertexAttribArray(vboCount);
            vbosEnable.set(pos, true);
        }
    }


    public void addAllVbo(float[] vertices, int[] vertexSizes, int ... storage){
        int oneLineSize = MightyMath.Sum(vertexSizes);
        int currentSum = 0;

        for (int i = 0; i < vertexSizes.length; ++i){
            float[] coordinatesInfo = new float[vertices.length / oneLineSize * vertexSizes[i]];
            for (int line = 0; line < vertices.length / oneLineSize; ++line){
                for (int inLine = 0; inLine < vertexSizes[i]; ++inLine){
                    coordinatesInfo[line * vertexSizes[i] + inLine] = vertices[line * oneLineSize + currentSum + inLine];
                }
            }

            addVboFloat(coordinatesInfo, vertexSizes[i], storage[i]);
            currentSum += vertexSizes[i];
        }
    }


    public void setUseEbo(boolean state){
        if (this.useEbo && ebo != 0)
            glDeleteBuffers(this.ebo);

        this.useEbo = state;

        if (this.useEbo)
            ebo = glGenBuffers();
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
            System.err.print(">(Shape.java) Providing EBO without using EBO !");

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, eboStorage);
    }

    public void setEboNumberIndex(int newIndexSize) {
        this.indicesSize = newIndexSize;
    }

    public void updateEbo(int[] newArray, int startIndex) {
        bind();
        if (!useEbo) {
            System.err.print(">(Shape.java) Trying to update array without using EBO !");
            return;
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, startIndex, newArray);
    }

    public void display() {
        applyDimension();
        applyShader();
        bind();
        draw();
    }


    public void bind(){
        glBindVertexArray(vao);
    }


    public void unbind(){
        glBindVertexArray(0);
    }


    public void applyDimension() {
        if (in2D)   glDisable(GL_DEPTH_TEST);
        else        glEnable(GL_DEPTH_TEST);
    }


    public void setDimensionTo2D(boolean state){
        this.in2D = state;
    }


    public boolean getIn2D(){
        return this.in2D;
    }

    public void draw(){
        if (useEbo)
            glDrawElements(glDrawMode, indicesSize, GL_UNSIGNED_INT, 0);
        else
            glDrawArrays(glDrawMode, 0, verticesToDraw);
    }


    public void printStatus(){
        System.out.println("ebo" +  ebo + " vao  " + vao + " size " + indicesSize);
    }


    public void applyShader(){
        shader.use();
    }


    public ID getShaderId(){
        return new ID(shader.getShaderId());
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

    public void unload(){
        for (int vbo : vbos){
            glDeleteBuffers(vbo);
        }

        glDeleteVertexArrays(vao);

        if(useEbo)
            glDeleteBuffers(ebo);
    }
}
