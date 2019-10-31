package MightyLibrary.render.shape;

import MightyLibrary.render.shader.ShaderManager;
import MightyLibrary.util.Id;
import MightyLibrary.main.ManagerContainer;
import MightyLibrary.util.math.Math;
import org.lwjgl.opengl.GL15;

import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Shape{
    public static final int STATIC_STORE = GL15.GL_STATIC_DRAW;
    public static final int DYNAMIC_STORE = GL_DYNAMIC_DRAW;
    public static final int STREAM_STORE = GL_STREAM_DRAW;

    protected int vboStorage, eboStorage;

    protected int vao, vbo, ebo;
    protected int indicesSize;

    protected int[] info;

    protected boolean useEbo;
    protected int verticesSize;
    protected int verticesDraw;

    protected boolean in2D;

    protected ShaderManager shadManager;
    protected Id shaderId;

    public Shape(String shaderName, boolean useEbo){
        this(shaderName, useEbo, true);
    }

    public Shape(String shaderName, boolean useEbo, boolean in2D){
        setDimensionTo2D(in2D).setStorage(STATIC_STORE, STATIC_STORE);
        this.shadManager = ManagerContainer.getInstance().shadManager;
        this.shaderId = this.shadManager.getIdShaderFromString(shaderName);

        verticesDraw = 0;
        verticesSize = 0;
        info = new int[0];
        indicesSize = 0;

        vao = glGenVertexArrays();
        bind();
        vbo = glGenBuffers();
        ebo = 0;
        setUseEbo(useEbo);
    }

    public Shape setStorage(int vboStorage, int eboStorage){
        this.vboStorage = vboStorage;
        this.eboStorage = eboStorage;
        return this;
    }

    public Shape setUseEbo(boolean state){
        if (this.useEbo && ebo != 0) glDeleteBuffers(this.ebo);

        this.useEbo = state;

        if (this.useEbo) ebo =  glGenBuffers();
        return this;
    }

    public Shape setReading(int[] info){
        this.bind();
        this.info = info;

        int total = 0;
        int sum = Math.sum(info);

        if(verticesSize == 0)     this.setVbo(new float[sum]);
        else                         verticesDraw = verticesSize / sum;

        for(int i = 0; i < info.length; i++) {
            glVertexAttribPointer(i, info[i], GL_FLOAT, false,4 * sum, total);
            total += info[i] * 4;
            glEnableVertexAttribArray(i);
        }
        return this;
    }

    public Shape setVbo(float[] vertices){
        bind();
        this.verticesSize = vertices.length;

        if (info.length == 0)   verticesDraw = 0;
        else                    verticesDraw = vertices.length / Math.sum(info);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        return this;
    }

    public Shape setEbo(int[] indices){
        bind();
        this.indicesSize = indices.length;
        if (!useEbo) System.err.print(">(Shape.java) Providing EBO without using EBO !");

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
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

    public void draw(){

        if (useEbo) glDrawElements(GL_TRIANGLES, indicesSize, GL_UNSIGNED_INT, 0);
        else glDrawArrays(GL_TRIANGLES, 0, verticesDraw);
    }

    public void printStatus(){
        System.out.println("ebo" +  ebo + " vao  " + vao + " vbo " + vbo + " size " + indicesSize);
    }

    public void applyShader(){
        shadManager.getShader(shaderId).use();
    }

    public Id getShaderId(){
        return shaderId;
    }

    public void unload(){
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        if(useEbo)glDeleteBuffers(ebo);
    }
}
