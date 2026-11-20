package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.RectangularFace;
import io.github.mightycode.mightylib.graphics.renderer.RendererUtils;
import io.github.mightycode.mightylib.utils.math.geometry.Direction;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylib.utils.math.geometry.EFlip;
import io.github.mightycode.mightylib.utils.math.geometry.ERotation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The vertices of a batched shape are those its vertex shader computes when it is drawn alone
class Shape2DBatchGeometryTest {
    // Like Renderer.applyModel: translation, rotation around Z, scale (the size of a rectangle)
    private static Matrix4f model(float x, float y, float angle, float width, float height) {
        return new Matrix4f().translate(new Vector3f(x, y, 0)).rotate(angle, new Vector3f(0, 0, 1))
                .scale(new Vector3f(width, height, 1));
    }

    @Test
    void positionsAreTransformedLikeTheShader() {
        float[] angles = {0f, 0.3f, -1.2f, (float) Math.PI, 5f};
        for (EDirection reference : EDirection.values()) {
            if (reference == EDirection.Undef)
                continue;

            Vector2f point = Direction.DirectionToRangedVector(reference, new Vector4f(0, 0, 1, 1));
            float[] positions = RendererUtils.calculatePositionForSquare(new Vector2f(1, 1), point);
            for (float angle : angles) {
                Matrix4f model = model(123.5f, -40.25f, angle, 37f, 12.5f);
                float[] transformed = new float[positions.length];
                Shape2DRenderer.TransformPositions(model, positions, transformed);

                for (int i = 0; i < positions.length; i += 2) {
                    Vector4f shader = model.transform(new Vector4f(positions[i], positions[i + 1], 0, 1));
                    assertEquals(shader.x, transformed[i], 1e-3f, reference + " " + angle);
                    assertEquals(shader.y, transformed[i + 1], 1e-3f, reference + " " + angle);
                }
            }
        }
    }

    // A model of the transform only (positions given already in world units)
    @Test
    void theIdentityKeepsThePositions() {
        float[] positions = {0, 0, 1, 2, -3, 4.5f};
        float[] transformed = new float[positions.length];
        Shape2DRenderer.TransformPositions(new Matrix4f(), positions, transformed);
        assertArrayEquals(positions, transformed);
    }

    // The batch takes the texture coordinates of the same hook as the mesh: every flip and rotation of the face
    // gives four corners, one per vertex
    @Test
    void everyFaceGivesOneCoordinatePerVertex() {
        float[] positions = RendererUtils.calculatePositionForSquare(new Vector2f(1, 1), new Vector2f());
        for (EFlip flip : EFlip.values()) {
            for (ERotation rotation : ERotation.values()) {
                RectangularFace face = new RectangularFace();
                face.setFlip(flip);
                face.setRotation(rotation);
                float[] coordinates = face.textureCornerPosition(new Vector4f(0.25f, 0.75f, 0.1f, 0.6f));
                assertEquals(positions.length, coordinates.length, flip + " " + rotation);
            }
        }
    }
}
