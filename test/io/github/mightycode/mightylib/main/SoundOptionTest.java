package io.github.mightycode.mightylib.main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// -Dmightylib.sound=silent forces the silent sound; another value is ignored
class SoundOptionTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(EngineConfig.SOUND_PROPERTY);
    }

    @Test
    void onlySilentForcesTheSilentSound() {
        EngineConfig config = new EngineConfig();
        assertFalse(config.isSoundForcedSilent());

        System.setProperty(EngineConfig.SOUND_PROPERTY, " Silent ");
        assertTrue(config.isSoundForcedSilent());

        System.setProperty(EngineConfig.SOUND_PROPERTY, "openal");
        assertFalse(config.isSoundForcedSilent());
    }
}
