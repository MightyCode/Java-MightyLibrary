package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.main.EngineConfig;

import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * The configuration of the demo for its tests: the one of the game (Main.createConfig), with the folder of the
 * resources seen from the working directory of the tests (the module folder, the resources are at the root).
 * Example of a game: the test kit starts the engine from it (@MightyEngineTest, @MightySceneTest).
 */
public final class DemoTestConfig implements Supplier<EngineConfig> {
    @Override
    public EngineConfig get() {
        return Main.createConfig().setResourcesFolder(Paths.get("..", "resources").toString());
    }
}
