package MightyLibrary.render.shape;

import MightyLibrary.main.ManagerContainer;
import MightyLibrary.render.shader.ShaderManager;
import MightyLibrary.render.shape.Shape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;

public abstract class Renderer{
    public Vector3f positionv;
    protected Matrix4f position;
    protected boolean display;
    protected FloatBuffer translatef;
    protected Shape shape;
    protected ShaderManager shadManager;

    public Renderer(String shaderName, boolean useEbo, boolean in2D) {
        shape = new Shape(shaderName, useEbo, in2D);
        shadManager = ManagerContainer.getInstance().shadManager;
        position = new Matrix4f();
        display = true;
    }

    public void display(){
        if(display) shape.display();
    }

    public void setPosition(Vector3f position){
        this.positionv = position;
        this.position.identity();
        this.position.translate(this.positionv);
        this.position.get(translatef);
    }

    public void setDisplayState(boolean state) {display = state;}
    public void invertDisplayState() {display = !display;}

    public void unload(){
        shape.unload();
    }
}
