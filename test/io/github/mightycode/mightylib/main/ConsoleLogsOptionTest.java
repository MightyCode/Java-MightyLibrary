package io.github.mightycode.mightylib.main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleLogsOptionTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(EngineConfig.CONSOLE_LOGS_PROPERTY);
    }

    @Test
    void parse() {
        assertEquals(Boolean.TRUE, EngineConfig.ParseLogsShown("shown"));
        assertEquals(Boolean.FALSE, EngineConfig.ParseLogsShown(" HIDDEN "));
        assertNull(EngineConfig.ParseLogsShown("off"));
        assertNull(EngineConfig.ParseLogsShown(""));
    }

    @Test
    void theLaunchOptionWinsAnUnknownValueKeepsTheConfiguration() {
        EngineConfig config = new EngineConfig();
        assertTrue(config.isConsoleLogsShown());
        config.setConsoleLogsShown(false);
        assertFalse(config.isConsoleLogsShown());

        System.setProperty(EngineConfig.CONSOLE_LOGS_PROPERTY, "shown");
        assertTrue(config.isConsoleLogsShown());
        System.setProperty(EngineConfig.CONSOLE_LOGS_PROPERTY, "visible");
        assertFalse(config.isConsoleLogsShown());
    }
}
