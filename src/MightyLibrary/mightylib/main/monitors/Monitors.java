package MightyLibrary.mightylib.main.monitors;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Connected screens, GLFW must be initialized. Call on the main thread.
 */
public final class Monitors {
    private Monitors() {}

    public static List<MonitorInfo> List() {
        PointerBuffer handles = glfwGetMonitors();
        if (handles == null)
            return Collections.emptyList();

        long primary = glfwGetPrimaryMonitor();
        List<MonitorInfo> monitors = new ArrayList<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1), y = stack.mallocInt(1);
            IntBuffer workX = stack.mallocInt(1), workY = stack.mallocInt(1);
            IntBuffer workWidth = stack.mallocInt(1), workHeight = stack.mallocInt(1);

            for (int i = 0; i < handles.limit(); ++i) {
                long handle = handles.get(i);

                glfwGetMonitorPos(handle, x, y);
                glfwGetMonitorWorkarea(handle, workX, workY, workWidth, workHeight);

                GLFWVidMode mode = glfwGetVideoMode(handle);
                int width = mode == null ? 0 : mode.width();
                int height = mode == null ? 0 : mode.height();
                int refreshRate = mode == null ? 0 : mode.refreshRate();

                String name = glfwGetMonitorName(handle);

                monitors.add(new MonitorInfo(i, handle, name == null ? "monitor" + i : name, handle == primary,
                        x.get(0), y.get(0), width, height, refreshRate,
                        workX.get(0), workY.get(0), workWidth.get(0), workHeight.get(0)));
            }
        }

        return monitors;
    }

    // Monitor whose area contains the center of the window, null if none (window between screens or not created)
    public static MonitorInfo OfWindow(long windowId, List<MonitorInfo> monitors) {
        if (windowId == NULL)
            return null;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1), y = stack.mallocInt(1);
            IntBuffer width = stack.mallocInt(1), height = stack.mallocInt(1);
            glfwGetWindowPos(windowId, x, y);
            glfwGetWindowSize(windowId, width, height);

            int centerX = x.get(0) + width.get(0) / 2;
            int centerY = y.get(0) + height.get(0) / 2;

            for (MonitorInfo monitor : monitors) {
                if (centerX >= monitor.getX() && centerX < monitor.getX() + monitor.getWidth()
                        && centerY >= monitor.getY() && centerY < monitor.getY() + monitor.getHeight())
                    return monitor;
            }
        }

        return null;
    }
}
