package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.test.MightyTest;
import io.github.mightycode.mightylib.test.TestEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// A test engine sees only the simulated inputs: what happens at the machine meanwhile (mouse, keys, gamepads) is ignored
class RealDevicesKitTest {
    @Test
    void theRealDevicesAreIgnoredWhileATestEngineRuns(@TempDir Path game) {
        assertFalse(RealDevices.isIgnored());

        try (TestEngine ignored = MightyTest.headless(new EngineConfig().setResourcesFolder(game.toString())).start()) {
            assertTrue(RealDevices.isIgnored());
            // No gamepad of the machine is seen, GLFW is not even asked
            assertFalse(new GlfwGamepadSource().isPresent(0));
        }

        assertFalse(RealDevices.isIgnored());
    }
}
