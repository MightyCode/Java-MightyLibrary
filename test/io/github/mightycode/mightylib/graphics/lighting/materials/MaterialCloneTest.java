package io.github.mightycode.mightylib.graphics.lighting.materials;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// A clone has its own vectors: changing it leaves the original as it is (the clones shared them)
class MaterialCloneTest {
    @Test
    void basicMaterialCloneIsDeep() {
        BasicMaterial original = new BasicMaterial(new Vector3f(1), new Vector3f(2), new Vector3f(3));
        BasicMaterial copy = original.clone();
        copy.Ambient.set(9);
        copy.Diffuse.set(9);
        copy.Specular.set(9);

        assertEquals(new Vector3f(1), original.Ambient);
        assertEquals(new Vector3f(2), original.Diffuse);
        assertEquals(new Vector3f(3), original.Specular);
        assertEquals(new Vector3f(3), original.cloneBasicMaterial().Specular);
        assertNotSame(original.Ambient, original.cloneBasicMaterial().Ambient);
    }

    @Test
    void materialCloneIsDeep() {
        Material original = Material.Gold();
        Material copy = original.clone();
        copy.Diffuse.set(0);
        copy.Shininess = 1;

        assertEquals(Material.Gold().Diffuse, original.Diffuse);
        assertEquals(Material.Gold().Shininess, original.Shininess);
    }
}
