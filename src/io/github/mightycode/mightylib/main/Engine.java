package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.monitors.IMonitorSelector;
import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import io.github.mightycode.mightylib.main.monitors.MonitorSelectors;
import io.github.mightycode.mightylib.main.monitors.Monitors;
import io.github.mightycode.mightylib.main.procedures.IProjectLoading;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.Icon;
import io.github.mightycode.mightylib.resources.texture.TextureUploader;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.sounds.SoundManager;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.enginecommand.DebugServer;
import org.lwjgl.Version;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

/**
 * The services shared by everything the engine shows: resources, GL elements, shaders, sounds, windows and scenes.
 * One per process (GLFW and the sound device are), created and driven by MainLoop. Code that runs for a view
 * reaches it by Views.engine(); the engine is itself the view of the code that runs outside any scene
 * (start, commands, loading), with the main window.
 */
public final class Engine implements IView {
    private final EngineConfig config;

    private final ContextManager contexts = new ContextManager();
    private ShaderManager shaderManager;
    private Resources resources;
    private GLResources glResources;
    private SoundManager soundManager;
    private SceneManager sceneManager;
    private DebugServer debugServer;

    private boolean glfwStarted;

    Engine(EngineConfig config) {
        this.config = config;
    }

    public EngineConfig config() { return config; }

    public ContextManager contexts() { return contexts; }

    public ShaderManager shaderManager() { return shaderManager; }

    public Resources resources() { return resources; }

    public GLResources glResources() { return glResources; }

    public SoundManager soundManager() { return soundManager; }

    public SceneManager sceneManager() { return sceneManager; }

    // ---- IView: the code outside any scene runs for the main window, without cameras ----

    @Override
    public Engine engine() { return this; }

    @Override
    public Context context() { return contexts.getMainContext(); }

    @Override
    public Camera2D camera2D() {
        throw new IllegalStateException("The engine has no camera: create it from a scene");
    }

    @Override
    public Camera3D camera3D() {
        throw new IllegalStateException("The engine has no camera: create it from a scene");
    }

    // ---- Start ----

    private int loadLibraries() {
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            System.err.println("GLFW fail to initialize");
            return -1;
        }

        glfwStarted = true;
        return 0;
    }

    // Decide the screen of the window before its creation: system property, else the selector of the project
    private MonitorInfo chooseMonitor() {
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
    int setup() {
        config.validate();
        MainThread.setCurrentAsMain();
        Workers.start(config.getMaxThreads());
        MainLoop.setMode(config.getMode());
        Logger.DO_CHECK = config.isOpenGLErrorCheck();

        System.out.println("--Start program.");
        System.out.println("--Load libraries.");

        if (loadLibraries() == -1)
            return ListError.LIBRARIES_LOAD_FAIL;

        GameTime.setFixedDeltaTime(1f / config.getTps());

        System.out.println("--Create main context.");

        WindowCreationInfo wci = new WindowCreationInfo();
        wci.Size = config.getWindowSize();
        wci.VirtualSize = config.getVirtualSize();

        wci.WindowName = config.getProjectName();
        wci.Fullscreen = config.isFullscreen();
        wci.Monitor = chooseMonitor();
        wci.KeyboardLanguage = config.getKeyboardLanguage();

        contexts.createDefaultContext(wci);

        Context context = contexts.getContext("Main");
        Window window = context.getWindow();

        // Checked before any GL call: there is no GL context without a window
        if (!window.getInfo().isWindowCreated())
            return ListError.WINDOW_CREATION_FAIL;

        System.out.println("\n" + Version.getVersion());
        System.out.println(glfwGetVersionString());
        System.out.println("GL VENDOR   : " + glGetString(GL_VENDOR));
        System.out.println("GL RENDERER : " + glGetString(GL_RENDERER));
        System.out.println("GL VERSION  : " + glGetString(GL_VERSION));
        System.out.println("GLSL VERSION :" + glGetString(GL_SHADING_LANGUAGE_VERSION));

        GenDeleteResources.Init();

        System.out.println("--Create ShaderManager");
        shaderManager = new ShaderManager();
        shaderManager.init(config.getShaderVersion());

        System.out.println("--Create Resources");
        resources = new Resources(config.getResourcesLoadingMethod(), Math.max(1, config.getMaxThreads()), shaderManager);
        resources.setStrict(config.isStrictResources());
        if (config.isStrictResources())
            System.out.println("--Strict resources: a resource that fails stops the program");

        if (config.getWatchResources() != null) {
            resources.watcher().configure(config.getWatchResources(), config.getWatchResourcesInterval());
            resources.watcher().watchAll();
        }

        // The batches send the GL elements of their data (textures) during the loading
        glResources = new GLResources(resources, config.getMaxGLUploadMillisecondsPerFrame());
        resources.setGLElementSink(glResources);
        TextureUploader.setMode(config.getTextureUpload());

        soundManager = new SoundManager(resources, config.getGainTreePath());
        if (!soundManager.init()) {
            System.err.println("SoundManager fail to initialize");
            return ListError.LIBRARIES_LOAD_FAIL;
        }

        System.out.println("--Create SceneManager");
        sceneManager = new SceneManager(this, config.getLoadingScene());

        IProjectLoading projectLoading = config.getProjectLoading();
        if (projectLoading != null) {
            projectLoading.init(resources);
            projectLoading.contextLoading(context);
        }

        // Create all resources dependencies
        resources.init();

        // Load all resources
        resources.load();
        sceneManager.init(config.getStartScene().get(), new String[]{"start"});

        // Commands are only processed in debug mode
        if (MainLoop.isDebug()) {
            sceneManager.getCommands().startConsole();

            int debugServerPort = config.getDebugServerPort();
            if (debugServerPort > 0) {
                debugServer = new DebugServer(sceneManager.getCommands(), debugServerPort);
                if (!debugServer.start())
                    debugServer = null;
            }
        }

        if (config.getIconName() != null) {
            // An icon that failed to load (already reported) keeps the default one
            Icon icon = resources.isExistingResource(Icon.class, config.getIconName())
                    ? resources.getResource(Icon.class, config.getIconName()) : null;
            if (icon != null && icon.isLoaded())
                context.getWindow().setIcon(icon);
        }

        return ListError.NO_ERROR;
    }

    // ---- Stop ----

    // Free everything created by setup (whatever stage it reached), return the status
    int shutdown(int status) {
        if (debugServer != null) {
            debugServer.stop();
            debugServer = null;
        }

        if (soundManager != null)
            soundManager.unloadSoundSource();

        if (sceneManager != null) {
            sceneManager.unload();
            sceneManager = null;
        } else {
            // The start failed before the scenes
            if (resources != null)
                resources.unload();
            if (glResources != null)
                glResources.unload();
        }

        // Tasks still waiting can free GL objects: run them while the context exists
        MainThread.processAll();

        contexts.unload();

        Workers.stop();

        // Terminate GLFW and free the error callback
        if (glfwStarted)
            glfwTerminate();
        if (soundManager != null)
            soundManager.unload();

        if (status != ListError.NO_ERROR)
            System.err.println("Exit with error " + status);
        else
            System.out.println("Exit without error");

        return status;
    }
}
