package io.github.mightycode.mightylib.main;

import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WindowSizeTest {
    private static final Vector2i SCREEN = new Vector2i(2560, 1440);
    private static final Vector2i DEFAULT = new Vector2i(WindowConfig.DEFAULT_WIDTH, WindowConfig.DEFAULT_HEIGHT);

    @Test
    void fullscreenWithoutSizeTakesTheScreenSize() {
        assertEquals(SCREEN, Window.SizeWithoutGivenSize(true, SCREEN));
    }

    @Test
    void windowWithoutSizeTakesTheDefaultSize() {
        assertEquals(DEFAULT, Window.SizeWithoutGivenSize(false, SCREEN));
    }

    @Test
    void fullscreenOnAnUnknownScreenTakesTheDefaultSize() {
        assertEquals(DEFAULT, Window.SizeWithoutGivenSize(true, null));
        assertEquals(DEFAULT, Window.SizeWithoutGivenSize(true, new Vector2i(0, 0)));
    }

    @Test
    void windowConfigWithoutSizeLeavesItToTheWindow() {
        WindowConfig config = new WindowConfig().setFullscreen(true);
        assertNull(config.getSize());
        assertNull(config.getVirtualSize());

        config.setVirtualSize(1280, 720);
        assertNull(config.getSize());
        assertEquals(new Vector2i(1280, 720), config.getVirtualSize());
    }

    @Test
    void engineConfigWithoutSizeIsTheScreenInFullscreenAndTheDefaultInAWindow() {
        EngineConfig config = new EngineConfig();
        Vector2i engineDefault = new Vector2i(EngineConfig.DEFAULT_WINDOW_WIDTH, EngineConfig.DEFAULT_WINDOW_HEIGHT);
        assertEquals(engineDefault, config.getWindowSize());
        assertEquals(engineDefault, config.getVirtualSize());

        config.setFullscreen(true);
        assertNull(config.getWindowSize());
        assertNull(config.getVirtualSize());

        config.setWindowSize(1920, 1080);
        assertEquals(new Vector2i(1920, 1080), config.getWindowSize());
        assertEquals(new Vector2i(1920, 1080), config.getVirtualSize());
    }
}
