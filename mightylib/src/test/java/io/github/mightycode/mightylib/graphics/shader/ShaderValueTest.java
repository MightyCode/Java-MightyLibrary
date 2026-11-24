package io.github.mightycode.mightylib.graphics.shader;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The version of a value: what keeps a copy of its content knows it changed without comparing it (when tracked)
class ShaderValueTest {
    @Test
    void setObjectCopyAndMarkChangedMakeANewVersion() {
        Matrix4f matrix = new Matrix4f();
        ShaderValue value = new ShaderValue("view", Matrix4f.class, matrix);
        int version = value.version();

        // Changed in place without notice: the version does not see it
        matrix.translate(1, 0, 0);
        assertEquals(version, value.version());

        value.markChanged();
        assertEquals(version + 1, value.version());
        value.setObject(new Matrix4f());
        assertEquals(version + 2, value.version());
        assertTrue(value.copyValueFrom(new ShaderValue("view", Matrix4f.class, new Matrix4f().scale(2))));
        assertEquals(version + 3, value.version());

        // A copy that is not made in place changes nothing
        assertFalse(value.copyValueFrom(new ShaderValue("view", Float.class, 1f)));
        assertEquals(version + 3, value.version());
    }

    @Test
    void aValueIsTrackedOnlyWhenItsOwnerSaysSo() {
        ShaderValue value = new ShaderValue("projection", Matrix4f.class, new Matrix4f());
        assertFalse(value.isTracked());
        assertSame(value, value.trackChanges());
        assertTrue(value.isTracked());
        // A copy belongs to whoever made it: not tracked
        assertFalse(value.copy().isTracked());
    }
}
