package io.github.mightycode.mightylib.main;

import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisplaySettingsTest {
    private static final Vector2i SCREEN = new Vector2i(2560, 1440);
    private static final Vector2i DEFAULT = new Vector2i(DisplaySettings.DEFAULT_WIDTH, DisplaySettings.DEFAULT_HEIGHT);

    @Test
    void windowedTakesItsSizeElseTheDefault() {
        DisplaySettings display = new DisplaySettings();
        assertEquals(EWindowMode.WINDOWED, display.getMode());
        assertEquals(DEFAULT, display.sizeOn(SCREEN));

        display.setWindowedSize(800, 600);
        assertEquals(new Vector2i(800, 600), display.sizeOn(SCREEN));
    }

    @Test
    void fullscreenTakesItsResolutionElseTheScreen() {
        DisplaySettings display = new DisplaySettings(EWindowMode.FULLSCREEN).setWindowedSize(800, 600);
        assertEquals(SCREEN, display.sizeOn(SCREEN));

        display.setResolution(1920, 1080);
        assertEquals(new Vector2i(1920, 1080), display.sizeOn(SCREEN));
    }

    @Test
    void borderlessAlwaysCoversTheScreen() {
        DisplaySettings display = new DisplaySettings(EWindowMode.BORDERLESS)
                .setWindowedSize(800, 600).setResolution(1920, 1080);
        assertEquals(SCREEN, display.sizeOn(SCREEN));
    }

    @Test
    void unknownScreenFallsBackOnTheWindowedSize() {
        DisplaySettings display = new DisplaySettings(EWindowMode.BORDERLESS);
        assertEquals(DEFAULT, display.sizeOn(null));
        assertEquals(DEFAULT, display.sizeOn(new Vector2i(0, 0)));

        display.setMode(EWindowMode.FULLSCREEN).setWindowedSize(800, 600);
        assertEquals(new Vector2i(800, 600), display.sizeOn(null));
    }

    @Test
    void switchingModeKeepsTheSettingsOfTheOthers() {
        DisplaySettings display = new DisplaySettings()
                .setWindowedSize(800, 600).setDecorated(false).setResizable(true)
                .setResolution(1920, 1080).setRefreshRate(144).setMinimizeOnFocusLoss(false);

        display.setMode(EWindowMode.FULLSCREEN).setMode(EWindowMode.BORDERLESS).setMode(EWindowMode.WINDOWED);

        assertEquals(new Vector2i(800, 600), display.getWindowedSize());
        assertFalse(display.isDecorated());
        assertTrue(display.isResizable());
        assertEquals(new Vector2i(1920, 1080), display.getResolution());
        assertEquals(144, display.getRefreshRate());
        assertFalse(display.minimizesOnFocusLoss());
    }

    @Test
    void copyIsIndependent() {
        DisplaySettings display = new DisplaySettings(EWindowMode.FULLSCREEN).setWindowedSize(800, 600);
        DisplaySettings copy = display.copy();

        display.setMode(EWindowMode.WINDOWED).setWindowedSize(640, 360).setResolution(1280, 720);

        assertEquals(EWindowMode.FULLSCREEN, copy.getMode());
        assertEquals(new Vector2i(800, 600), copy.getWindowedSize());
        assertNull(copy.getResolution());
    }

    @Test
    void configsKeepTheirOwnCopy() {
        DisplaySettings display = new DisplaySettings(EWindowMode.BORDERLESS);
        WindowConfig windowConfig = new WindowConfig().setDisplay(display);
        EngineConfig engineConfig = new EngineConfig().setDisplay(display);

        display.setMode(EWindowMode.FULLSCREEN);
        windowConfig.getDisplay().setMode(EWindowMode.WINDOWED);

        assertEquals(EWindowMode.BORDERLESS, windowConfig.getDisplay().getMode());
        assertEquals(EWindowMode.BORDERLESS, engineConfig.getDisplay().getMode());
    }

    @Test
    void shortcutsChangeTheDisplay() {
        WindowConfig windowConfig = new WindowConfig().setSize(640, 360).setWindowMode(EWindowMode.FULLSCREEN);
        assertEquals(EWindowMode.FULLSCREEN, windowConfig.getDisplay().getMode());
        assertEquals(new Vector2i(640, 360), windowConfig.getDisplay().getWindowedSize());
        assertNull(windowConfig.getVirtualSize());

        EngineConfig engineConfig = new EngineConfig().setWindowSize(1920, 1080).setWindowMode(EWindowMode.BORDERLESS);
        assertEquals(EWindowMode.BORDERLESS, engineConfig.getDisplay().getMode());
        assertEquals(new Vector2i(1920, 1080), engineConfig.getDisplay().getWindowedSize());
        assertNull(engineConfig.getVirtualSize());
    }

    @Test
    void invalidValuesAreRefused() {
        DisplaySettings display = new DisplaySettings();
        assertThrows(IllegalArgumentException.class, () -> display.setMode(null));
        assertThrows(IllegalArgumentException.class, () -> display.setWindowedSize(0, 600));
        assertThrows(IllegalArgumentException.class, () -> display.setResolution(1920, -1));
        assertThrows(IllegalArgumentException.class, () -> display.setRefreshRate(-1));
    }

    @Test
    void modeFromText() {
        assertEquals(EWindowMode.BORDERLESS, EWindowMode.FromText("Borderless"));
        assertEquals(EWindowMode.FULLSCREEN, EWindowMode.FromText("FULLSCREEN"));
        assertNull(EWindowMode.FromText("maximized"));
    }
}
