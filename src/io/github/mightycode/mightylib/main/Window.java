package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.resources.texture.Icon;
import io.github.mightycode.mightylib.utils.Logger;
import org.joml.Vector2f;
import org.joml.Vector2i;
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

    // Only set when OpenGL error checks are enabled
    private Callback debugMessageCallback;

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
        info.fullscreen = wci.Fullscreen;
        info.monitor = wci.Monitor;
    }

    public void createNewWindow(){
        if (info.windowId != 0)
            destroyWindow();

        // Configure GLFW for this window
        glfwDefaultWindowHints(); // optional, the current window hints are already the default

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // Active AntiAliasing
        glfwWindowHint(GLFW_SAMPLES, 4);

        // A debug context lets the driver report OpenGL errors itself, with a readable message
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, Logger.DO_CHECK ? GLFW_TRUE : GLFW_FALSE);

        long monitorHandle = info.monitor != null ? info.monitor.getHandle() : glfwGetPrimaryMonitor();

        if (info.fullscreen)
            info.windowId = glfwCreateWindow(info.size.x, info.size.y, info.windowName, monitorHandle, NULL);
        else
            info.windowId = glfwCreateWindow(info.size.x, info.size.y, info.windowName, NULL, NULL);

        System.out.println("\nWindow with id : " + info.windowId + " created");

        if (info.windowId == NULL) {
            System.out.println("Window won't created");
        }

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

            if (!info.fullscreen) {
                glfwSetWindowPos(
                        info.windowId,
                        areaX.get(0) + Math.max(0, (areaWidth.get(0) - pWidth.get(0)) / 2),
                        areaY.get(0) + Math.max(0, (areaHeight.get(0) - pHeight.get(0)) / 2)
                );
            }
            // the stack frame is popped automatically
        } catch (Error e){
            info.windowId = 0;
        }

        /*new GLFWImage.Buffer();*/

        //glfwSetWindowIcon(info.windowId, );

        // Make the OpenGL context current
        bindWindow();

        // Make the window visible
        glfwShowWindow(info.windowId);
        GLCapabilities capabilities = GL.createCapabilities();
        if (Logger.DO_CHECK)
            setupDebugMessages(capabilities);

        glfwSwapInterval(0);

        setViewPort(info.size.x, info.size.y);

        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);

        // Enable antiAliasing
        glEnable(GL_MULTISAMPLE);

        glEnable(GL_PROGRAM_POINT_SIZE);

        wantExit();
    }


    public boolean wantExit(){
        return glfwWindowShouldClose(info.windowId) && info.isWindowCreated();
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

    public void setFullscreen(boolean fullscreen){
        if (info.isWindowCreated()) {
            if (info.fullscreen != fullscreen){
                destroyWindow();
            }
        }

        info.fullscreen = fullscreen;
        if (info.isWindowCreated()) createNewWindow();
    }

    public void setIcon(Icon icon){
        glfwSetWindowIcon(info.windowId, icon.getBuffer());
    }

    public void bindWindow() {
        glfwMakeContextCurrent(info.windowId);
    }


    /**
     * Destroy the current window.
     */
    private void setupDebugMessages(GLCapabilities capabilities) {
        debugMessageCallback = GLUtil.setupDebugMessageCallback(System.err);
        if (debugMessageCallback == null) {
            System.err.println("OpenGL debug messages are not supported by this driver");
            return;
        }

        // Notifications are very frequent and not errors: only keep low, medium and high severity messages
        if (capabilities.OpenGL43)
            GL43.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL43.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
        else if (capabilities.GL_KHR_debug)
            KHRDebug.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
    }

    public void destroyWindow(){
        if (debugMessageCallback != null) {
            debugMessageCallback.free();
            debugMessageCallback = null;
        }

        if (info.isWindowCreated()) {
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
