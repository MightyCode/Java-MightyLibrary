package MightyLibrary.render.shape;

import MightyLibrary.render.shader.ShaderManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class Light extends Shape{
    public Vector3f color;
    protected Vector3f intensity;
    public Vector3f position;
    protected Matrix4f translate;
    protected boolean display;
    private FloatBuffer translatef;


    public Light(ShaderManager shadManager, Vector3f position, float size){
        super(shadManager, "colorShape3D", false, false);

        float[] table = { -size, -size, -size,   size, -size, -size,   size,  size, -size,   size,  size, -size,   -size,  size, -size,   -size, -size, -size,

                        -size, -size,  size,   size, -size,  size,   size,  size,  size,   size,  size,  size,   -size,  size,  size,   -size, -size,  size,

                        -size,  size,  size,   -size,  size, -size,  -size, -size, -size,  -size, -size, -size,  -size, -size,  size,   -size,  size,  size,

                        size,  size,  size,    size,  size, -size,   size, -size, -size,   size, -size, -size,   size, -size,  size,    size,  size,  size,

                        -size, -size, -size,   size, -size, -size,   size, -size,  size,   size, -size,  size,   -size, -size,  size,   -size, -size, -size,

                        -size,  size, -size,   size,  size, -size,   size,  size,  size,   size,  size,  size,   -size,  size,  size,   -size,  size, -size,
                };
        setReading(new int[]{3});

        setVbo(table);

        translate = new Matrix4f();
        translatef = BufferUtils.createFloatBuffer(16);
        setPosition(position);
    }

    public void display(){
        if(display){
            shadManager.getShader(shaderId).glUniform("model", translatef);
            super.display();
        }
    }

    public void setColor(Vector3f color){
        this.color = color;
        updateColor();
    }

    public void updateColor(){
        shadManager.getShader(shaderId).glUniform("color", color.x, color.y, color.z);
    }

    public void setIntensity(Vector3f intensity){
        this.intensity = intensity;
    }

    public void setPosition(Vector3f position){
        this.position = position;
        translate.identity();
        translate.translate(this.position);
        translate.get(translatef);
    }

    public void enable(){display = true;}
    public void disable(){display = false;}
}
