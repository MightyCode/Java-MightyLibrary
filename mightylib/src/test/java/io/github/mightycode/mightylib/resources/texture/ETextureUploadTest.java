package io.github.mightycode.mightylib.resources.texture;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ETextureUploadTest {
    @AfterEach
    void restore() {
        TextureUploader.setMode(ETextureUpload.DIRECT);
    }

    @Test
    void parseIgnoresCaseAndSpaces() {
        assertEquals(ETextureUpload.PBO, ETextureUpload.parse("pbo"));
        assertEquals(ETextureUpload.PBO, ETextureUpload.parse(" PBO "));
        assertEquals(ETextureUpload.DIRECT, ETextureUpload.parse("Direct"));
    }

    @Test
    void parseGivesNullForAnUnknownOrMissingName() {
        assertNull(ETextureUpload.parse("dma"));
        assertNull(ETextureUpload.parse(""));
        assertNull(ETextureUpload.parse(null));
    }

    @Test
    void modeCanBeChangedAndNullMeansDirect() {
        TextureUploader.setMode(ETextureUpload.PBO);
        assertEquals(ETextureUpload.PBO, TextureUploader.getMode());

        TextureUploader.setMode(null);
        assertEquals(ETextureUpload.DIRECT, TextureUploader.getMode());
    }

    @Test
    void describeStartsAtZeroWhenTheModeIsSet() {
        TextureUploader.setMode(ETextureUpload.PBO);
        JSONObject description = TextureUploader.describe();

        assertEquals("PBO", description.getString("mode"));
        assertEquals(0, description.getInt("strips"));
        assertEquals(0, description.getInt("pixelBuffers"));
    }
}
