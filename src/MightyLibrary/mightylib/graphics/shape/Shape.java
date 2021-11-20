package MightyLibrary.mightylib.graphics.shape;

import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.util.Id;
import MightyLibrary.mightylib.util.math.MightyMath;
import org.lwjgl.opengl.GL15;

import java.util.ArrayList;

import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Shape{
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
    protected Id shaderId;

    public Shape(String shaderName, boolean useEbo){
        this(shaderName, useEbo, true);
    }

    public Shape(String shaderName, boolean useEbo, boolean in2D) {
        setDimensionTo2D(in2D);
        this.shadManager = ShaderManager.getInstance();
        this.shaderId = this.shadManager.getIdShaderFromString(shaderName);

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

    public int addVbo(float[] vertices, int vertexSize, int storage){
        bind();
        int vbo = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, storage);
        glVertexAttribPointer(vboCount, vertexSize, GL_FLOAT, false, 0, 0);

        vbosEnable.add(false);
        enableVbo(vboCount);

        verticesDraw = vertices.length / vertexSize;
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
        int oneLineSize = MightyMath.sum(vertexSizes);
        int currentSum = 0;

        for (int i = 0; i < vertexSizes.length; ++i){
            float[] coordinatesInfo = new float[vertices.length / oneLineSize * vertexSizes[i]];
            for (int line = 0; line < vertices.length / oneLineSize; ++line){
                for (int inLine = 0; inLine < vertexSizes[i]; ++inLine){
                    coordinatesInfo[line * vertexSizes[i] + inLine] = vertices[line * oneLineSize + currentSum + inLine];
                }
            }

            addVbo(coordinatesInfo, vertexSizes[i], storage[i]);
            currentSum += vertexSizes[i];
        }
    }


    public Shape setUseEbo(boolean state){
        if (this.useEbo && ebo != 0) glDeleteBuffers(this.ebo);

        this.useEbo = state;

        if (this.useEbo) ebo =  glGenBuffers();
        return this;
    }


    public Shape setEboStorage(int eboStorage){
        this.eboStorage = eboStorage;
        return this;
    }


    public Shape setEbo(int[] indices){
        bind();
        this.indicesSize = indices.length;
        if (!useEbo) System.err.print(">(Shape.java) Providing EBO without using EBO !");

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, eboStorage);
        return this;
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


    public Shape unbind(){
        glBindVertexArray(0);
        return this;
    }


    public void applyDimension() {
        if (in2D)   glDisable(GL_DEPTH_TEST);
        else        glEnable(GL_DEPTH_TEST);
    }


    public Shape setDimensionTo2D(boolean state){
        this.in2D = state;
        return this;
    }


    public boolean getIn2D(){
        return this.in2D;
    }


    public void draw(){
        if (useEbo) glDrawElements(GL_TRIANGLES, indicesSize, GL_UNSIGNED_INT, 0);
        else glDrawArrays(GL_TRIANGLES, 0, verticesDraw);
    }


    public void printStatus(){
        System.out.println("ebo" +  ebo + " vao  " + vao + " size " + indicesSize);
    }


    public void applyShader(){
        shadManager.getShader(shaderId).use();
    }


    public Id getShaderId(){
        return shaderId;
    }


    public void unload(){
        for (int vbo : vbos){
            glDeleteBuffers(vbo);
        }
        glDeleteVertexArrays(vao);
        if(useEbo)glDeleteBuffers(ebo);
    }
}
