package MightyLibrary.render.shape._3D;

import MightyLibrary.render.shape.Renderer;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class CubeRenderer extends Renderer {

    public CubeRenderer(String shaderName, Vector3f position, float size){
        super(shaderName, false, false);

        float[] table = { 0.0f, 0.0f, 0.0f,   size, 0.0f, 0.0f,   size,  size, 0.0f,   size,  size, 0.0f,   0.0f,  size, 0.0f,   0.0f, 0.0f, 0.0f,

                        0.0f, 0.0f,  size,   size, 0.0f,  size,   size,  size,  size,   size,  size,  size,   0.0f,  size,  size,   0.0f, 0.0f,  size,

                        0.0f,  size,  size,   0.0f,  size, 0.0f,  0.0f, 0.0f, 0.0f,  0.0f, 0.0f, 0.0f,  0.0f, 0.0f,  size,   0.0f,  size,  size,

                        size,  size,  size,    size,  size, 0.0f,   size, 0.0f, 0.0f,   size, 0.0f, 0.0f,   size, 0.0f,  size,    size,  size,  size,

                        0.0f, 0.0f, 0.0f,   size, 0.0f, 0.0f,   size, 0.0f,  size,   size, 0.0f,  size,   0.0f, 0.0f,  size,   0.0f, 0.0f, 0.0f,

                        0.0f,  size, 0.0f,   size,  size, 0.0f,   size,  size,  size,   size,  size,  size,   0.0f,  size,  size,   0.0f,  size, 0.0f,
                };
        shape.setReading(new int[]{3});
        shape.setVbo(table);
        setPosition(position);
    }

    public void display(){
        super.display();
    }
}
