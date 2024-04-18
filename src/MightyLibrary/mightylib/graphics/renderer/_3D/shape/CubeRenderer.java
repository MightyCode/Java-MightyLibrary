package MightyLibrary.mightylib.graphics.renderer._3D.shape;

import MightyLibrary.mightylib.graphics.renderer.RectangularFace;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.utils.math.EDirection3D;
import MightyLibrary.mightylib.utils.math.MightyMath;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class CubeRenderer extends Renderer {
    private static final int FACE_NUMBER = 6;
    private static final int VERTEX_TEXTURE_SIZE = 2;
    private static final int VERTEX_POSITION_SIZE = 3;
    private static final int VERTEX_NORMAL_SIZE = 3;
    private static final int VERTEX_NUMBER = 4;

    private static final int FACE_EBO_SIZE = 6;

    private static final float[] TEXTURE_POSITION = new float[]{
            0, 0,
            0, 1,
            1, 1,
            1, 0
    };

    private static final EDirection3D[] FACES_ORDER = {
            EDirection3D.Forward, EDirection3D.Backward,
            EDirection3D.Right, EDirection3D.Left,
            EDirection3D.Up, EDirection3D.Down
    };

    private final Map<EDirection3D, RectangularFace> faces;

    private boolean includeTextures;
    private boolean includeNormal;
    private final int positionIndex;
    private int textureIndex;
    private int normalIndex;

    public CubeRenderer(String shaderName){
        super(shaderName, true);

        faces = new HashMap<>();
        for (EDirection3D direction3D : EDirection3D.values())
            faces.put(direction3D, new RectangularFace());

        /*faces.get(EDirection3D.Forward).Rotation = ERotation.TwoPi;
        faces.get(EDirection3D.Forward).Flip = EFlip.Horizontal;*/

        float[] vertices = {
                // Positive X face
                1, 1, 1,
                1, 0, 1,
                1, 0, 0,
                1, 1, 0,

                // Negative X face
                0, 1, 0,
                0, 0, 0,
                0, 0, 1,
                0, 1, 1,

                // Positive z face
                0, 1, 1,
                0, 0, 1,
                1, 0, 1,
                1, 1, 1,

                // Negative z face
                1, 1, 0,
                1, 0, 0,
                0, 0, 0,
                0, 1, 0,

                // Positive Y face
                0, 1, 1,
                1, 1, 1,
                1, 1, 0,
                0, 1, 0,

                // Negative Y face
                0, 0, 0,
                1, 0, 0,
                1, 0, 1,
                0, 0, 1,
        };

        positionIndex = shape.addVboFloat(vertices, VERTEX_POSITION_SIZE, Shape.STATIC_STORE);

        int[] ref = RectangularFace.IndicesForSquare();

        int[] ebo = new int[FACE_EBO_SIZE * FACE_NUMBER];
        for (int i = 0; i < FACE_NUMBER; ++i){
            for (int j = 0; j < FACE_EBO_SIZE; ++j){
                ebo[i * FACE_EBO_SIZE + j] = ref[j] + VERTEX_NUMBER * i;
            }
        }

        shape.setEbo(ebo);

        setPosition(new Vector3f(0));
    }

    public void setTexturePosition(){
        float[] table = new float[VERTEX_NUMBER * VERTEX_TEXTURE_SIZE * FACE_NUMBER];

        for(int i = 0; i < FACE_NUMBER; ++i){
            RectangularFace current = faces.get(FACES_ORDER[i]);

            float [] vertexTexture = current.applyRotationFlip(TEXTURE_POSITION);

            System.arraycopy(vertexTexture, 0, table,
                    VERTEX_TEXTURE_SIZE * VERTEX_NUMBER * i, vertexTexture.length);
        }

        textureIndex = shape.addVboFloat(table, VERTEX_TEXTURE_SIZE, Shape.STATIC_STORE);
    }

    public void setNormal(){
        float[] table = new float[FACE_NUMBER * VERTEX_NUMBER * VERTEX_NORMAL_SIZE];
        for (int i = 0; i < FACE_NUMBER; ++i) {
            Vector3f direction = MightyMath.ToVector(FACES_ORDER[i]);
            for (int j = 0; j < VERTEX_NUMBER; ++j) {
                table[i * VERTEX_NUMBER * VERTEX_NORMAL_SIZE  + j * VERTEX_NORMAL_SIZE] = direction.x;
                table[i * VERTEX_NUMBER * VERTEX_NORMAL_SIZE + j * VERTEX_NORMAL_SIZE + 1] = direction.y;
                table[i * VERTEX_NUMBER * VERTEX_NORMAL_SIZE + j * VERTEX_NORMAL_SIZE + 2] = direction.z;
            }
        }

        normalIndex = shape.addVboFloat(table, VERTEX_NORMAL_SIZE, Shape.STATIC_STORE);
    }
}
