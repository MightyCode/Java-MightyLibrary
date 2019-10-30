package MightyLibrary.main;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryStack;


import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Class of the game's main structure.
 *
 * @author MightyCode
 * @version of game : 2.2
 */
public class Window{

    private boolean windowCreated;

    public long windowId;
    public Vector2i size;
    public float ratio;
    public Vector2i virtualSize;

    public String windowName;

    public boolean fullscreen;


    public Window(){
        windowCreated = false;
        windowId = 0;
        size = new Vector2i(1, 1);
        virtualSize = new Vector2i(1,1);
        windowName = "";
        fullscreen = false;
    }

    public void createNewWindow(){
        // Configure GLFW for this window
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Active AntiAliasing
        glfwWindowHint(GLFW_SAMPLES, 4);

        if (fullscreen) windowId = glfwCreateWindow(size.x, size.y, windowName, glfwGetPrimaryMonitor(), NULL);
        else            windowId = glfwCreateWindow(size.x, size.y, windowName, NULL, NULL);


        System.out.println("\nWindow with id : "+ windowId +" created");

        if (windowId == NULL) {
            System.out.println("Window won't created");
        }

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(windowId, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    windowId,
                    (Objects.requireNonNull(vidmode).width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(windowId);

        // Make the window visible
        glfwShowWindow(windowId);
        createCapabilities();
        glfwSwapInterval(0);

        setViewPort(size.x, size.y);

        glEnable(GL_TEXTURE_2D);
        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);

        // Enable antiAliasing
        glEnable(GL_MULTISAMPLE);
        windowCreated = true;
    }


    public boolean wantExit(){
        return glfwWindowShouldClose(windowId) && windowCreated;
    }


    public void dispose(){
        glfwSwapBuffers(windowId);
        glfwPollEvents();
    }


    public void setTitle(String title){
        if (windowCreated){
            windowName = title;
            glfwSetWindowTitle(windowId, windowName);
        }

    }

    public void setFullscreen(boolean fullscreen){
        if (windowCreated) {
            if (this.fullscreen != fullscreen){
                destroyWindow();

            }
        }

        this.fullscreen = fullscreen;
        if (windowCreated) createNewWindow();
    }

    /**
     * Destroy the current window.
     */
    public void destroyWindow(){
        if (windowCreated) {
            // Free the window callbacks and destroy the window
            glfwFreeCallbacks(windowId);
            glfwDestroyWindow(windowId);
            System.out.println("Window with id : " + windowId + " deleted");
            windowId = 0;
        }
    }


    public Window setSize(Vector2f size){
        return setSize(size.x, size.y);
    }


    public Window setSize(float width, float height){
        return setSize((int)width, (int)height);
    }


    public Window setWidth(int width){
        return setSize(width, size.y);
    }


    public Window setHeight(int height){
        return setSize(size.x, height);
    }


    public Window setSize(int width, int height){
        size.x = width;
        size.y = height;
        this.ratio = (float) this.size.x / (float) this.size.y;
        if (windowCreated)
            glfwSetWindowSize(windowId, size.x, size.y);
        return this;
    }


    public Window setVirtualSize(int virtualWidth, int virtualHeight){
        virtualSize.x = virtualWidth;
        virtualSize.y = virtualHeight;
        return this;
    }

    public Window setVirtualViewport(){
        setViewPort(virtualSize.x, virtualSize.y);
        return this;
    }


    public Window setRealViewport(){
        setViewPort(size.x, size.y);
        return this;
    }


    public Window forceRatio(float ratio){
        this.ratio = ratio;
        return this;
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
        glViewport(0, 0, width, height);
    }
}
