package io.github.mightycode.mightylib.resources.models;

import io.github.mightycode.mightylib.resources.Resources;

import java.util.Collections;

/**
 * Fallback of the models: a unit cube with the fallback texture, created by the code.
 * Displayed in place of a model that failed, is still loading or has an unknown name.
 */
public class FallbackModel extends Model {
    // For each face: normal, then the two axes of the face plane
    private static final float[][] FACES = {
            { 0, 0, 1,   1, 0, 0,   0, 1, 0 },
            { 0, 0, -1, -1, 0, 0,   0, 1, 0 },
            { 1, 0, 0,   0, 0, -1,  0, 1, 0 },
            { -1, 0, 0,  0, 0, 1,   0, 1, 0 },
            { 0, 1, 0,   1, 0, 0,   0, 0, -1 },
            { 0, -1, 0,  1, 0, 0,   0, 0, 1 },
    };
    private static final float[][] CORNERS = { { -1, -1 }, { 1, -1 }, { 1, 1 }, { -1, 1 } };

    public FallbackModel() {
        super(Resources.FALLBACK_NAME, null);
    }

    @Override
    public boolean internLoad() {
        float[] positions = new float[6 * 4 * 3];
        float[] normals = new float[6 * 4 * 3];
        float[] textureCoordinates = new float[6 * 4 * 2];
        int[] indices = new int[6 * 6];

        for (int face = 0; face < FACES.length; ++face) {
            float[] f = FACES[face];
            for (int corner = 0; corner < 4; ++corner) {
                int vertex = face * 4 + corner;
                float u = CORNERS[corner][0], v = CORNERS[corner][1];
                for (int axis = 0; axis < 3; ++axis) {
                    positions[vertex * 3 + axis] = 0.5f * (f[axis] + u * f[3 + axis] + v * f[6 + axis]);
                    normals[vertex * 3 + axis] = f[axis];
                }
                textureCoordinates[vertex * 2] = (u + 1) / 2;
                textureCoordinates[vertex * 2 + 1] = (v + 1) / 2;
            }

            int first = face * 4;
            int[] faceIndices = { first, first + 1, first + 2, first + 2, first + 3, first };
            System.arraycopy(faceIndices, 0, indices, face * 6, 6);
        }

        setMeshes(Collections.singletonList(new ModelMesh("cube", positions, normals, textureCoordinates, indices,
                Resources.FALLBACK_NAME, null, 32f)));

        return super.internLoad();
    }
}
