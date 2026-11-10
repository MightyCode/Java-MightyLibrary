package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.console.ConsoleLog;
import io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import io.github.mightycode.mightylib.inputs.keyboardlanguage.QWERTKeyboardLanguage;
import io.github.mightycode.mightylib.main.monitors.IMonitorSelector;
import io.github.mightycode.mightylib.main.monitors.MonitorSelectors;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.main.procedures.IProjectLoading;
import io.github.mightycode.mightylib.resources.ResourceWatcher;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.ETextureUpload;
import io.github.mightycode.mightylib.scenes.DefaultLoadingScene;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.TemplateSceneLoading;
import org.joml.Vector2i;

import java.util.function.Supplier;

/**
 * Startup configuration of the engine, filled by the project and given to MainLoop.run.
 * Only the start scene is required, every other value has a default.
 */
public class EngineConfig {
    public static final int DEFAULT_DEBUG_SERVER_PORT = 7777;

    private Supplier<Scene> startScene;
    private Supplier<TemplateSceneLoading> loadingScene = DefaultLoadingScene::new;

    // Display of the main window; its screen is the one of the monitor selector
    private DisplaySettings display = new DisplaySettings();
    // Null: the size of the window when it is created
    private Vector2i virtualSize;

    private String projectName = "MightyLibrary";
    private int fps = 60;
    private int tps = 60;
    private String iconName;
    private IMonitorSelector monitorSelector = MonitorSelectors.Primary();
    private KeyboardLanguage keyboardLanguage = QWERTKeyboardLanguage.getInstance();

    // Null: no gain tree, every sound keeps a gain of 1
    private String gainTreePath;

    private EEngineMode mode = EEngineMode.RELEASE;
    private boolean strictResources;
    private ResourceWatcher.Mode watchResources;
    private long watchResourcesInterval = ResourceWatcher.DEFAULT_INTERVAL_MS;
    private boolean openGLErrorCheck;
    private int debugServerPort = DEFAULT_DEBUG_SERVER_PORT;
    // Commands of the player: unset (null) gives DEV in debug mode, none in release
    private boolean commandAccessSet;
    private ECommandLevel commandAccess;
    // Unset: DEV in debug mode, USER in release; set to null: no access at all
    private boolean maxCommandAccessSet;
    private ECommandLevel maxCommandAccess;
    private boolean cheatCommand;
    private String consoleFont = "bahnschrift";
    private int consoleLogCapacity = ConsoleLog.DEFAULT_CAPACITY;

    private IProjectLoading projectLoading;

    private int shaderVersion = -1;
    private Resources.LoadingMethod resourcesLoadingMethod = new Resources.AllResourcesMethod();
    private int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    private int maxGLUploadMillisecondsPerFrame = -1;
    private float inGameLoadingShare = 0.3f;
    private ETextureUpload textureUpload = ETextureUpload.DIRECT;

    // Called once, when the engine is ready
    public EngineConfig setStartScene(Supplier<Scene> startScene) {
        this.startScene = startScene;
        return this;
    }

    // Displayed while a scene loads, unless the scene overrides Scene.createLoadingScene
    public EngineConfig setLoadingScene(Supplier<TemplateSceneLoading> loadingScene) {
        this.loadingScene = loadingScene;
        return this;
    }

    /** Mode and fine settings of the display of the main window (a copy is kept). */
    public EngineConfig setDisplay(DisplaySettings display) {
        this.display = display.copy();
        return this;
    }

    // Shortcut of DisplaySettings.setMode
    public EngineConfig setWindowMode(EWindowMode mode) {
        display.setMode(mode);
        return this;
    }

    // Shortcut of DisplaySettings.setWindowedSize: the size in WINDOWED mode
    public EngineConfig setWindowSize(int width, int height) {
        display.setWindowedSize(width, height);
        return this;
    }

    public EngineConfig setVirtualSize(int width, int height) {
        virtualSize = new Vector2i(width, height);
        return this;
    }

    public EngineConfig setProjectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    public EngineConfig setFps(int fps) {
        this.fps = fps;
        return this;
    }

    public EngineConfig setTps(int tps) {
        this.tps = tps;
        return this;
    }

    // Name of an Icon resource, null for none
    public EngineConfig setIconName(String iconName) {
        this.iconName = iconName;
        return this;
    }

    /**
     * Screen on which the window opens, decided before its creation (MonitorSelectors: Primary, Secondary, Index...).
     * It replaces the screen of the display settings.
     * The system property MonitorSelectors.PROPERTY (-Dmightylib.window.monitor=secondary) overrides it.
     */
    public EngineConfig setMonitorSelector(IMonitorSelector monitorSelector) {
        this.monitorSelector = monitorSelector;
        return this;
    }

    public EngineConfig setKeyboardLanguage(KeyboardLanguage keyboardLanguage) {
        this.keyboardLanguage = keyboardLanguage;
        return this;
    }

    public EngineConfig setGainTreePath(String gainTreePath) {
        this.gainTreePath = gainTreePath;
        return this;
    }

    // DEBUG enables the console, the commands and the debug server
    public EngineConfig setMode(EEngineMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Debug only: a resource that fails throws an exception instead of being replaced by its fallback,
     * so that no fallback goes unnoticed (tests, tools/scenario.py --strict).
     * The system property STRICT_RESOURCES_PROPERTY (-Dmightylib.resources.strict=true) overrides it.
     */
    public EngineConfig setStrictResources(boolean strictResources) {
        this.strictResources = strictResources;
        return this;
    }

    public static final String STRICT_RESOURCES_PROPERTY = "mightylib.resources.strict";

    /**
     * Hot reload of all the resources when their files change, from the start (null = off, the default; it can
     * still be started later by the code or the command watch). The system property WATCH_RESOURCES_PROPERTY
     * (-Dmightylib.resources.watch=poll|service) overrides it, WATCH_INTERVAL_PROPERTY the interval in ms.
     */
    public EngineConfig setWatchResources(ResourceWatcher.Mode watchMode) {
        this.watchResources = watchMode;
        return this;
    }

    public EngineConfig setWatchResourcesInterval(long milliseconds) {
        this.watchResourcesInterval = milliseconds;
        return this;
    }

    public static final String WATCH_RESOURCES_PROPERTY = "mightylib.resources.watch";
    public static final String WATCH_INTERVAL_PROPERTY = "mightylib.resources.watch.interval";
    // none, user, cheat or dev: the access to the commands at the start (see setCommandAccess)
    public static final String COMMAND_ACCESS_PROPERTY = "mightylib.commands.access";

    // Check OpenGL errors after each call (glGetError): helps to locate an error, but slows down rendering
    public EngineConfig setOpenGLErrorCheck(boolean openGLErrorCheck) {
        this.openGLErrorCheck = openGLErrorCheck;
        return this;
    }

    // Port of the local debug server receiving commands from other processes, -1 to disable it.
    // The server only starts in debug mode.
    public EngineConfig setDebugServerPort(int debugServerPort) {
        this.debugServerPort = debugServerPort;
        return this;
    }

    /**
     * Level of the player at the start, bounded by setMaxCommandAccess; null: no access, no in-game console.
     * By default DEV in debug mode, none in release: a released game opens the access itself (USER for a chat or
     * the options, CHEAT...). The console opens with ² (and F1 in DEV), F2 puts it in a window of its own.
     * The option -Dmightylib.commands.access=none|user|cheat|dev overrides it (a developer mode for the modders),
     * still bounded by the maximum.
     */
    public EngineConfig setCommandAccess(ECommandLevel level) {
        this.commandAccessSet = true;
        this.commandAccess = level;
        return this;
    }

    /**
     * Highest level of the build: the session never goes above it (Commands.setSessionLevel). By default DEV in debug
     * mode, USER in release: a game raises it to CHEAT for cheats, to DEV for a developer mode; a competitive game
     * keeps USER. Null: no access at all, whatever the access and the launch option (no console in the game).
     */
    public EngineConfig setMaxCommandAccess(ECommandLevel level) {
        this.maxCommandAccessSet = true;
        this.maxCommandAccess = level;
        return this;
    }

    /**
     * Add the command "cheats on|off" (level USER) letting the player switch between USER and CHEAT. Not added when
     * the maximum is below CHEAT. Off by default: a game can also call Commands.setSessionLevel from its own settings.
     */
    public EngineConfig setCheatCommand(boolean cheatCommand) {
        this.cheatCommand = cheatCommand;
        return this;
    }

    // Font of the console, among the fonts loaded at start (the fallback font otherwise)
    public EngineConfig setConsoleFont(String consoleFont) {
        this.consoleFont = consoleFont;
        return this;
    }

    // Lines kept by the console (program output and commands), the oldest are forgotten
    public EngineConfig setConsoleLogCapacity(int consoleLogCapacity) {
        this.consoleLogCapacity = consoleLogCapacity;
        return this;
    }

    public EngineConfig setProjectLoading(IProjectLoading projectLoading) {
        this.projectLoading = projectLoading;
        return this;
    }

    // -1 for the newest version
    public EngineConfig setShaderVersion(int shaderVersion) {
        this.shaderVersion = shaderVersion;
        return this;
    }

    public EngineConfig setResourcesLoadingMethod(Resources.LoadingMethod resourcesLoadingMethod) {
        this.resourcesLoadingMethod = resourcesLoadingMethod;
        return this;
    }

    public EngineConfig setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    // GL elements (textures...) are sent with the time left in each frame, at least one step per frame.
    // This caps that time, for instance to keep a margin while streaming in game. -1: no cap (default).
    public EngineConfig setMaxGLUploadMillisecondsPerFrame(int maxGLUploadMillisecondsPerFrame) {
        this.maxGLUploadMillisecondsPerFrame = maxGLUploadMillisecondsPerFrame;
        return this;
    }

    /**
     * Share of a frame (0 to 1) that the loading (resources, GL elements) may use while the scene is displayed.
     * Under a loading screen it uses all the time left in the frame. At least one step is done per frame either way.
     * Default 0.3: a scene that streams resources keeps most of its frame.
     */
    public EngineConfig setInGameLoadingShare(float inGameLoadingShare) {
        if (inGameLoadingShare < 0f || inGameLoadingShare > 1f)
            throw new IllegalArgumentException("EngineConfig: the loading share must be between 0 and 1");

        this.inGameLoadingShare = inGameLoadingShare;
        return this;
    }

    /**
     * How the big textures are sent to the card. PBO is faster or not depending on the card and the driver:
     * measure with the command "textureupload bench". The system property ETextureUpload.PROPERTY
     * (-Dmightylib.texture.upload=pbo) overrides it.
     */
    public EngineConfig setTextureUpload(ETextureUpload textureUpload) {
        this.textureUpload = textureUpload == null ? ETextureUpload.DIRECT : textureUpload;
        return this;
    }

    void validate() {
        if (startScene == null)
            throw new IllegalStateException("EngineConfig: no start scene, call setStartScene");
        if (loadingScene == null)
            throw new IllegalStateException("EngineConfig: no loading scene, keep the default one or call setLoadingScene");
        if (consoleLogCapacity < 1)
            throw new IllegalStateException("EngineConfig: the console log capacity must be positive");
        if (fps <= 0 || tps <= 0)
            throw new IllegalStateException("EngineConfig: fps and tps must be positive");
    }

    public Supplier<Scene> getStartScene() { return startScene; }
    public Supplier<TemplateSceneLoading> getLoadingScene() { return loadingScene; }
    // A copy
    public DisplaySettings getDisplay() { return display.copy(); }
    // Null when not given: the size of the window when it is created
    public Vector2i getVirtualSize() { return virtualSize != null ? new Vector2i(virtualSize) : null; }
    public String getProjectName() { return projectName; }
    public int getFps() { return fps; }
    public int getTps() { return tps; }
    public String getIconName() { return iconName; }
    public IMonitorSelector getMonitorSelector() { return monitorSelector; }
    public KeyboardLanguage getKeyboardLanguage() { return keyboardLanguage; }
    public String getGainTreePath() { return gainTreePath; }
    public EEngineMode getMode() { return mode; }

    public ETextureUpload getTextureUpload() {
        ETextureUpload fromProperty = ETextureUpload.parse(System.getProperty(ETextureUpload.PROPERTY));
        return fromProperty != null ? fromProperty : textureUpload;
    }

    // Strict only in debug mode, the system property wins over the configuration
    public boolean isStrictResources() {
        if (mode != EEngineMode.DEBUG)
            return false;

        String property = System.getProperty(STRICT_RESOURCES_PROPERTY);
        return property != null ? Boolean.parseBoolean(property) : strictResources;
    }
    // Null when the hot reload is not started with the engine
    public ResourceWatcher.Mode getWatchResources() {
        String property = System.getProperty(WATCH_RESOURCES_PROPERTY);
        if (property == null)
            return watchResources;

        switch (property.trim().toLowerCase()) {
            case "poll": case "polling": case "true":
                return ResourceWatcher.Mode.POLLING;
            case "service":
                return ResourceWatcher.Mode.SERVICE;
            default:
                return null;
        }
    }

    public long getWatchResourcesInterval() {
        String property = System.getProperty(WATCH_INTERVAL_PROPERTY);
        if (property != null) {
            try {
                return Long.parseLong(property.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return watchResourcesInterval;
    }
    public boolean isOpenGLErrorCheck() { return openGLErrorCheck; }
    public int getDebugServerPort() { return debugServerPort; }
    // Null: no access at all (setMaxCommandAccess(null)), the launch option is ignored
    public ECommandLevel getMaxCommandAccess() {
        if (maxCommandAccessSet)
            return maxCommandAccess;

        return mode == EEngineMode.DEBUG ? ECommandLevel.DEV : ECommandLevel.USER;
    }

    // Level of the player at the start, bounded by the maximum; null: no access (no console)
    public ECommandLevel getCommandAccess() {
        if (getMaxCommandAccess() == null)
            return null;

        ECommandLevel access;
        String property = System.getProperty(COMMAND_ACCESS_PROPERTY);
        if (property != null && property.trim().equalsIgnoreCase("none")) {
            access = null;
        } else if (property != null) {
            try {
                access = ECommandLevel.Parse(property);
            } catch (IllegalArgumentException e) {
                System.err.println("--" + COMMAND_ACCESS_PROPERTY + " : " + e.getMessage());
                access = defaultCommandAccess();
            }
        } else {
            access = defaultCommandAccess();
        }

        return access == null ? null : ECommandLevel.Min(access, getMaxCommandAccess());
    }

    private ECommandLevel defaultCommandAccess() {
        if (commandAccessSet)
            return commandAccess;

        return mode == EEngineMode.DEBUG ? ECommandLevel.DEV : null;
    }

    // The in-game console exists when the player has an access to the commands
    public boolean isConsoleEnabled() { return getCommandAccess() != null; }

    public boolean isCheatCommand() { return cheatCommand; }
    public String getConsoleFont() { return consoleFont; }
    public int getConsoleLogCapacity() { return consoleLogCapacity; }
    public IProjectLoading getProjectLoading() { return projectLoading; }
    public int getShaderVersion() { return shaderVersion; }
    public Resources.LoadingMethod getResourcesLoadingMethod() { return resourcesLoadingMethod; }
    public int getMaxThreads() { return maxThreads; }
    public int getMaxGLUploadMillisecondsPerFrame() { return maxGLUploadMillisecondsPerFrame; }
    public float getInGameLoadingShare() { return inGameLoadingShare; }
}
