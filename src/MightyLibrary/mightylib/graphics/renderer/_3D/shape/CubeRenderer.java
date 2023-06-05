package MightyLibrary.mightylib.graphics.renderer._3D.shape;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import org.joml.Vector3f;

public class CubeRenderer extends Renderer {

    private boolean includeTextures;
    private boolean includeNormal;

    private final int positionIndex;
    private int textureIndex;
    private int normalIndex;

    public CubeRenderer(String shaderName, Vector3f position, float size){
        super(shaderName, false, false);

        float[] table = { 0.0f, 0.0f, 0.0f,
                        size, 0.0f, 0.0f,
                        size,  size, 0.0f,

                        size,  size, 0.0f,
                        0.0f,  size, 0.0f,
                        0.0f, 0.0f, 0.0f,

                        0.0f, 0.0f,  size,
                        size, 0.0f,  size,
                        size,  size,  size,

                        size,  size,  size,
                        0.0f,  size,  size,
                        0.0f, 0.0f,  size,

                        0.0f,  size,  size,
                        0.0f,  size, 0.0f,
                        0.0f, 0.0f, 0.0f,

                        0.0f, 0.0f, 0.0f,
                        0.0f, 0.0f,  size,
                        0.0f,  size,  size,

                        size,  size,  size,
                        size,  size, 0.0f,
                        size, 0.0f, 0.0f,

                        size, 0.0f, 0.0f,
                        size, 0.0f,  size,
                        size,  size,  size,

                        0.0f, 0.0f, 0.0f,
                        size, 0.0f, 0.0f,
                        size, 0.0f,  size,

                        size, 0.0f,  size,
                        0.0f, 0.0f,  size,
                        0.0f, 0.0f, 0.0f,

                        0.0f,  size, 0.0f,
                        size,  size, 0.0f,
                        size,  size,  size,

                        size,  size,  size,
                        0.0f,  size,  size,
                        0.0f,  size, 0.0f,
        };

        positionIndex = shape.addVboFloat(table, 3, Shape.STATIC_STORE);
        /*textureIndex = shape.addVbo(new float[0], 2, Shape.STATIC_STORE);
        normalIndex = shape.addVbo(new float[0], 3, Shape.STATIC_STORE);*/

        /*shape.disableVbo(textureIndex);
        shape.disableVbo(normalIndex);*/

        setPosition(position);
    }

    public void addNormal(){
        float[] table = {
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,

                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,

                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,

                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,

                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        };

        normalIndex = shape.addVboFloat(table, 3, Shape.STATIC_STORE);
    }
}
