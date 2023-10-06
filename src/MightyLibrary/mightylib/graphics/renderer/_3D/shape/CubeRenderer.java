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
    private static final int FACE_NUMBER = 6;
    private static final int VERTEX_TEXTURE_SIZE = 2;
    private static final int VERTEX_POSITION_SIZE = 3;
    private static final int VERTEX_NORMAL_SIZE = 3;
    private static final int VERTEX_NUMBER = 6;

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
            // Positive X face
            1, 1, 1,
            1, 1, 0,
            1, 0, 0,

            1, 1, 1,
            1, 0, 0,
            1, 0, 1,

            // Negative X face
            0, 1, 0,
            0, 1, 1,
            0, 0, 1,

            0, 1, 0,
            0, 0, 1,
            0, 0, 0,

            // Positive z face
            0, 1, 1,
            1, 1, 1,
            1, 0, 1,

            0, 1, 1,
            1, 0, 1,
            0, 0, 1,

            // Negative z face
            1, 1, 0,
            0, 1, 0,
            0, 0, 0,

            1, 1, 0,
            0, 0, 0,
            1, 0, 0,

            // Positive Y face
            0, 1, 1,
            0, 1, 0,
            1, 1, 0,

            0, 1, 1,
            1, 1, 0,
            1, 1, 1,

            // Negative Y face
            0, 0, 0,
            0, 0, 1,
            1, 0, 1,

            0, 0, 0,
            1, 0, 1,
            1, 0, 0,
        };

        positionIndex = shape.addVboFloat(vertices, VERTEX_POSITION_SIZE, Shape.STATIC_STORE);

        setPosition(new Vector3f(0));
    }

    public void setTexturePosition(){
        float[] table = new float[VERTEX_NUMBER * VERTEX_TEXTURE_SIZE * FACE_NUMBER];

        for(int i = 0; i < FACE_NUMBER; ++i){
            table[12 * i + 0]  = 0; table[12 * i + 1] = 0;
            table[12 * i + 2]  = 1; table[12 * i + 3] = 0;
            table[12 * i + 4]  = 1; table[12 * i + 5] = 1;

            table[12 * i + 6]  = 0; table[12 * i + 7]  = 0;
            table[12 * i + 8]  = 1; table[12 * i + 9]  = 1;
            table[12 * i + 10] = 0; table[12 * i + 11] = 1;
        }

        textureIndex = shape.addVboFloat(table, 2, Shape.STATIC_STORE);
    }

    public void setNormal(){
        float[] table = {
                1, 0, 0,
                1, 0, 0,
                1, 0, 0,
                1, 0, 0,
                1, 0, 0,
                1, 0, 0,

                -1, 0, 0,
                -1, 0, 0,
                -1, 0, 0,
                -1, 0, 0,
                -1, 0, 0,
                -1, 0, 0,

                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,

                0, 0, -1,
                0, 0, -1,
                0, 0, -1,
                0, 0, -1,
                0, 0, -1,
                0, 0, -1,

                0, 1, 0,
                0, 1, 0,
                0, 1, 0,
                0, 1, 0,
                0, 1, 0,
                0, 1, 0,

                0, -1, 0,
                0, -1, 0,
                0, -1, 0,
                0, -1, 0,
                0, -1, 0,
                0, -1, 0,
        };

        normalIndex = shape.addVboFloat(table, VERTEX_NORMAL_SIZE, Shape.STATIC_STORE);
    }
}
