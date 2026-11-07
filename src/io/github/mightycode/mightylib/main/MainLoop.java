package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.graphics.utils.Screenshot;
import io.github.mightycode.mightylib.main.monitors.IMonitorSelector;
import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import io.github.mightycode.mightylib.main.monitors.MonitorSelectors;
import io.github.mightycode.mightylib.main.monitors.Monitors;
import io.github.mightycode.mightylib.main.procedures.IProjectLoading;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.main.utils.FrameEvent;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.main.utils.TickEvent;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.Icon;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.sounds.SoundManager;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.enginecommand.DebugServer;
import org.lwjgl.Version;

import java.util.List;

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

    // Decide the screen of the window before its creation: system property, else the selector of the project
    private static MonitorInfo chooseMonitor(EngineConfig config) {
        List<MonitorInfo> monitors = Monitors.List();
        System.out.println("--Monitors :");
        for (MonitorInfo monitor : monitors)
            System.out.println("--  " + monitor);

        String property = System.getProperty(MonitorSelectors.PROPERTY);
        IMonitorSelector selector = property != null && !property.trim().isEmpty()
                ? MonitorSelectors.FromText(property) : config.getMonitorSelector();

        MonitorInfo chosen = selector == null ? null : selector.select(monitors);
        if (chosen == null)
            System.out.println("--Window on the primary monitor" + (property != null ? " (no monitor matches " + property + ")" : ""));
        else
            System.out.println("--Window on monitor " + chosen.getIndex() + " : " + chosen.getName());

        return chosen;
    }

    // Return ListError.NO_ERROR, or the error that prevents the engine from starting
    private static int setup(EngineConfig config) {
        config.validate();
        admin = config.isAdmin();
        Logger.DO_CHECK = config.isOpenGLErrorCheck();

        System.out.println("--Start program.");
        System.out.println("--Load libraries.");

        if (loadLibraries(config) == -1) {
            return ListError.LIBRARIES_LOAD_FAIL;
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
        wci.Monitor = chooseMonitor(config);
        wci.KeyboardLanguage = config.getKeyboardLanguage();

        contextManager.createDefaultContext(wci);

        Context context = contextManager.getContext("Main");
        Window window = context.getWindow();

        // Checked before any GL call: there is no GL context without a window
        if (!window.getInfo().isWindowCreated()) {
            return ListError.WINDOW_CREATION_FAIL;
        }

        System.out.println("\n" + Version.getVersion());
        System.out.println(glfwGetVersionString());
        System.out.println("GL VENDOR   : " + glGetString(GL_VENDOR));
        System.out.println("GL RENDERER : " + glGetString(GL_RENDERER));
        System.out.println("GL VERSION  : " + glGetString(GL_VERSION));
        System.out.println("GLSL VERSION :" + glGetString(GL_SHADING_LANGUAGE_VERSION));

        GenDeleteResources.Init();

        System.out.println("--Create ShaderManager");
        ShaderManager shaderManager = ShaderManager.getInstance();
        shaderManager.init(config.getShaderVersion());

        System.out.println("--Create Resources");
        Resources resource = Resources.createInstance(
                config.getResourcesLoadingMethod(),
                Math.max(1, config.getMaxThreads())
        );

        GLResources.createInstance(config.getMaxGLUploadMillisecondsPerFrame());

        System.out.println("--Create SceneManager");
        sceneManager = new SceneManager(config.getLoadingScene());

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

        return ListError.NO_ERROR;
    }

    /**
     * Run the engine until the window is closed or a scene asks to exit, then free everything.
     * The JVM is not stopped: the application decides what to do with the returned status.
     *
     * @return ListError.NO_ERROR, a ListError start failure, or the status given by the scene
     */
    @SuppressWarnings("BusyWait")
    public static int run(EngineConfig config) {
        int setupStatus = setup(config);
        if (setupStatus != ListError.NO_ERROR)
            return shutdown(setupStatus);

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
        GLResources glResources = GLResources.getInstance();

        while (!window.wantExit() && !sceneManager.isExitRequested()) {
            long now = System.nanoTime() - start;
            if (now - lastTick >= tickTime) {
                TickEvent tickEvent = new TickEvent();
                tickEvent.begin();
                long tickStart = System.nanoTime();
                GameTime.update();
                sceneManager.update();
                sceneManager.postUpdate();
                long tickWork = System.nanoTime() - tickStart;
                tickNanos += tickWork;
                EngineStats.endTick(tickWork);
                tickEvent.commit();
                ++ticks;

                while (now - lastTick >= tickTime)
                    lastTick += tickTime;
            } else if (now - lastFrame >= frameTime) {
                FrameEvent frameEvent = new FrameEvent();
                frameEvent.begin();
                long frameStart = System.nanoTime();
                sceneManager.display();

                // GL elements are sent with the time left in this frame, before the next tick or frame.
                // No tick is due now (the tick branch would have run), at least one step is done anyway.
                long nextTick = start + (long) (lastTick + tickTime);
                glResources.process(Math.min(nextTick, frameStart + (long) frameTime));

                // Measured before the buffer swap, which can wait for the GPU
                long frameWork = System.nanoTime() - frameStart;
                frameNanos += frameWork;
                EngineStats.endFrame(frameStart, frameWork);
                frameEvent.commit();

                Screenshot.captureIfRequested(window.getInfo().getWindowId());
                long swapStart = System.nanoTime();
                window.swapBuffersAndPollEvents();
                EngineStats.endSwap(System.nanoTime() - swapStart);
                ++frames;
                while (now - lastFrame >= frameTime)
                    lastFrame += frameTime;
            }

            if (now - lastSecond >= NANO_IN_SECOND) {
                EngineStats.endSecond(frames, ticks, frameNanos, tickNanos);

                if (MainLoop.admin) {
                    window.setTitle(String.format("%s | FPS:%d; TPS:%d | Frame: %.2f ms; Tick: %.2f ms",
                            config.getProjectName(), EngineStats.getFps(), EngineStats.getTps(),
                            EngineStats.getFrameMs(), EngineStats.getTickMs()));                }

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

        return shutdown(sceneManager.isExitRequested() ? sceneManager.getExitStatus() : ListError.NO_ERROR);
    }

    // Free everything created by setup, return the status
    private static int shutdown(int status) {
        if (debugServer != null) {
            debugServer.stop();
            debugServer = null;
        }

        // When the libraries failed to load, the sound manager may not exist
        boolean librariesLoaded = status != ListError.LIBRARIES_LOAD_FAIL;
        if (librariesLoaded)
            SoundManager.getInstance().unloadSoundSource();

        if (sceneManager != null) {
            sceneManager.unload();
            sceneManager = null;
        }

        if (contextManager != null) {
            contextManager.unload();
            contextManager = null;
        }

        // Terminate GLFW and free the error callback
        glfwTerminate();
        if (librariesLoaded)
            SoundManager.getInstance().unload();

        if (status != ListError.NO_ERROR)
            System.err.println("Exit with error " + status);
        else
            System.out.println("Exit without error");

        return status;
    }
}
