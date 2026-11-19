package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Color space of the draws at the start: gamma by default, EngineConfig.setColorSpace, -Dmightylib.colorSpace wins
class ColorSpaceOptionTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(EngineConfig.COLOR_SPACE_PROPERTY);
    }

    @Test
    void gammaByDefaultAndSetByTheGame() {
        EngineConfig config = new EngineConfig();
        assertEquals(ERenderColorSpace.GAMMA, config.getColorSpace());

        config.setColorSpace(ERenderColorSpace.LINEAR);
        assertEquals(ERenderColorSpace.LINEAR, config.getColorSpace());

        assertThrows(IllegalArgumentException.class, () -> config.setColorSpace(ERenderColorSpace.INHERIT));
        assertThrows(IllegalArgumentException.class, () -> config.setColorSpace(null));
        assertEquals(ERenderColorSpace.LINEAR, config.getColorSpace());
    }

    @Test
    void propertyWinsAndUnknownValuesAreIgnored() {
        EngineConfig config = new EngineConfig();

        System.setProperty(EngineConfig.COLOR_SPACE_PROPERTY, " LINEAR ");
        assertEquals(ERenderColorSpace.LINEAR, config.getColorSpace());

        config.setColorSpace(ERenderColorSpace.LINEAR);
        System.setProperty(EngineConfig.COLOR_SPACE_PROPERTY, "gamma");
        assertEquals(ERenderColorSpace.GAMMA, config.getColorSpace());

        System.setProperty(EngineConfig.COLOR_SPACE_PROPERTY, "inherit");
        assertEquals(ERenderColorSpace.LINEAR, config.getColorSpace());
        System.setProperty(EngineConfig.COLOR_SPACE_PROPERTY, "srgb");
        assertEquals(ERenderColorSpace.LINEAR, config.getColorSpace());
    }
}
