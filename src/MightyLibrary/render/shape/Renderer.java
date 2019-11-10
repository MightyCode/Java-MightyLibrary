package MightyLibrary.render.shape;

import MightyLibrary.main.ManagerContainer;
import MightyLibrary.render.shader.ShaderManager;
import MightyLibrary.render.texture.TextureManager;
import MightyLibrary.util.Id;
import MightyLibrary.util.math.Color4f;
import MightyLibrary.util.math.ColorList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public abstract class Renderer{
    public Vector3f modelV;
    protected Matrix4f model;
    protected boolean display;
    protected FloatBuffer translateF;
    protected Shape shape;
    protected ShaderManager shadManager;
    protected TextureManager textureManager;


    protected boolean coloredMode;

    // Textured
    protected Id textureId;

    // Colored
    public Color4f color;

    public Renderer(String shaderName, boolean useEbo, boolean in2D){
        shadManager = ManagerContainer.getInstance().shadManager;
        textureManager = ManagerContainer.getInstance().texManager;

        shape = new Shape(shaderName, useEbo, in2D);
        model = new Matrix4f().identity();
        translateF = BufferUtils.createFloatBuffer(16);
        display = true;

        // Display mode
        textureId = new Id(0);
        coloredMode = true;
        color = ColorList.BLACK;
    }


    public void display(){
        if(display){
            updateShader();
            draw();
        }
    }



    public void updateShader(){
        // Apply model matrix
        shadManager.getShader(shape.getShaderId()).glUniform("model", translateF);
        if (coloredMode){
            shadManager.getShader(shape.getShaderId()).glUniform("color", color.getR(), color.getG(), color.getB(), color.getA());
        } else {
            textureManager.bind(textureId, 0);
        }
    }


    public void draw(){
        shape.display();
    }


    public void setPosition(Vector3f position){
        this.modelV = position;
        this.model.identity();
        this.model.translate(this.modelV);
        this.model.get(translateF);
    }

    public void setDisplayState(boolean state) {display = state;}
    public void invertDisplayState() {display = !display;}

    public void setTexture(String texture){
        coloredMode = false;
        textureId = textureManager.getIdShaderFromString(texture);
    }

    public void setColor(Color4f color){
        coloredMode = true;
        this.color = color.copy();
    }

    public void unload(){
        shape.unload();
        translateF.clear();
    }
}
