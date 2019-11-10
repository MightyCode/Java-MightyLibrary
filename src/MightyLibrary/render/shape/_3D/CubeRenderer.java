package MightyLibrary.render.shape._3D;

import MightyLibrary.render.shape.Renderer;
import MightyLibrary.render.shape.Shape;
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

        shape.addVbo(table, 3, Shape.STATIC_STORE);
        setPosition(position);
    }
}
