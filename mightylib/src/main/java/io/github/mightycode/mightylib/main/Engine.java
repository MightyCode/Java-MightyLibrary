package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.console.CommandEcho;
import io.github.mightycode.mightylib.console.IConsoleService;
import io.github.mightycode.mightylib.console.ConsoleLog;
import io.github.mightycode.mightylib.console.LogCapture;
import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.shader.IShaderService;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.monitors.IMonitorSelector;
import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import io.github.mightycode.mightylib.main.monitors.MonitorSelectors;
import io.github.mightycode.mightylib.main.monitors.Monitors;
import io.github.mightycode.mightylib.inputs.InputRegistry;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.LifecycleHooks;
import io.github.mightycode.mightylib.registry.Registrations;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.IResourceSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.resources.texture.Icon;
import io.github.mightycode.mightylib.resources.texture.TextureUploader;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.ISceneManager;
import io.github.mightycode.mightylib.scenes.SceneRegistry;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.services.EngineServices;
import io.github.mightycode.mightylib.services.IEngineService;
import io.github.mightycode.mightylib.services.RunningService;
import io.github.mightycode.mightylib.services.ServiceKey;
import io.github.mightycode.mightylib.services.ServiceSet;
import io.github.mightycode.mightylib.sounds.ISoundService;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandOwner;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.DebugServer;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.apiguardian.api.API;
import org.joml.Vector2i;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apiguardian.api.API.Status.INTERNAL;
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
    // What the plugins registered (the engine first), frozen; null before setup
    private Registrations registrations;
    // The sources of the files of the resources, built once the registry is frozen; null before
    private ResourceSources resourceSources;
    private Resources resources;
    private GLResources glResources;
    private ISceneManager sceneManager;
    private Commands commands;
    // The services registered by the plugins, created by setup; null before
    private ServiceSet services;
    private DebugServer debugServer;
    // Program output and commands: null without console nor debug mode
    private ConsoleLog consoleLog;

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

    public Resources resources() { return resources; }
    // What the plugins registered, frozen (null before the start)
    public Registrations registrations() { return registrations; }
    // The actions and axes declared by the plugins (null before the start)
    public InputRegistry inputs() { return registrations != null ? registrations.inputs() : null; }
    // The hooks of the plugins on the life of the engine (none before the registration), called by the engine
    public LifecycleHooks lifecycle() { return registrations != null ? registrations.lifecycle() : NO_HOOKS; }
    private static final LifecycleHooks NO_HOOKS = new LifecycleHooks();
    // The scenes registered by the plugins, by name (an IllegalStateException before the start)
    public SceneRegistry scenes() {
        if (registrations == null)
            throw new IllegalStateException("No scene registry: the engine is not started");

        return registrations.scenes();
    }

    public GLResources glResources() { return glResources; }

    // The shaders: declarations and prelude of their sources (service mightylib:shaders)
    public IShaderService shaders() { return service(EngineServices.SHADERS); }

    // The sound (service mightylib:sound)
    public ISoundService sound() { return service(EngineServices.SOUND); }

    // The scenes of the main context
    public ISceneManager sceneManager() { return sceneManager; }

    // The commands of the engine, the game and the views, created by setup
    public Commands commands() { return commands; }

    // The in-game console (service mightylib:console), null without access to the commands
    // (EngineConfig.setCommandAccess)
    public IConsoleService console() {
        return hasService(EngineServices.CONSOLE) ? service(EngineServices.CONSOLE) : null;
    }

    // What the console shows: program output and commands of every source; null without console nor debug mode
    @API(status = INTERNAL)
    public ConsoleLog consoleLog() { return consoleLog; }

    /**
     * The service of this key, as its contract.
     * @throws IllegalArgumentException if no service has this key: test an optional one with hasService
     */
    public <T extends IEngineService> T service(ServiceKey<T> key) {
        if (services == null)
            throw new IllegalArgumentException("No service " + key + ": the engine is not started");
        return services.get(key);
    }

    public boolean hasService(ServiceKey<?> key) {
        return services != null && services.has(key);
    }

    // The services created, in their order of creation (empty before the start)
    public List<RunningService> services() {
        return services == null ? Collections.emptyList() : services.running();
    }

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
        Logger.setGLErrorCheck(config.isOpenGLErrorCheck());

        // Resolved once (the launch option is read and reported once). Without access, the player has no console:
        // only the game (api) and, in debug mode, the standard input and the debug server send commands.
        // A maximum of null closes the access for good: no console, the launch option is ignored
        ECommandLevel access = config.getCommandAccess();
        ECommandLevel maxAccess = config.getMaxCommandAccess();

        // From the first line: the console and the debug server clients see the whole output
        if (access != null || config.getMode() == EEngineMode.DEBUG) {
            consoleLog = new ConsoleLog(config.getConsoleLogCapacity());
            LogCapture.install(consoleLog);
        }

        System.out.println("--Start program.");

        // Before the window: a mistake of a plugin stops the engine before anything is opened
        System.out.println("--Register the plugins.");
        try {
            registrations = EngineRegistry.Collect(new EnginePlugin(config,
                    () -> hasService(EngineServices.SHADERS) ? shaders() : null, access != null),
                    config.getPlugins());
        } catch (RuntimeException e) {
            Logger.LogException("The registration of the plugins failed", e);
            return ListError.PLUGIN_REGISTRATION_FAIL;
        }
        // Before the shaders and the resources, which read their files in it
        try {
            resourceSources = registrations.resourceSources(new FolderSource(config.getResourcesFolder()));
        } catch (RuntimeException e) {
            Logger.LogException("The sources of the resources can't be stacked", e);
            return ListError.PLUGIN_REGISTRATION_FAIL;
        }
        contexts.setResourceSources(resourceSources);
        System.out.println("--Resource sources : " + resourceSources.sources().stream()
                .map(IResourceSource::name).collect(Collectors.joining(" < ")));
        contexts.setInputRegistry(registrations.inputs());
        contexts.setInputManagerFactory(registrations.inputManagerFactory());
        registrations.lifecycle().setStrict(config.isStrictResources());

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

        System.out.println("--Create Resources");
        resources = new Resources(config.getResourcesLoadingMethod(), Math.max(1, config.getMaxThreads()),
                resourceSources, registrations.loaders());
        resources.setStrict(config.isStrictResources());
        if (config.isStrictResources())
            System.out.println("--Strict resources: a resource that fails stops the program");

        if (config.getWatchResources() != null) {
            resources.watcher().configure(config.getWatchResources(), config.getWatchResourcesInterval());
            resources.watcher().watchAll();
        }

        // The batches send the GL elements of their data (textures) during the loading
        glResources = new GLResources(config.getMaxGLUploadMillisecondsPerFrame());
        resources.setGLElementSink(glResources);
        TextureUploader.setMode(config.getTextureUpload());

        // Before the scenes and the game: they add their commands to it
        if (maxAccess == null && System.getProperty(EngineConfig.COMMAND_ACCESS_PROPERTY) != null)
            System.out.println("--" + EngineConfig.COMMAND_ACCESS_PROPERTY + " ignored: this game has no command access");

        commands = new Commands(maxAccess == null ? ECommandLevel.USER : maxAccess,
                access == null ? ECommandLevel.USER : access);
        // Each plugin owns its commands (the engine its general ones), fixed for the whole run
        for (String plugin : registrations.plugins()) {
            CommandOwner owner = CommandOwner.Plugin(plugin);
            for (BaseCommand command : registrations.commands(plugin))
                commands.register(command, owner);
        }
        commands.freezePluginCommands();
        commands.setViewChooser(this::targetedView);
        if (consoleLog != null)
            commands.addListener(new CommandEcho(consoleLog));

        // After the resources and the commands, before the scenes: they can use the services
        System.out.println("--Create the services");
        services = new ServiceSet();
        if (config.isSoundForcedSilent()) {
            System.out.println("--" + EngineConfig.SOUND_PROPERTY + "=silent: no sound device");
            services.override(EngineServices.SOUND, EnginePlugin.SilentSound());
        }
        if (!services.create(this, registrations.services()))
            return ListError.SERVICE_FAIL;
        // Before the resources are loaded: the sounds keep their samples in the sound service
        resources.setSoundBuffers(sound());

        System.out.println("--Create the scene manager");
        sceneManager = registrations.sceneManagerFactory().create(this, mainContext, true, config.getLoadingScene());
        mainContext.setSceneManager(sceneManager);

        // Create all resources dependencies
        resources.init();

        // Load all resources
        resources.load();
        lifecycle().started();
        sceneManager.init(config.getStartScene().get(), new String[]{"start"});

        // The standard input and the debug server only exist in debug mode
        if (MainLoop.isDebug()) {
            commands.startConsole();

            int debugServerPort = config.getDebugServerPort();
            if (debugServerPort > 0) {
                debugServer = new DebugServer(commands, debugServerPort, consoleLog);
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

        ISceneManager manager = registrations.sceneManagerFactory().create(this, context, false,
                config.getLoadingScene());
        context.setSceneManager(manager);

        try (Views.Scope ignored = Views.enter(context)) {
            context.getMainWindow().getWindow().bindWindow();
            manager.init(startScene.get(), new String[]{"start"});
        }

        if (previous != null && !previous.isDestroyed())
            previous.makeCurrent();
        else
            contexts.requireMainWindow().getWindow().bindWindow();
        return context;
    }


    // ---- Window targeted by the commands ----

    // Null: the main window of the main context
    private WindowContext commandWindow;

    /** The window the debug commands (input, screenshot, monitors, scene, camera...) work on. */
    public WindowContext commandWindow() {
        // A closed window gives the target back to the main window
        if (commandWindow != null && !isOpen(commandWindow))
            commandWindow = null;

        return commandWindow != null ? commandWindow : contexts.getMainWindow();
    }

    public Context commandContext() {
        return commandWindow().getContext();
    }

    private boolean isOpen(WindowContext window) {
        return window.getContext().getWindows().contains(window) && contexts.getContexts().contains(window.getContext());
    }

    /**
     * The window the command being run works on: the window of the view whose code ran it directly
     * (Commands.execute from a scene), else the one chosen by the "window" command (console, stdin, debug server).
     * The commands read their target here, not in commandWindow().
     */
    public WindowContext targetWindow() {
        WindowContext caller = commands.getCallerWindow();
        return caller != null && isOpen(caller) ? caller : commandWindow();
    }

    public Context targetContext() {
        return targetWindow().getContext();
    }

    // Null to come back to the main window
    public void setCommandWindow(WindowContext window) {
        commandWindow = window;
    }

    /**
     * Among views having the same command (a scene shown in two windows), the one of the targeted window, else the
     * one of its context; null if none or several.
     */
    private IView targetedView(List<IView> views) {
        WindowContext target = targetWindow();
        IView inContext = null;
        int inContextCount = 0;
        for (IView view : views) {
            WindowContext window = view.context();
            if (window == target)
                return view;
            if (window != null && window.getContext() == target.getContext()) {
                inContext = view;
                ++inContextCount;
            }
        }

        return inContextCount == 1 ? inContext : null;
    }

    /**
     * A window by its number, its title (case-insensitive), or the name of a context (its main window); null if none.
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
        // Before the scenes: the console captures the inputs and runs what is typed in this tick
        services.update();

        for (Context context : contexts.getContexts())
            context.update();

        services.lateUpdate();
        contexts.postUpdate();

        for (Context context : contexts.getContexts())
            context.postUpdate();
    }

    // The frame is drawn and swapped, the events are read: a window can be drawn again from a callback
    boolean isPollingEvents() { return pollingEvents; }

    // The overlays of the services (the console on top) over the main window, once its scene is drawn
    void displayOverlays() {
        services.displayOverlays();
    }

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
        lifecycle().shutdown();

        if (debugServer != null) {
            debugServer.stop();
            debugServer = null;
        }

        // What every window shows, then the resources, while the OpenGL contexts exist
        contexts.unloadViews();

        WindowContext mainWindow = contexts.getMainWindow();
        if (mainWindow != null && mainWindow.getWindow().getInfo().isWindowCreated()) {
            mainWindow.getWindow().bindWindow();
            // The sources of the sound, the GL objects of the console
            if (services != null)
                services.unload();
        }

        if (resources != null)
            resources.unload();
        if (glResources != null)
            glResources.unload();
        // After the resources: their workers may still read the sources until their unload
        if (resourceSources != null)
            resourceSources.close();

        // Tasks still waiting can free GL objects: run them while the context exists
        MainThread.processAll();

        contexts.unload();

        Workers.stop();

        // Terminate GLFW and free the error callback
        if (glfwStarted) {
            glfwTerminate();
            // Freed just below (Callback.free is its close)
            //noinspection resource
            GLFWErrorCallback callback = glfwSetErrorCallback(null);
            if (callback != null)
                callback.free();
        }
        // The sound device, once everything is released
        if (services != null)
            services.terminate();

        // Strict mode: a hook that failed while stopping did not stop the release, it fails the exit
        if (status == ListError.NO_ERROR && lifecycle().hasTeardownFailed())
            status = ListError.LIFECYCLE_HOOK_FAIL;

        if (status != ListError.NO_ERROR)
            System.err.println("Exit with error " + status);
        else
            System.out.println("Exit without error");

        LogCapture.uninstall();

        return status;
    }
}
