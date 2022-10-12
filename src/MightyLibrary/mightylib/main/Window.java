package MightyLibrary.mightylib.main;

import MightyLibrary.mightylib.graphics.texture.Icon;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;


import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
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
public class Window {
    private final WindowInfo info;

    public WindowInfo getInfo() { return info; }

    Window(){
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

        info.size = new Vector2i(wci.size);
        info.ratio = (float) info.size.x / (float) info.size.y;
        info.virtualSize = new Vector2i(wci.virtualSize);
        info.virtualRatio = (float) info.virtualSize.x / (float) info.virtualSize.y;

        info.windowName = wci.windowName;
        info.fullscreen = wci.fullscreen;
    }

    public void createNewWindow(){
        if (info.windowId != 0) destroyWindow();

        // Configure GLFW for this window
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Active AntiAliasing
        glfwWindowHint(GLFW_SAMPLES, 4);

        if (info.fullscreen) info.windowId = glfwCreateWindow(info.size.x, info.size.y, info.windowName, glfwGetPrimaryMonitor(), NULL);
        else                 info.windowId = glfwCreateWindow(info.size.x, info.size.y, info.windowName, NULL, NULL);


        System.out.println("\nWindow with id : "+ info.windowId +" created");

        if (info.windowId == NULL) {
            System.out.println("Window won't created");
        }

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(info.windowId, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    info.windowId,
                    (Objects.requireNonNull(videoMode).width() - pWidth.get(0)) / 2,
                    (videoMode.height() - pHeight.get(0)) / 2
            );
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
        GL.createCapabilities();
        glfwSwapInterval(0);

        setViewPort(info.size.x, info.size.y);

        glEnable(GL_TEXTURE_2D);
        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);

        // Enable antiAliasing
        glEnable(GL_MULTISAMPLE);

        setIcon(new Icon("icon", "resources/icon.png"));
    }


    public boolean wantExit(){
        return glfwWindowShouldClose(info.windowId) && info.isWindowCreated();
    }


    public void dispose(){
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
        GLFWImage.Buffer buffer = GLFWImage.create(1);
        GLFWImage iconGI = GLFWImage.create().set(icon.width, icon.height, icon.Buffer);
        buffer.put(0, iconGI);

        glfwSetWindowIcon(info.windowId, buffer);
    }

    public void bindWindow() {
        glfwMakeContextCurrent(info.windowId);
    }


    /**
     * Destroy the current window.
     */
    public void destroyWindow(){
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
