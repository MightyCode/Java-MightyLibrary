package MightyLibrary.mightylib.graphics.renderer._3D.shape;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.util.math.EDirection3D;
import MightyLibrary.mightylib.util.math.EFlip;
import MightyLibrary.mightylib.util.math.ERotation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class CubeRenderer extends Renderer {
    public static class Face {
        public EFlip flip;
        public ERotation rotation;
    }

    private final Map<EDirection3D, Face> faces;

    private boolean includeTextures;
    private boolean includeNormal;

    private final int positionIndex;
    private int textureIndex;
    private int normalIndex;

    public CubeRenderer(String shaderName){
        super(shaderName, false);

        faces = new HashMap<>();
        for (EDirection3D direction3D : EDirection3D.values())
            faces.put(direction3D, new Face());

        float[] vertices = {
                        // Negative X face
                        0.0f,  1,  1,
                        0.0f,  1, 0.0f,
                        0.0f, 0.0f, 0.0f,

                        0.0f, 0.0f, 0.0f,
                        0.0f, 0.0f,  1,
                        0.0f,  1,  1,

                        // Positive X face
                        1,  1,  1,
                        1,  1, 0.0f,
                        1, 0.0f, 0.0f,

                        1, 0.0f, 0.0f,
                        1, 0.0f,  1,
                        1,  1,  1,

                        // Negative Y face
                        0.0f, 0.0f, 0.0f,
                        1, 0.0f, 0.0f,
                        1, 0.0f,  1,

                        1, 0.0f,  1,
                        0.0f, 0.0f,  1,
                        0.0f, 0.0f, 0.0f,

                        // Positive Y face
                        0.0f,  1, 0.0f,
                        1,  1, 0.0f,
                        1,  1,  1,

                        1,  1,  1,
                        0.0f,  1,  1,
                        0.0f,  1, 0.0f,

                        // Negative z face
                        0.0f, 0.0f, 0.0f,
                        1, 0.0f, 0.0f,
                        1,  1, 0.0f,

                        1,  1, 0.0f,
                        0.0f,  1, 0.0f,
                        0.0f, 0.0f, 0.0f,

                        // Positive z face
                        0.0f, 0.0f,  1,
                        1, 0.0f,  1,
                        1,  1,  1,

                        1,  1,  1,
                        0.0f,  1,  1,
                        0.0f, 0.0f,  1,
        };

        positionIndex = shape.addVboFloat(vertices, 3, Shape.STATIC_STORE);

        setPosition(new Vector3f(0));
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
                0.0f, 1.0f, 0.0f,

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
                0.0f, 0.0f, 1.0f
        };

        normalIndex = shape.addVboFloat(table, 3, Shape.STATIC_STORE);
    }
}
