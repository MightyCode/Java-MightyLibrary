package MightyLibrary.project.main;

import MightyLibrary.mightylib.main.*;
import MightyLibrary.mightylib.scene.SceneManager;
import MightyLibrary.project.scenes.Test2DScene;
import MightyLibrary.project.scenes.Test3DScene;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public class MainLoop {
    ContextManager contextManager;
    private final SceneManager sceneManager;

    private final float SECOND = 1000000000.0f;
    private final float TPS = 60.0f;

    private final float FPS = 100000.0f;

    private final double TICK_TIME = SECOND / TPS;
    private final double FRAME_TIME = SECOND / FPS;

    MainLoop(){
        if (loadLibraries() == -1){
            exit(ListError.LIBRARIES_LOAD_FAIL);
        }

        contextManager = ContextManager.getInstance();

        WindowCreationInfo wci = new WindowCreationInfo();
        wci.size = new Vector2i(1280, 720);
        wci.virtualSize = new Vector2i(1280, 720);
        wci.windowName = "3D project test";
        wci.fullscreen = false;

        contextManager.createDefaultContext(wci);

        Context context = contextManager.getContext("Main");
        Window window = context.getWindow();


        if (!window.getInfo().isWindowCreated()){
            exit(ListError.WINDOW_CREATION_FAIL);
        }

        sceneManager = new SceneManager(this, new Test3DScene(), new String[]{""});

        ProjectLoading.ContextLoading(context);

        sceneManager.init();
    }

    void run(){
        // Set loop parameters
        int ticks = 0;
        int frames = 0;

        double lastTick = 0.0;
        double lastFrame = 0.0;
        double lastSecond = 0.0;

        long start = System.nanoTime();

        Context mainContext = contextManager.getMainContext();
        Window window = mainContext.getWindow();

        while (!window.wantExit()){
            if (System.nanoTime() - start - lastTick >= TICK_TIME) {
                GameTime.update();
                sceneManager.update();
                sceneManager.dispose();
                ++ticks;
                lastTick += TICK_TIME;
            } else if (System.nanoTime() - start - lastFrame >= FRAME_TIME) {
                sceneManager.display();
                window.dispose();
                ++frames;
                lastFrame += FRAME_TIME;
            }

            if (System.nanoTime() - start - lastSecond >= SECOND) {
                if (Main.admin) window.setTitle("3D Project | FPS:" + frames + "; TPS:" + ticks);

                ticks = frames = 0;
                lastSecond += SECOND;
            }
        }

        exit(ListError.NO_ERROR);
    }


    public void exit(int status){
        if (status != ListError.LIBRARIES_LOAD_FAIL){
            // Terminate GLFW and free the error callback
            unloadLibraries();
        }

        sceneManager.unload();
        contextManager.unload();

        if (status != ListError.NO_ERROR){
            System.err.println("Exit with error "  + status);
            System.exit(status);
        } else {
            sceneManager.unload();

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
