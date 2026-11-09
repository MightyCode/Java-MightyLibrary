package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.GLContext;
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
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.sounds.SoundManager;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.enginecommand.DebugServer;
import org.joml.Vector2i;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.util.List;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

/**
 * The services shared by everything the engine shows: resources, GL elements, shaders, sounds, and the contexts
 * (a scene and its windows). One per process (GLFW and the sound device are), created and driven by MainLoop.
 * Code that runs for a view reaches it by Views.engine(); the engine is itself the view of the code that runs
 * outside any scene (start, commands, loading), with the main window.
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
    // A window that stops the engine was closed
    private boolean stopRequested;
    // Inside glfwPollEvents, see isPollingEvents
    private boolean pollingEvents;

    Engine(EngineConfig config) {
        this.config = config;
    }

    public EngineConfig config() { return config; }

    public ContextManager contexts() { return contexts; }

    public ShaderManager shaderManager() { return shaderManager; }

    public Resources resources() { return resources; }

    public GLResources glResources() { return glResources; }

    public SoundManager soundManager() { return soundManager; }

    // The scenes of the main context
    public SceneManager sceneManager() { return sceneManager; }

    // ---- IView: the code outside any scene runs for the main window, without cameras ----

    @Override
    public Engine engine() { return this; }

    @Override
    public WindowContext context() { return contexts.getMainWindow(); }

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
        // GLFW errors (window or context creation...) are written with the program errors
        GLFWErrorCallback.createPrint(System.err).set();

        WindowPlatform.applyInitHint();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            System.err.println("GLFW fail to initialize");
            return -1;
        }

        glfwStarted = true;
        WindowPlatform.readPlatform();
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

        WindowConfig windowConfig = new WindowConfig(config.getProjectName())
                .setDisplay(config.getDisplay())
                .setMonitor(chooseMonitor())
                .setKeyboardLanguage(config.getKeyboardLanguage());
        // Null: the size of the window when it is created
        Vector2i virtualSize = config.getVirtualSize();
        if (virtualSize != null)
            windowConfig.setVirtualSize(virtualSize.x, virtualSize.y);

        // Checked before any GL call: there is no GL context without a window
        Context mainContext = contexts.createMainContext(this, windowConfig);
        if (mainContext == null)
            return ListError.WINDOW_CREATION_FAIL;

        WindowContext mainWindow = mainContext.getMainWindow();

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
        sceneManager = new SceneManager(this, mainContext, true, config.getLoadingScene());
        mainContext.setSceneManager(sceneManager);

        IProjectLoading projectLoading = config.getProjectLoading();
        if (projectLoading != null) {
            projectLoading.init(resources);
            projectLoading.contextLoading(mainWindow);
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
                mainWindow.getWindow().setIcon(icon);
        }

        return ListError.NO_ERROR;
    }

    // ---- Contexts and windows ----

    /**
     * Open another context while the engine runs: a window with its own scene, scene changes and loading screen,
     * sharing the resources. Closing its main window (or its scene asking to exit) closes only this context.
     * The scene is created with the new context as its view.
     *
     * @throws IllegalStateException when the window can't be created
     */
    public Context openContext(String name, WindowConfig windowConfig, Supplier<Scene> startScene) {
        if (sceneManager == null)
            throw new IllegalStateException("The engine is not started");

        GLContext previous = GLContext.currentOrNull();

        Context context = contexts.createContext(this, name, false, windowConfig);
        if (context == null)
            throw new IllegalStateException("The window of the context " + name + " can't be created");

        initWindowInputs(context.getMainWindow(), windowConfig);

        SceneManager manager = new SceneManager(this, context, false, config.getLoadingScene());
        context.setSceneManager(manager);

        try (Views.Scope ignored = Views.enter(context)) {
            context.getMainWindow().getWindow().bindWindow();
            manager.init(startScene.get(), new String[]{"start"});
        }

        if (previous != null && !previous.isDestroyed())
            previous.makeCurrent();
        else
            contexts.getMainWindow().getWindow().bindWindow();
        return context;
    }

    // The inputs of the project (IProjectLoading.contextLoading) are bound to each window that asks for them
    void initWindowInputs(WindowContext window, WindowConfig windowConfig) {
        IProjectLoading projectLoading = config.getProjectLoading();
        if (projectLoading != null && windowConfig.hasProjectInputs())
            projectLoading.contextLoading(window);
    }


    // ---- Window targeted by the commands ----

    // Null: the main window of the main context
    private WindowContext commandWindow;

    /** The window the debug commands (input, screenshot, monitors, scene, camera...) work on. */
    public WindowContext commandWindow() {
        // A closed window gives the target back to the main window
        if (commandWindow != null && !commandWindow.getContext().getWindows().contains(commandWindow)
                || commandWindow != null && !contexts.getContexts().contains(commandWindow.getContext()))
            commandWindow = null;

        return commandWindow != null ? commandWindow : contexts.getMainWindow();
    }

    public Context commandContext() {
        return commandWindow().getContext();
    }

    // Null to come back to the main window
    public void setCommandWindow(WindowContext window) {
        commandWindow = window;
    }

    /**
     * A window by its number, its title (case insensitive), or the name of a context (its main window); null if none.
     */
    public WindowContext findWindow(String reference) {
        try {
            int id = Integer.parseInt(reference);
            for (Context context : contexts.getContexts()) {
                for (WindowContext window : context.getWindows()) {
                    if (window.getId() == id)
                        return window;
                }
            }

            return null;
        } catch (NumberFormatException ignored) {
            // Not a number: a title or a context
        }

        for (Context context : contexts.getContexts()) {
            for (WindowContext window : context.getWindows()) {
                if (window.getName().equalsIgnoreCase(reference))
                    return window;
            }
        }

        Context context = contexts.getContext(reference);
        return context == null ? null : context.getMainWindow();
    }

    // ---- Run by the loop ----

    boolean isStopRequested() { return stopRequested; }

    // A scene of any context is loading: the loading queues use the whole frame
    boolean isAnySceneLoading() {
        for (Context context : contexts.getContexts()) {
            if (context.getSceneManager() != null && context.getSceneManager().isCurrentSceneLoading())
                return true;
        }

        return false;
    }

    // One tick of every context, then the inputs of every window
    void update() {
        for (Context context : contexts.getContexts())
            context.update();

        soundManager.lateUpdate();
        contexts.postUpdate();

        for (Context context : contexts.getContexts())
            context.postUpdate();
    }

    // The frame is drawn and swapped, the events are read: a window can be drawn again from a callback
    boolean isPollingEvents() { return pollingEvents; }

    // Each window shows its view
    void displayWindows() {
        for (Context context : contexts.getContexts())
            context.displayWindows();
    }

    // The buffers of all the windows are swapped, then the events of all of them are read
    void swapWindows() {
        for (Context context : contexts.getContexts())
            context.swapWindows();

        pollingEvents = true;
        try {
            glfwPollEvents();
        } finally {
            pollingEvents = false;
        }

        if (contexts.processClosures())
            stopRequested = true;
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

        // What every window shows, then the resources, while the OpenGL contexts exist
        contexts.unloadViews();

        WindowContext mainWindow = contexts.getMainWindow();
        if (mainWindow != null && mainWindow.getWindow().getInfo().isWindowCreated())
            mainWindow.getWindow().bindWindow();

        if (resources != null)
            resources.unload();
        if (glResources != null)
            glResources.unload();

        // Tasks still waiting can free GL objects: run them while the context exists
        MainThread.processAll();

        contexts.unload();

        Workers.stop();

        // Terminate GLFW and free the error callback
        if (glfwStarted) {
            glfwTerminate();
            GLFWErrorCallback callback = glfwSetErrorCallback(null);
            if (callback != null)
                callback.free();
        }
        if (soundManager != null)
            soundManager.unload();

        if (status != ListError.NO_ERROR)
            System.err.println("Exit with error " + status);
        else
            System.out.println("Exit without error");

        return status;
    }
}
