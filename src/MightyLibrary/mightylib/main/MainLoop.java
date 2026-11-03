package MightyLibrary.mightylib.main;

import MightyLibrary.mightylib.graphics.GLResources;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.graphics.utils.GenDeleteResources;
import MightyLibrary.mightylib.graphics.utils.Screenshot;
import MightyLibrary.mightylib.main.procedures.IProjectLoading;
import MightyLibrary.mightylib.main.utils.EngineStats;
import MightyLibrary.mightylib.main.utils.FrameEvent;
import MightyLibrary.mightylib.main.utils.GameTime;
import MightyLibrary.mightylib.main.utils.TickEvent;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.Icon;
import MightyLibrary.mightylib.scenes.SceneManager;
import MightyLibrary.mightylib.sounds.SoundManager;
import MightyLibrary.mightylib.utils.Logger;
import MightyLibrary.mightylib.utils.enginecommand.DebugServer;
import org.lwjgl.Version;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

public final class MainLoop {
    private final static float NANO_IN_SECOND = 1000000000.0f;
    private static ContextManager contextManager;
    private static SceneManager sceneManager;
    private static DebugServer debugServer;

    private static double tickTime;
    private static double frameTime;

    private static boolean admin = false;
    public static boolean isAdmin() { return admin; }


    private static int loadLibraries(EngineConfig config) {
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            System.err.println("GLFW fail to initialize");
            return -1;
        }

        SoundManager.initSoundManager(config.getGainTreePath());
        SoundManager soundManager = SoundManager.getInstance();
        if (!soundManager.init()) {
            System.err.println("SoundManager fail to initialize");
            return -1;
        }

        return 0;
    }

    private static void setup(EngineConfig config) {
        config.validate();
        admin = config.isAdmin();
        Logger.DO_CHECK = config.isOpenGLErrorCheck();

        System.out.println("--Start program.");
        System.out.println("--Load libraries.");

        if (loadLibraries(config) == -1) {
            exit(ListError.LIBRARIES_LOAD_FAIL);
        }

        tickTime = NANO_IN_SECOND / config.getTps();
        frameTime = NANO_IN_SECOND / config.getFps();
        GameTime.setFixedDeltaTime(1f / config.getTps());

        System.out.println("--Create main context.");
        contextManager = ContextManager.getInstance();

        ThreadManager.NumberMaxThreadToUse = config.getMaxThreads();

        WindowCreationInfo wci = new WindowCreationInfo();
        wci.Size = config.getWindowSize();
        wci.VirtualSize = config.getVirtualSize();

        wci.WindowName = config.getProjectName();
        wci.Fullscreen = config.isFullscreen();
        wci.KeyboardLanguage = config.getKeyboardLanguage();

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
        shaderManager.init(config.getShaderVersion());

        System.out.println("--Create Resources");
        Resources resource = Resources.createInstance(
                config.getResourcesLoadingMethod(),
                Math.max(1, config.getMaxThreads())
        );

        GLResources glResources = GLResources.createInstance(config.getGLResourceCreation());

        System.out.println("--Create SceneManager");
        sceneManager = new SceneManager(new StopLibrary());

        IProjectLoading projectLoading = config.getProjectLoading();
        if (projectLoading != null) {
            projectLoading.init(resource);
            projectLoading.contextLoading(context);
        }

        // Create all resources dependencies
        resource.init();

        // Load all resources
        resource.load();
        sceneManager.init(config.getStartScene().get(), new String[]{"start"});

        // Commands are only processed in admin state
        if (admin) {
            sceneManager.getCommands().startConsole();

            int debugServerPort = config.getDebugServerPort();
            if (debugServerPort > 0) {
                debugServer = new DebugServer(sceneManager.getCommands(), debugServerPort);
                if (!debugServer.start())
                    debugServer = null;
            }
        }

        if (config.getIconName() != null) {
            if (resource.isExistingResource(Icon.class, config.getIconName()))
                context.getWindow().setIcon(resource.getResource(Icon.class, config.getIconName()));
        }
    }

    @SuppressWarnings("BusyWait")
    public static void run(EngineConfig config) {
        setup(config);

        // Set loop parameters
        int ticks = 0;
        int frames = 0;

        // CPU time spent in update and display, to measure optimizations independently of the FPS cap
        long tickNanos = 0;
        long frameNanos = 0;

        double lastTick = 0.0;
        double lastFrame = 0.0;
        double lastSecond = 0.0;

        long start = System.nanoTime();

        Context mainContext = contextManager.getMainContext();

        Window window = mainContext.getWindow();

        while (!window.wantExit()) {
            long now = System.nanoTime() - start;
            if (now - lastTick >= tickTime) {
                TickEvent tickEvent = new TickEvent();
                tickEvent.begin();
                long tickStart = System.nanoTime();
                GameTime.update();
                sceneManager.update();
                sceneManager.postUpdate();
                tickNanos += System.nanoTime() - tickStart;
                tickEvent.commit();
                ++ticks;

                while (now - lastTick >= tickTime)
                    lastTick += tickTime;
            } else if (now - lastFrame >= frameTime) {
                FrameEvent frameEvent = new FrameEvent();
                frameEvent.begin();
                long frameStart = System.nanoTime();
                sceneManager.display();
                // Measured before the buffer swap, which can wait for the GPU
                frameNanos += System.nanoTime() - frameStart;
                frameEvent.commit();

                Screenshot.captureIfRequested(window.getInfo().getWindowId());
                window.swapBuffersAndPollEvents();
                ++frames;
                while (now - lastFrame >= frameTime)
                    lastFrame += frameTime;
            }

            if (now - lastSecond >= NANO_IN_SECOND) {
                EngineStats.endSecond(frames, ticks, frameNanos, tickNanos);

                if (MainLoop.admin) {
                    window.setTitle(String.format("%s | FPS:%d; TPS:%d | Frame: %.2f ms; Tick: %.2f ms",
                            config.getProjectName(), EngineStats.getFps(), EngineStats.getTps(),
                            EngineStats.getFrameMs(), EngineStats.getTickMs()));
                }

                ticks = frames = 0;
                tickNanos = frameNanos = 0;
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
        if (debugServer != null)
            debugServer.stop();

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
