package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.resources.EngineResources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The fonts of the interface and of the console: the fonts of the engine unless the game says otherwise
class EngineFontsConfigTest {
    @Test
    void defaultsAreTheFontsOfTheEngine() {
        EngineConfig config = new EngineConfig();

        assertEquals(EngineResources.DEFAULT_FONT, config.getDefaultFont());
        assertEquals(EngineResources.CONSOLE_FONT, config.getConsoleFont());
    }

    @Test
    void theGameChoosesItsFonts() {
        EngineConfig config = new EngineConfig().setDefaultFont("title").setConsoleFont(EngineResources.CONSOLE_FONT_BOLD);

        assertEquals("title", config.getDefaultFont());
        assertEquals(EngineResources.CONSOLE_FONT_BOLD, config.getConsoleFont());
    }
}
