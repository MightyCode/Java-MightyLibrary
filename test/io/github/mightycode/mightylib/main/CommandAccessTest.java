package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandAccessTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(EngineConfig.COMMAND_ACCESS_PROPERTY);
    }

    @Test
    void debugGivesEverythingReleaseNothing() {
        EngineConfig debug = new EngineConfig().setMode(EEngineMode.DEBUG);
        assertEquals(ECommandLevel.DEV, debug.getCommandAccess());
        assertEquals(ECommandLevel.DEV, debug.getMaxCommandAccess());
        assertTrue(debug.isConsoleEnabled());

        EngineConfig release = new EngineConfig().setMode(EEngineMode.RELEASE);
        assertNull(release.getCommandAccess());
        assertEquals(ECommandLevel.USER, release.getMaxCommandAccess());
        assertFalse(release.isConsoleEnabled());
    }

    @Test
    void accessIsBoundedByTheMaximum() {
        EngineConfig config = new EngineConfig().setMode(EEngineMode.RELEASE).setCommandAccess(ECommandLevel.DEV);
        assertEquals(ECommandLevel.USER, config.getCommandAccess());

        config.setMaxCommandAccess(ECommandLevel.CHEAT);
        assertEquals(ECommandLevel.CHEAT, config.getCommandAccess());

        config.setCommandAccess(null);
        assertNull(config.getCommandAccess());
        assertFalse(config.isConsoleEnabled());
    }

    @Test
    void launchOptionChangesTheAccessWithinTheMaximum() {
        EngineConfig config = new EngineConfig().setMode(EEngineMode.RELEASE).setCommandAccess(ECommandLevel.USER);

        System.setProperty(EngineConfig.COMMAND_ACCESS_PROPERTY, "dev");
        assertEquals(ECommandLevel.USER, config.getCommandAccess());
        config.setMaxCommandAccess(ECommandLevel.DEV);
        assertEquals(ECommandLevel.DEV, config.getCommandAccess());

        System.setProperty(EngineConfig.COMMAND_ACCESS_PROPERTY, "NONE");
        assertNull(config.getCommandAccess());

        // A wrong value keeps the access of the game
        System.setProperty(EngineConfig.COMMAND_ACCESS_PROPERTY, "admin");
        assertEquals(ECommandLevel.USER, config.getCommandAccess());
    }
}
