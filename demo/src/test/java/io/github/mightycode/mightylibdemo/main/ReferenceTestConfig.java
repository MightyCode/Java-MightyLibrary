package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.main.EngineConfig;

import java.util.function.Supplier;

/**
 * The configuration of the image tests: the one of the demo (DemoTestConfig) in a window of 640x360, the size of
 * its virtual scene too (the references are of this size).
 */
public final class ReferenceTestConfig implements Supplier<EngineConfig> {
    public static final int WIDTH = 640;
    public static final int HEIGHT = 360;

    @Override
    public EngineConfig get() {
        return new DemoTestConfig().get().setWindowSize(WIDTH, HEIGHT).setVirtualSize(WIDTH, HEIGHT);
    }
}
