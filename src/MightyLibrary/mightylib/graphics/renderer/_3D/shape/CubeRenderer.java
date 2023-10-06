package MightyLibrary.mightylib.graphics.renderer._3D.shape;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.util.math.EDirection3D;
import MightyLibrary.mightylib.util.math.EFlip;
import MightyLibrary.mightylib.util.math.ERotation;
import MightyLibrary.mightylib.util.valueDebug.TableDebug;
import org.joml.Vector3f;
import org.joml.Vector4f;

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

    public static class Face {
        public EFlip Flip;
        public ERotation Rotation;

        public Face(){
            Flip = EFlip.None;
            Rotation = ERotation.None;
        }
    }

    private final Map<EDirection3D, Face> faces;

    private boolean includeTextures;
    private boolean includeNormal;
    private final int positionIndex;
    private int textureIndex;
    private int normalIndex;

    private static final EDirection3D[] FACES_ORDER = {
            EDirection3D.Forward, EDirection3D.Backward,
            EDirection3D.Right, EDirection3D.Left,
            EDirection3D.Up, EDirection3D.Down
    };

    public CubeRenderer(String shaderName){
        super(shaderName, true);

        faces = new HashMap<>();
        for (EDirection3D direction3D : EDirection3D.values())
            faces.put(direction3D, new Face());

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

        int[] ref = RendererUtils.indicesForSquare();

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
            Face current = faces.get(FACES_ORDER[i]);

            float [] vertexTexture = RendererUtils.ApplyRotationFlip(TEXTURE_POSITION, current.Flip, current.Rotation);

            System.arraycopy(vertexTexture, 0, table,
                    VERTEX_TEXTURE_SIZE * VERTEX_NUMBER * i, vertexTexture.length);
        }

        textureIndex = shape.addVboFloat(table, VERTEX_TEXTURE_SIZE, Shape.STATIC_STORE);
    }

    public void setNormal(){
        float[] table = {
                1, 0, 0,
                1, 0, 0,
                1, 0, 0,
                1, 0, 0,

                -1, 0, 0,
                -1, 0, 0,
                -1, 0, 0,
                -1, 0, 0,

                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,

                0, 0, -1,
                0, 0, -1,
                0, 0, -1,
                0, 0, -1,

                0, 1, 0,
                0, 1, 0,
                0, 1, 0,
                0, 1, 0,

                0, -1, 0,
                0, -1, 0,
                0, -1, 0,
                0, -1, 0,
        };

        normalIndex = shape.addVboFloat(table, VERTEX_NORMAL_SIZE, Shape.STATIC_STORE);
    }
}
