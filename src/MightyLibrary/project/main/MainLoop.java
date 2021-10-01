package MightyLibrary.project.main;

import MightyLibrary.mightylib.main.ListError;
import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.main.Window;
import MightyLibrary.mightylib.scene.SceneManager;
import MightyLibrary.mightylib.util.Timer;
import MightyLibrary.project.scenes.Test3DScene;

import static org.lwjgl.glfw.GLFW.*;

public class MainLoop {
    private Window window;
    private SceneManager sceneManager;

    private final float SECOND = 1000000000.0f;
    private final float TPS = 60.0f;

    private final float FPS = 100000.0f;

    private final double TICK_TIME = SECOND / TPS;
    private final double FRAME_TIME = SECOND / FPS;

    MainLoop(){
        if (loadLibraries() == -1){
            exit(ListError.LIBRARIES_LOAD_FAIL);
        }

        window = new Window();

        // First instancing of manager container
        ManagerContainer manContainer = ManagerContainer.getInstance();
        manContainer.setManager(window);

        // Load or create config
        window.setSize(1280, 720);
        window.setVirtualSize(1280, 720);

        window.setTitle("3D project test");
        window.setFullscreen(false);
        window.createNewWindow();

        if (!window.windowCreated){
            exit(ListError.WINDOW_CREATION_FAIL);
        }

        sceneManager = new SceneManager(this);
        sceneManager.setNewScene(new Test3DScene(), new String[]{""});

        ProjectLoading.Loading(manContainer);

        sceneManager.init();
    }

    public void run(){
        // Set loop parameters
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
                ++ticks;
                lastTick += TICK_TIME;
            } else if (timer.getDuration() - lastFrame >= FRAME_TIME) {
                sceneManager.display();
                window.dispose();
                ++frames;
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
            unloadLibraries();
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


    private int loadLibraries() {
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            System.err.println("GLFW fail to initialize");
            return -1;
        }

        return 0;
    }


    private void unloadLibraries(){
        glfwTerminate();
    }
}
