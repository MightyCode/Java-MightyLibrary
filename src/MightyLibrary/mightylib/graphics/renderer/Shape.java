package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.graphics.shader.Shader;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.util.Id;
import MightyLibrary.mightylib.util.math.MightyMath;

import java.util.ArrayList;

import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.GL30.*;

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
    protected int verticesDraw;

    protected boolean in2D;

    protected ShaderManager shadManager;
    protected Shader shader;
    public Shader getShader() { return shader; }

    public Shape(String shaderName, boolean useEbo) {
        this.shadManager = ShaderManager.getInstance();
        this.shader = shadManager.getShader(shaderName);
        setDimensionTo2D(this.shader.isDimension2DShader());

        verticesDraw = 0;
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

        verticesDraw = arrayLength / vertexSize;
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



    public void disableVbo(int pos){
        if(vbosEnable.size() <= pos) return;

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
        if (this.useEbo && ebo != 0) glDeleteBuffers(this.ebo);

        this.useEbo = state;

        if (this.useEbo) ebo =  glGenBuffers();
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
        if (!useEbo) System.err.print(">(Shape.java) Providing EBO without using EBO !");

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


    public void display(){
        applyDimension();
        applyShader();
        bind();
        draw();
        unbind();
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
            glDrawElements(GL_TRIANGLES, indicesSize, GL_UNSIGNED_INT, 0);
        else
            glDrawArrays(GL_TRIANGLES, 0, verticesDraw);
    }


    public void printStatus(){
        System.out.println("ebo" +  ebo + " vao  " + vao + " size " + indicesSize);
    }


    public void applyShader(){
        shader.use();
    }


    public Id getShaderId(){
        return new Id(shader.getShaderId());
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
