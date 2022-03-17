package MightyLibrary.project.main;

import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.main.*;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.scene.SceneManager;
import MightyLibrary.mightylib.sounds.SoundManager;
import MightyLibrary.project.scenes.MenuScene;
import org.joml.Vector2i;
import org.lwjgl.Version;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

public class MainLoop {
    private final ContextManager contextManager;
    private final SceneManager sceneManager;

    private final static float NANO_IN_SECOND = 1000000000.0f;
    private final static float TPS = 60.0f;

    private final static float FPS = 100000.0f;

    private final static double TICK_TIME = NANO_IN_SECOND / TPS;
    private final static double FRAME_TIME = NANO_IN_SECOND / FPS;

    MainLoop(){
        System.out.println("--Start program. ");
        System.out.println("--Load libraries.");

        if (loadLibraries() == -1){
            exit(ListError.LIBRARIES_LOAD_FAIL);
        }

        System.out.println("--Create main context.");
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

        System.out.println("--Create ShaderManager");
        ShaderManager shaderManager = ShaderManager.getInstance();
        //shaderManager.forceShaderVersion(140);
        System.out.println("--Create Resources");
        Resources resource = Resources.getInstance();

        System.out.println("--Create SceneManager");
        sceneManager = new SceneManager(this);

        ProjectLoading.init();
        ProjectLoading.ContextLoading(context);

        resource.init();
        sceneManager.init(new MenuScene(), new String[]{""});

        System.out.println("\n" + Version.getVersion());
        System.out.println(glfwGetVersionString());
        System.out.println("GL VENDOR   : " + glGetString(GL_VENDOR));
        System.out.println("GL RENDERER : " + glGetString(GL_RENDERER));
        System.out.println("GL VERSION  : " + glGetString(GL_VERSION));
        System.out.println("GLSL VERSION :" + glGetString(GL_SHADING_LANGUAGE_VERSION));
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

            if (System.nanoTime() - start - lastSecond >= NANO_IN_SECOND) {
                if (Main.admin) window.setTitle("3D Project | FPS:" + frames + "; TPS:" + ticks);

                ticks = frames = 0;
                lastSecond += NANO_IN_SECOND;
            }
        }

        exit(ListError.NO_ERROR);
    }


    public void exit(int status){
        if (status != ListError.LIBRARIES_LOAD_FAIL){
            // Terminate GLFW and free the error callback
            unloadLibraries();
        }

        if (sceneManager != null)
            sceneManager.unload();

        if (contextManager != null)
            contextManager.unload();

        if (status != ListError.NO_ERROR){
            System.err.println("Exit with error "  + status);
            System.exit(status);
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

        SoundManager soundManager = SoundManager.getInstance();
        if (!soundManager.init()){
            System.err.println("SoundManager fail to initialize");
            return -1;
        }

        return 0;
    }


    private void unloadLibraries(){
        glfwTerminate();

        SoundManager.getInstance().unload();
    }
}
