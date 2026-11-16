package io.github.mightycode.mightylib.main;

// Windows without GLFW window nor inputs, to tell two windows apart in the tests
public final class TestWindows {
    private TestWindows() {}

    public static WindowContext create(String name) {
        return new WindowContext(0, null, name, false, null, null, null, null, null);
    }
}
