package MightyLibrary.main;

import MightyLibrary.scene.SceneManager;
import MightyLibrary.util.Timer;
import org.lwjgl.glfw.GLFWErrorCallback;
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
@SuppressWarnings("InfiniteLoopStatement")
public class Window{

    private WindowParams wParams;
    private final float SECOND = 1000000000.0f;
    public final float TPS = 60.0f;

    public final float FPS = 100000.0f;

    private final double TICK_TIME = SECOND / TPS;
    private final double FRAME_TIME = SECOND / FPS;

    public Window(){
        // Get the game global configurations.
        wParams = new WindowParams();
        ManagerContainer manContainer = ManagerContainer.getInstance();
        manContainer.setManager(wParams);
        createWindow();
    }
    private void createWindow() {
        // Setup an error callback.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        createNewWindow();
    }

    public void createNewWindow(){
        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        // Active AntiAliasing
        glfwWindowHint(GLFW_SAMPLES, 4);

        wParams.setSize(1280, 720);

        wParams.windowId = glfwCreateWindow(wParams.size.x, wParams.size.y, "Opengl test", NULL, NULL);
        System.out.println(wParams.size.x + " " + wParams.size.y);

        System.out.println("\nWindow with id : "+ wParams.windowId +" created");

        if (wParams.windowId == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(wParams.windowId, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    wParams.windowId,
                    (Objects.requireNonNull(vidmode).width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(wParams.windowId);

        // Make the window visible
        glfwShowWindow(wParams.windowId);
        createCapabilities();
        glfwSwapInterval(0);

        setViewPort(wParams.size.x, wParams.size.y);
        //setViewPort(1920, 1920);

        glEnable(GL_TEXTURE_2D);
        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);

        // Enable antiAliasing
        glEnable(GL_MULTISAMPLE);
    }

    /**
     * Destroy the current window.
     */
    public void destroyWindow(){
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(wParams.windowId);
        glfwDestroyWindow(wParams.windowId);
        System.out.println("Window with id : " + wParams.windowId + " deleted");
    }

    /**
     * Method call by main class to begin the game.
     */
    void run() {
        loop();
        exit();
    }

    /**
     * Main method of game.
     */
    private void loop() {
        // Set render parameters
        int ticks = 0;
        int frames = 0;

        Timer timer = new Timer();

        double lastTick = 0.0;
        double lastFrame = 0.0;
        double lastSecond = 0.0;

        SceneManager screenManager = new SceneManager(this);

        while(!glfwWindowShouldClose(wParams.windowId)){
            if (timer.getDuration() - lastTick >= TICK_TIME) {
                screenManager.update();
                screenManager.dispose();
                ticks++;
                lastTick += TICK_TIME;
            } else if (timer.getDuration() - lastFrame >= FRAME_TIME) {
                screenManager.display();
                glfwSwapBuffers(wParams.windowId);
                glfwPollEvents();
                frames++;
                lastFrame += FRAME_TIME;
            }

            if (timer.getDuration() - lastSecond >= SECOND) {
                if(Main.admin) glfwSetWindowTitle(wParams.windowId, "OPEN GL | FPS:" + frames + "; TPS:" + ticks);
                //System.out.println(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
                ticks = frames = 0;
                lastSecond += SECOND;
            }
        }

        screenManager.unload();
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

    public static void setViewPort(int width, int height){
        glViewport(0, 0, width, height);
    }

    /**
     * Exit the game.
     */
    public void exit() {

        destroyWindow();
        // Terminate GLFW and free the error callback
        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
        System.exit(0);
    }
}
