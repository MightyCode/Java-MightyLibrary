package io.github.mightycode.mightylib.main;

import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.Platform;
import org.lwjgl.system.windows.User32;

import static org.lwjgl.glfw.GLFW.GLFW_FLOATING;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwSetWindowAttrib;

/**
 * Makes a window belong to another one like a dialog of a desktop program: it stays above its owner, is hidden with
 * it when it is minimized and has no entry of its own in the task bar. Only Windows gives that natively here; on
 * the other systems the window floats above the others instead.
 */
final class NativeWindowOwner {
    private NativeWindowOwner() {}

    static void set(long ownedWindow, long ownerWindow) {
        if (Platform.get() == Platform.WINDOWS) {
            long owned = GLFWNativeWin32.glfwGetWin32Window(ownedWindow);
            long owner = GLFWNativeWin32.glfwGetWin32Window(ownerWindow);

            // "Parent" of GWL_HWNDPARENT is the owner for a top-level window
            User32.SetWindowLongPtr(null, owned, User32.GWL_HWNDPARENT, owner);
        } else if (WindowPlatform.CanFloatWindows()) {
            glfwSetWindowAttrib(ownedWindow, GLFW_FLOATING, GLFW_TRUE);
        }
    }
}
