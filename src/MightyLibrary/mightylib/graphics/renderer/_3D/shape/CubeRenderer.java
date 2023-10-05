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

        setPosition(position);
    }

    public void setTexturePosition(){
        float[] table = new float[12 * 6];

        for(int i = 0; i < 6; ++i){
            table[12 * i + 0] = 0.0f; table[12 * i + 1] = 1.0f;
            table[12 * i + 2] = 1.0f; table[12 * i + 3] = 1.0f;
            table[12 * i + 4] = 1.0f; table[12 * i + 5] = 0.0f;
            table[12 * i + 6] = 1.0f; table[12 * i + 7] = 0.0f;
            table[12 * i + 8] = 0.0f; table[12 * i + 9] = 0.0f;
            table[12 * i + 10] = 0.0f; table[12 * i + 11] = 1.0f;
        }

        textureIndex = shape.addVboFloat(table, 2, Shape.STATIC_STORE);
    }

    public void setNormal(){
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
