package MightyLibrary.mightylib.main;

import MightyLibrary.mightylib.graphics.GLResources;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.graphics.utils.GenDeleteResources;
import MightyLibrary.mightylib.inputs.keyboardlanguage.AZERTYKeyboardLanguage;
import MightyLibrary.mightylib.main.procedures.IProjectLoading;
import MightyLibrary.mightylib.main.procedures.IStartLibraryProcedure;
import MightyLibrary.mightylib.main.utils.GameTime;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.Icon;
import MightyLibrary.mightylib.scenes.SceneManager;
import MightyLibrary.mightylib.sounds.SoundManager;
import org.joml.Vector2i;
import org.lwjgl.Version;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

public final class MainLoop {
    private final static float NANO_IN_SECOND = 1000000000.0f;
    private static ContextManager contextManager;
    private static SceneManager sceneManager;

    private static double tickTime;
    private static double frameTime;

    private static boolean admin = false;
    public static boolean isAdmin() { return admin; }


    private static int loadLibraries(IStartLibraryProcedure startProcedure) {
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            System.err.println("GLFW fail to initialize");
            return -1;
        }

        SoundManager.initSoundManager(startProcedure.returnGainTreePath());
        SoundManager soundManager = SoundManager.getInstance();
        if (!soundManager.init()) {
            System.err.println("SoundManager fail to initialize");
            return -1;
        }

        return 0;
    }

    private static void setup(IStartLibraryProcedure startProcedure) {
        admin = startProcedure.returnAdminState();

        System.out.println("--Start program.");
        System.out.println("--Load libraries.");

        if (loadLibraries(startProcedure) == -1) {
            exit(ListError.LIBRARIES_LOAD_FAIL);
        }

        tickTime = NANO_IN_SECOND / startProcedure.returnTPS();
        frameTime = NANO_IN_SECOND / startProcedure.returnFPS();

        System.out.println("--Create main context.");
        contextManager = ContextManager.getInstance();

        ThreadManager.NumberMaxThreadToUse = startProcedure.returnMaxNumberOfThreadsToUse();

        WindowCreationInfo wci = new WindowCreationInfo();
        wci.Size = new Vector2i(
                startProcedure.returnSceneSize().x,
                startProcedure.returnSceneSize().y);

        wci.VirtualSize = new Vector2i(
                startProcedure.returnVirtualSceneSize().x,
                startProcedure.returnVirtualSceneSize().y);

        wci.WindowName = startProcedure.returnProjectName();
        wci.Fullscreen = startProcedure.returnFullscreenState();
        wci.KeyboardLanguage = startProcedure.returnDefaultKeyboardLanguage();

        contextManager.createDefaultContext(wci);

        System.out.println("\n" + Version.getVersion());
        System.out.println(glfwGetVersionString());
        System.out.println("GL VENDOR   : " + glGetString(GL_VENDOR));
        System.out.println("GL RENDERER : " + glGetString(GL_RENDERER));
        System.out.println("GL VERSION  : " + glGetString(GL_VERSION));
        System.out.println("GLSL VERSION :" + glGetString(GL_SHADING_LANGUAGE_VERSION));

        Context context = contextManager.getContext("Main");
        Window window = context.getWindow();

        if (!window.getInfo().isWindowCreated()) {
            exit(ListError.WINDOW_CREATION_FAIL);
        }

        GenDeleteResources.Init();

        System.out.println("--Create ShaderManager");
        ShaderManager shaderManager = ShaderManager.getInstance();
        shaderManager.init(startProcedure.returnShaderVersion());

        System.out.println("--Create Resources");
        Resources resource = Resources.createInstance(
                startProcedure.returnResourcesLoadingMethod(),
                Math.max(1, startProcedure.returnMaxNumberOfThreadsToUse())
        );

        GLResources glResources = GLResources.createInstance(startProcedure.returnGLResourceCreation());

        System.out.println("--Create SceneManager");
        sceneManager = new SceneManager(new StopLibrary());

        IProjectLoading projectLoading = startProcedure.returnIProjectLoading();
        projectLoading.init(resource);
        projectLoading.contextLoading(context);

        // Create all resources dependencies
        resource.init();

        // Load all resources
        resource.load();
        sceneManager.init(startProcedure.returnStartScene(), new String[]{"start"});

        if (startProcedure.returnIconName() != null) {
            if (resource.isExistingResource(Icon.class, startProcedure.returnIconName()))
                context.getWindow().setIcon(resource.getResource(Icon.class, startProcedure.returnIconName()));
        }
    }

    @SuppressWarnings("BusyWait")
    public static void run(IStartLibraryProcedure startLibraryProcedure) {
        setup(startLibraryProcedure);

        // Set loop parameters
        int ticks = 0;
        int frames = 0;

        double lastTick = 0.0;
        double lastFrame = 0.0;
        double lastSecond = 0.0;

        long start = System.nanoTime();

        Context mainContext = contextManager.getMainContext();
        mainContext.getKeyboardManager().setKeyboardLanguage(AZERTYKeyboardLanguage.getInstance());

        Window window = mainContext.getWindow();

        while (!window.wantExit()) {
            long now = System.nanoTime() - start;
            if (now - lastTick >= tickTime) {
                GameTime.update();
                sceneManager.update();
                sceneManager.dispose();
                ++ticks;

                while (now - lastTick >= tickTime)
                    lastTick += tickTime;
            } else if (now - lastFrame >= frameTime) {
                sceneManager.display();
                window.dispose();
                ++frames;
                while (now - lastFrame >= frameTime)
                    lastFrame += frameTime;
            }

            if (now - lastSecond >= NANO_IN_SECOND) {
                if (MainLoop.admin)
                    window.setTitle(startLibraryProcedure.returnProjectName() + " | FPS:" + frames + "; TPS:" + ticks);

                ticks = frames = 0;
                lastSecond += NANO_IN_SECOND;
            }

            double remaining = Math.min(tickTime - (now - lastTick), frameTime - (now - lastFrame)) / 1e6 * 0.7f;
            if (remaining > 1) {
                try {
                    Thread.sleep((long)remaining);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        exit(ListError.NO_ERROR);
    }

    public static class StopLibrary{
        public void exit(int status){
            MainLoop.exit(status);
        }
    }

    private static void exit(int status) {
        if (status != ListError.LIBRARIES_LOAD_FAIL) {
            // Terminate GLFW and free the error callback
            preUnload();
        }

        if (sceneManager != null)
            sceneManager.unload();

        if (contextManager != null)
            contextManager.unload();

        afterUnload();

        if (status != ListError.NO_ERROR) {
            System.err.println("Exit with error " + status);
            System.exit(status);
        } else {
            System.out.println("Exit without error");
            System.exit(0);
        }
    }

    private static void preUnload() {
        SoundManager.getInstance().unloadSoundSource();
    }

    private static void afterUnload() {
        glfwTerminate();
        SoundManager.getInstance().unload();
    }
}
