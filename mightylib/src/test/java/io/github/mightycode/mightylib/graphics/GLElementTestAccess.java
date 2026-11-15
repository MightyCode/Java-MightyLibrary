package io.github.mightycode.mightylib.graphics;

// Names an element without a GLResources: for the tests whose fake GL element sink creates the elements itself
public final class GLElementTestAccess {
    private GLElementTestAccess() {
    }

    public static <T extends GLElement> T named(T element, String name) {
        element.setName(name);
        return element;
    }
}
