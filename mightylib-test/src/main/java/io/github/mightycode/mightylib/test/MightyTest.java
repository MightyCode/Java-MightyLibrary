package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.main.EngineConfig;

/**
 * Entry of the test kit: an engine started from the configuration of the game (same plugins), driven by the test.
 * The configuration is changed by the kit (strict resources, sources of the test): give one per engine.
 */
public final class MightyTest {
    private MightyTest() {}

    /**
     * An engine without window nor GL: plugins, resources (loaded on the CPU, their GL elements only recorded),
     * commands and services. No scene, no input, no console. Not started: add the sources of the test, then start.
     */
    public static TestEngine headless(EngineConfig config) {
        if (config == null)
            throw new IllegalArgumentException("config");

        return new TestEngine(config, true);
    }
}
