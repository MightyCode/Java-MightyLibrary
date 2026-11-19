package io.github.mightycode.mightylib.resources.models;

import io.github.mightycode.mightylib.resources.texture.ETextureColorSpace;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The role of a texture in a model fixes its encoding; a file shared by two roles keeps the first one
class ModelTextureRoleTest {
    @Test
    void sameRoleIsNoConflict() {
        TextureData texture = new TextureData("model/diffuse.png", "model/diffuse.png");
        assertFalse(ModelLoader.WarnIfOtherRole("model", texture, ETextureColorSpace.SRGB));
    }

    @Test
    void otherRoleKeepsTheFirst() {
        TextureData texture = new TextureData("model/both.png", "model/both.png");
        texture.setColorSpace(ETextureColorSpace.LINEAR);

        assertTrue(ModelLoader.WarnIfOtherRole("model", texture, ETextureColorSpace.SRGB));
        assertEquals(ETextureColorSpace.LINEAR, texture.getColorSpace());
    }
}
