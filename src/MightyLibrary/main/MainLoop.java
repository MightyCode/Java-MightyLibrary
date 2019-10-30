package MightyLibrary.main;

import MightyLibrary.scene.SceneManager;
import MightyLibrary.util.Timer;
import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.*;

public class MainLoop {
    private Window window;
    private SceneManager sceneManager;

    private final float SECOND = 1000000000.0f;
    public final float TPS = 60.0f;

    public final float FPS = 100000.0f;

    private final double TICK_TIME = SECOND / TPS;
    private final double FRAME_TIME = SECOND / FPS;

    public MainLoop(){
        if (initLibraries() == -1){
            exit(ListError.LIBRARIES_LOAD_FAIL);
        }

        window = new Window();

        // First instancing of manager container
        ManagerContainer manContainer = ManagerContainer.getInstance();
        manContainer.setManager(window);

        // Load or create config
        window.setSize(1280, 720);
        window.setVirtualSize(1280, 720);

        window.setTitle("Opengl test");
        window.setFullscreen(false);
        window.createNewWindow();

        sceneManager = new SceneManager(this);
    }

    public void run(){
        // Set render parameters
        int ticks = 0;
        int frames = 0;

        Timer timer = new Timer();

        double lastTick = 0.0;
        double lastFrame = 0.0;
        double lastSecond = 0.0;

        while(!window.wantExit()){
            if (timer.getDuration() - lastTick >= TICK_TIME) {
                sceneManager.update();
                sceneManager.dispose();
                ticks++;
                lastTick += TICK_TIME;
            } else if (timer.getDuration() - lastFrame >= FRAME_TIME) {
                sceneManager.display();
                window.dispose();
                frames++;
                lastFrame += FRAME_TIME;
            }

            if (timer.getDuration() - lastSecond >= SECOND) {
                if(Main.admin) window.setTitle("3D Project | FPS:" + frames + "; TPS:" + ticks);

                ticks = frames = 0;
                lastSecond += SECOND;
            }
        }

        sceneManager.unload();
        exit(ListError.NO_ERROR);
    }

    public void exit(int status){
        if (status != ListError.LIBRARIES_LOAD_FAIL){
            // Terminate GLFW and free the error callback
            glfwTerminate();
        }
        System.exit(status);
    }

    private int initLibraries() {
        // Setup an error callback.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            System.err.println("GLFW fail to initialize");
            return -1;
        }
        return 0;
    }
}
