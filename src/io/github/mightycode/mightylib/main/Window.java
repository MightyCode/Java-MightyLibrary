package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.graphics.utils.Screenshot;
import io.github.mightycode.mightylib.resources.texture.Icon;
import io.github.mightycode.mightylib.utils.Logger;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;


import java.nio.IntBuffer;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL32C.GL_PROGRAM_POINT_SIZE;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Class of the game's main structure.
 *
 * @author MightyCode
 * @version of game : 2.2
 */
public final class Window {
    private final WindowInfo info;

    private GLContext glContext;

    // Window this one belongs to, 0 for none
    private long parentWindowId;

    public WindowInfo getInfo() { return info; }

    Window() {
        info = new WindowInfo();
        info.windowId = 0;

        info.size = new Vector2i(1, 1);
        info.ratio = 1;
        info.virtualSize = new Vector2i(1,1);
        info.virtualRatio = 1;

        info.windowName = "";
        info.fullscreen = false;
    }

    Window(WindowCreationInfo wci){
        info = new WindowInfo();
        info.windowId = 0;

        info.size = new Vector2i(wci.Size);
        info.ratio = (float) info.size.x / (float) info.size.y;
        info.virtualSize = new Vector2i(wci.VirtualSize);
        info.virtualRatio = (float) info.virtualSize.x / (float) info.virtualSize.y;

        info.windowName = wci.WindowName;
        parentWindowId = wci.ParentWindowId;
        info.fullscreen = wci.Fullscreen;
        info.monitor = wci.Monitor;
    }

    /**
     * Create the window and its OpenGL context, shared with the given one (the engine's hidden root context)
     * so that textures, buffers and programs are usable here too. On failure the window stays not created.
     */
    public void createNewWindow(GLContext shareWith){
        if (info.windowId != 0)
            destroyWindow();

        // Configure GLFW for this window
        GLContext.applyWindowHints();
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Active AntiAliasing
        glfwWindowHint(GLFW_SAMPLES, 4);

        long monitorHandle = info.monitor != null ? info.monitor.getHandle() : glfwGetPrimaryMonitor();
        long share = shareWith != null ? shareWith.getWindowHandle() : NULL;

        if (info.fullscreen)
            info.windowId = glfwCreateWindow(info.size.x, info.size.y, info.windowName, monitorHandle, share);
        else
            info.windowId = glfwCreateWindow(info.size.x, info.size.y, info.windowName, NULL, share);

        if (info.windowId == NULL) {
            System.err.println("--Window \"" + info.windowName + "\" can't be created (see the GLFW error above)");
            return;
        }

        System.out.println("\nWindow with id : " + info.windowId + " created");

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(info.windowId, pWidth, pHeight);

            // Center the window in the work area (without the task bar) of its screen, still hidden
            IntBuffer areaX = stack.mallocInt(1), areaY = stack.mallocInt(1);
            IntBuffer areaWidth = stack.mallocInt(1), areaHeight = stack.mallocInt(1);
            glfwGetMonitorWorkarea(monitorHandle, areaX, areaY, areaWidth, areaHeight);

            if (!info.fullscreen && parentWindowId != 0) {
                // Centered on its parent window
                IntBuffer parentX = stack.mallocInt(1), parentY = stack.mallocInt(1);
                IntBuffer parentWidth = stack.mallocInt(1), parentHeight = stack.mallocInt(1);
                glfwGetWindowPos(parentWindowId, parentX, parentY);
                glfwGetWindowSize(parentWindowId, parentWidth, parentHeight);

                glfwSetWindowPos(
                        info.windowId,
                        parentX.get(0) + (parentWidth.get(0) - pWidth.get(0)) / 2,
                        parentY.get(0) + (parentHeight.get(0) - pHeight.get(0)) / 2
                );
            } else if (!info.fullscreen) {
                glfwSetWindowPos(
                        info.windowId,
                        areaX.get(0) + Math.max(0, (areaWidth.get(0) - pWidth.get(0)) / 2),
                        areaY.get(0) + Math.max(0, (areaHeight.get(0) - pHeight.get(0)) / 2)
                );
            }
            // the stack frame is popped automatically
        }

        // Above its parent, not in the task bar
        if (parentWindowId != 0)
            NativeWindowOwner.set(info.windowId, parentWindowId);

        // Make the OpenGL context current, with the default state of the engine's contexts
        glContext = GLContext.ofWindow(info.windowName, info.windowId);

        // Make the window visible
        glfwShowWindow(info.windowId);

        setViewPort(info.size.x, info.size.y);
    }


    public boolean wantExit(){
        return glfwWindowShouldClose(info.windowId) && info.isWindowCreated();
    }


    // The OpenGL context of the window must be current; the events are polled once for all the windows (glfwPollEvents)
    public void swapBuffers(){
        glfwSwapBuffers(info.windowId);
    }

    public void swapBuffersAndPollEvents(){
        glfwSwapBuffers(info.windowId);
        glfwPollEvents();
    }


    public void setTitle(String title){
        if (info.isWindowCreated()){
            info.windowName = title;
            glfwSetWindowTitle(info.windowId, info.windowName);
        }
    }

    /**
     * Switch between fullscreen and window on the same window: its OpenGL context, its objects and its input
     * callbacks stay (recreating the window would lose them all).
     */
    public void setFullscreen(boolean fullscreen){
        if (info.fullscreen == fullscreen)
            return;

        info.fullscreen = fullscreen;
        if (!info.isWindowCreated())
            return;

        long monitorHandle = info.monitor != null ? info.monitor.getHandle() : glfwGetPrimaryMonitor();
        if (fullscreen) {
            glfwSetWindowMonitor(info.windowId, monitorHandle, 0, 0, info.size.x, info.size.y, GLFW_DONT_CARE);
        } else {
            // Back to a window centered in the work area of its screen
            try (MemoryStack stack = stackPush()) {
                IntBuffer areaX = stack.mallocInt(1), areaY = stack.mallocInt(1);
                IntBuffer areaWidth = stack.mallocInt(1), areaHeight = stack.mallocInt(1);
                glfwGetMonitorWorkarea(monitorHandle, areaX, areaY, areaWidth, areaHeight);

                glfwSetWindowMonitor(info.windowId, NULL,
                        areaX.get(0) + Math.max(0, (areaWidth.get(0) - info.size.x) / 2),
                        areaY.get(0) + Math.max(0, (areaHeight.get(0) - info.size.y) / 2),
                        info.size.x, info.size.y, GLFW_DONT_CARE);
            }
        }
    }

    public void setIcon(Icon icon){
        glfwSetWindowIcon(info.windowId, icon.getBuffer());
    }

    // The OpenGL context of this window, where the objects that are not shared (vertex arrays, framebuffers) live
    public GLContext getGLContext() { return glContext; }

    // Make the OpenGL context of this window the current one
    public void bindWindow() {
        glContext.makeCurrent();
    }


    /**
     * Destroy the current window and its OpenGL context.
     */
    public void destroyWindow(){
        Screenshot.cancelRequests(info.windowId);

        if (glContext != null) {
            glContext.destroy();
            glContext = null;
        }

        if (info.isWindowCreated()) {
            // Input callbacks (text, scroll) set on this window by the managers: their native part is freed with it
            Callbacks.glfwFreeCallbacks(info.windowId);
            glfwDestroyWindow(info.windowId);
            System.out.println("Window with id : " + info.windowId + " deleted");
            info.windowId = 0;
        }
    }


    public void setSize(Vector2f size){
        setSize(size.x, size.y);
    }
    public void setSize(float width, float height){
        setSize((int)width, (int)height);
    }
    public void setWidth(int width){
        setSize(width, info.size.y);
    }
    public void setHeight(int height){
        setSize(info.size.x, height);
    }


    public void setSize(int width, int height){
        info.size.x = width;
        info.size.y = height;
        info.ratio = (float) info.size.x / (float) info.size.y;
        if (info.isWindowCreated())
            glfwSetWindowSize(info.windowId, info.size.x, info.size.y);

    }


    public void setVirtualSize(int virtualWidth, int virtualHeight){
        info.virtualSize.x = virtualWidth;
        info.virtualSize.y = virtualHeight;
        info.virtualRatio = (float) info.virtualSize.x / (float) info.virtualSize.y;
    }

    public void setVirtualViewport(){
        setViewPort(info.virtualSize.x, info.virtualSize.y);
    }


    public void setRealViewport(){
        int x = 0, y = 0, width = info.size.x, height = info.size.y;

        if (!info.virtualSize.equals(info.size)){
            if (info.virtualRatio > info.ratio){
                float toMultiply = (float)info.size.x / (float)info.virtualSize.x;
                height = (int)(info.virtualSize.y * toMultiply);
                y = (info.size.y - height) / 2;
            } else {
                float toMultiply = (float)info.size.y / (float)info.virtualSize.y;
                width = (int)(info.virtualSize.x * toMultiply);
                x = (info.size.x - width) / 2;
            }
        }

        setViewPort(x, y, width, height);
    }

    /**
     * Set the 2D view.
     */
    public static void glEnable2D() {
        int[] vPort = new int[4];

        glGetIntegerv(GL_VIEWPORT, vPort);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        glOrtho(0, vPort[2], vPort[3], 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
    }

    /**
     * Set the 3D view.
     */
    public static void glDisable2D() {
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    private void setViewPort(int width, int height){
        setViewPort(0, 0, width, height);
    }

    private void setViewPort(int x, int y, int w, int h){
        glViewport(x, y, w, h);
    }
}
