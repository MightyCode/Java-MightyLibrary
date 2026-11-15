package io.github.mightycode.mightylib.graphics.renderer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The texture units of addTextureChannel: 1 to MAX_TEXTURE_CHANNELS, an error otherwise (was silently ignored)
class MeshRendererChannelTest {
    @Test
    void channelsInRangeAreAccepted() {
        assertDoesNotThrow(() -> MeshRenderer.checkTextureChannel(1));
        assertDoesNotThrow(() -> MeshRenderer.checkTextureChannel(MeshRenderer.MAX_TEXTURE_CHANNELS));
    }

    @Test
    void channelsOutOfRangeAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> MeshRenderer.checkTextureChannel(0));
        assertThrows(IllegalArgumentException.class,
                () -> MeshRenderer.checkTextureChannel(MeshRenderer.MAX_TEXTURE_CHANNELS + 1));
    }
}
