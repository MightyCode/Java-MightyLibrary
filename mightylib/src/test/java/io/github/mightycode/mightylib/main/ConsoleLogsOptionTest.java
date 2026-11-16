package io.github.mightycode.mightylib.main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleLogsOptionTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(EngineConfig.CONSOLE_LOGS_PROPERTY);
        System.clearProperty(EngineConfig.CONSOLE_DATA_PROPERTY);
    }

    @Test
    void parse() {
        assertEquals(Optional.of(true), EngineConfig.ParseShown("shown"));
        assertEquals(Optional.of(false), EngineConfig.ParseShown(" HIDDEN "));
        assertTrue(EngineConfig.ParseShown("off").isEmpty());
        assertTrue(EngineConfig.ParseShown("").isEmpty());
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

    @Test
    void theDataIsHiddenByDefaultAndHasItsOwnOption() {
        EngineConfig config = new EngineConfig();
        assertFalse(config.isConsoleDataShown());
        config.setConsoleDataShown(true);
        assertTrue(config.isConsoleDataShown());

        System.setProperty(EngineConfig.CONSOLE_DATA_PROPERTY, "hidden");
        assertFalse(config.isConsoleDataShown());
        // The option of the logs doesn't change the data
        System.setProperty(EngineConfig.CONSOLE_LOGS_PROPERTY, "shown");
        assertFalse(config.isConsoleDataShown());
        System.setProperty(EngineConfig.CONSOLE_DATA_PROPERTY, "visible");
        assertTrue(config.isConsoleDataShown());
    }
}
