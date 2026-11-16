package io.github.mightycode.mightylib.graphics.lighting.materials;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// A copy has its own vectors: changing it leaves the original as it is (the copies shared them)
class MaterialCopyTest {
    @Test
    void basicMaterialCopyIsDeep() {
        BasicMaterial original = new BasicMaterial(new Vector3f(1), new Vector3f(2), new Vector3f(3));
        BasicMaterial copy = original.copy();
        copy.Ambient.set(9);
        copy.Diffuse.set(9);
        copy.Specular.set(9);

        assertEquals(new Vector3f(1), original.Ambient);
        assertEquals(new Vector3f(2), original.Diffuse);
        assertEquals(new Vector3f(3), original.Specular);
        assertEquals(new Vector3f(3), original.copyBasicMaterial().Specular);
        assertNotSame(original.Ambient, original.copyBasicMaterial().Ambient);
    }

    @Test
    void materialCopyIsDeep() {
        Material original = Material.Gold();
        Material copy = original.copy();
        copy.Diffuse.set(0);
        copy.Shininess = 1;

        assertEquals(Material.Gold().Diffuse, original.Diffuse);
        assertEquals(Material.Gold().Shininess, original.Shininess);
    }
}
