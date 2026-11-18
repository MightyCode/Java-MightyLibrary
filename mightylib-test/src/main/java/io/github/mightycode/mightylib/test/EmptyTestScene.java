package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.scenes.Scene;

/**
 * The scene of an engine with a window between the tests: nothing to load, nothing to draw but the clear color.
 * The leaks are measured on it, before and after each test.
 */
public final class EmptyTestScene extends Scene {
}
