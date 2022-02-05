package MightyLibrary.mightylib.graphics.shape._3D;

import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import org.joml.Vector3f;

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
