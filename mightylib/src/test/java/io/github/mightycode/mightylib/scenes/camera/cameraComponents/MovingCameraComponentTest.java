package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovingCameraComponentTest {
    @Test
    void diagonalIsNotFaster() {
        Vector2f diagonal = MovingCameraComponent.ClampLength(new Vector2f(1, -1));
        assertEquals(1, diagonal.length(), 1e-6);
        assertEquals(-diagonal.x, diagonal.y, 1e-6);
    }

    @Test
    void partialStickIsKept() {
        Vector2f partial = MovingCameraComponent.ClampLength(new Vector2f(0.3f, 0.4f));
        assertEquals(0.3f, partial.x, 1e-6);
        assertEquals(0.4f, partial.y, 1e-6);
    }
}
