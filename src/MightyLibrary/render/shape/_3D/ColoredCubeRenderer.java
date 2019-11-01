package MightyLibrary.render.shape._3D;

import MightyLibrary.render.shape.Renderer;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class ColoredCubeRenderer extends Renderer {
    public Vector3f color;
    protected Vector3f intensity;

    public ColoredCubeRenderer(Vector3f position, float size){
        super("colorShape3D", false, false);

        float[] table = { -size, -size, -size,   size, -size, -size,   size,  size, -size,   size,  size, -size,   -size,  size, -size,   -size, -size, -size,

                        -size, -size,  size,   size, -size,  size,   size,  size,  size,   size,  size,  size,   -size,  size,  size,   -size, -size,  size,

                        -size,  size,  size,   -size,  size, -size,  -size, -size, -size,  -size, -size, -size,  -size, -size,  size,   -size,  size,  size,

                        size,  size,  size,    size,  size, -size,   size, -size, -size,   size, -size, -size,   size, -size,  size,    size,  size,  size,

                        -size, -size, -size,   size, -size, -size,   size, -size,  size,   size, -size,  size,   -size, -size,  size,   -size, -size, -size,

                        -size,  size, -size,   size,  size, -size,   size,  size,  size,   size,  size,  size,   -size,  size,  size,   -size,  size, -size,
                };
        shape.setReading(new int[]{3});

        shape.setVbo(table);

        translatef = BufferUtils.createFloatBuffer(16);
        setPosition(position);
    }

    public void display(){
        shadManager.getShader(shape.getShaderId()).glUniform("model", translatef);
        super.display();
    }

    public ColoredCubeRenderer setColor(Vector3f color){
        this.color = color;
        updateColor();
        return this;
    }

    public ColoredCubeRenderer updateColor(){
        shadManager.getShader(shape.getShaderId()).glUniform("color", color.x, color.y, color.z);
        return this;
    }
}
