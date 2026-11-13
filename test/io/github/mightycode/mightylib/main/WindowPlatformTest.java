package io.github.mightycode.mightylib.main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.glfw.GLFW.*;

class WindowPlatformTest {
    @Test
    void parsesThePlatformNamesWhateverTheCase() {
        assertEquals(GLFW_PLATFORM_X11, WindowPlatform.Parse("x11"));
        assertEquals(GLFW_PLATFORM_WAYLAND, WindowPlatform.Parse(" Wayland "));
        assertEquals(GLFW_PLATFORM_WIN32, WindowPlatform.Parse("WIN32"));
        assertEquals(GLFW_PLATFORM_COCOA, WindowPlatform.Parse("cocoa"));
        assertEquals(GLFW_PLATFORM_NULL, WindowPlatform.Parse("null"));
    }

    @Test
    void anUnknownNameLetsGlfwChoose() {
        assertEquals(GLFW_ANY_PLATFORM, WindowPlatform.Parse("mir"));
    }
}
