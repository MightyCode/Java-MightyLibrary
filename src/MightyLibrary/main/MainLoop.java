package MightyLibrary.main;

import MightyLibrary.scene.SceneManager;
import MightyLibrary.util.Timer;

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
        window.setSize(1920, 1080);
        window.setVirtualSize(1920, 1080);

        window.setTitle("Opengl test");
        window.setFullscreen(true);
        window.createNewWindow();

        if (!window.windowCreated){
            exit(ListError.WINDOW_CREATION_FAIL);
        }

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

        if (status != ListError.WINDOW_CREATION_FAIL) {
            window.destroyWindow();
        }

        if (status != ListError.NO_ERROR){
            System.err.println("Exit with error "  + status);
            System.exit(-1);
        } else {
            System.out.println("Exit without error");
            System.exit(0);
        }
    }

    private int initLibraries() {
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            System.err.println("GLFW fail to initialize");
            return -1;
        }
        return 0;
    }
}
