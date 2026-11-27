package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.console.internal.ConsoleLog;
import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import io.github.mightycode.mightylib.inputs.keyboardlanguage.QWERTKeyboardLanguage;
import io.github.mightycode.mightylib.main.monitors.IMonitorSelector;
import io.github.mightycode.mightylib.main.monitors.MonitorSelectors;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.ResourceWatcher;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.ETextureUpload;
import io.github.mightycode.mightylib.scenes.DefaultLoadingScene;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.TemplateSceneLoading;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Startup configuration of the engine, filled by the project and given to MainLoop.run.
 * Only the start scene is required, every other value has a default.
 */
public final class EngineConfig {
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
    private ERenderColorSpace colorSpace = ERenderColorSpace.GAMMA;
    private EBatch2DMode batch2DMode = EBatch2DMode.ORPHAN;
    private boolean debugDraw = true;
    private boolean viewCulling2D = true;
    private int debugServerPort = DEFAULT_DEBUG_SERVER_PORT;
    // Commands of the player: unset (null) gives DEV in debug mode, none in release
    private boolean commandAccessSet;
    private ECommandLevel commandAccess;
    // Unset: DEV in debug mode, USER in release; set to null: no access at all
    private boolean maxCommandAccessSet;
    private ECommandLevel maxCommandAccess;
    private boolean cheatCommand;
    private String defaultFont = EngineResources.DEFAULT_FONT;
    private String consoleFont = EngineResources.CONSOLE_FONT;
    private int consoleLogCapacity = ConsoleLog.DEFAULT_CAPACITY;
    private boolean consoleLogsShown = true;
    private boolean consoleDataShown;

    private final List<IPlugin> plugins = new ArrayList<>();

    private Resources.LoadingMethod resourcesLoadingMethod = new Resources.AllResourcesMethod();
    private String resourcesFolder = Resources.DEFAULT_FOLDER;
    private String playerDataFolder = DEFAULT_PLAYER_DATA_FOLDER;
    // Null: the classpath source of the engine
    private String engineResourcesFolder;
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

    // The folder of the data of the player by default, relative to the working folder
    public static final String DEFAULT_PLAYER_DATA_FOLDER = "data/";

    /**
     * The folder of the data of the player (DEFAULT_PLAYER_DATA_FOLDER by default, relative to the working folder):
     * its "configs" folder is the highest source of the resources, where ConfigData writes what the player changes.
     */
    public EngineConfig setPlayerDataFolder(String playerDataFolder) {
        this.playerDataFolder = Objects.requireNonNull(playerDataFolder, "playerDataFolder");
        return this;
    }

    public String getPlayerDataFolder() {
        return playerDataFolder;
    }

    /**
     * The color space of the draws at the start (IColorService, then changed by the code or the commands): GAMMA by
     * default (the 2D of most games), LINEAR for a lit scene; a view or a renderer overrides it. The system property
     * COLOR_SPACE_PROPERTY (-Dmightylib.colorSpace=linear|gamma) overrides it.
     *
     * @throws IllegalArgumentException if the mode is INHERIT or null
     */
    public EngineConfig setColorSpace(ERenderColorSpace colorSpace) {
        this.colorSpace = ERenderColorSpace.CheckEngine(colorSpace);
        return this;
    }

    public static final String COLOR_SPACE_PROPERTY = "mightylib.colorSpace";

    /**
     * How the 2D draws are sent at the start (then changed by the command batch mode): ORPHAN by default, OFF draws
     * each renderer alone. The system property BATCH_2D_PROPERTY (-Dmightylib.batch2D=off|orphan|ring|instanced)
     * overrides it.
     */
    public EngineConfig setBatch2DMode(EBatch2DMode batch2DMode) {
        this.batch2DMode = Objects.requireNonNull(batch2DMode, "batch2DMode");
        return this;
    }

    public static final String BATCH_2D_PROPERTY = "mightylib.batch2D";

    /**
     * The batched 2D draws wholly outside the view are culled before they are written (true by default, in release
     * too), then changed by Engine.setViewCulling2D or the command batch cull; a renderer opts out with
     * Renderer.setViewCulling(false). The system property VIEW_CULLING_2D_PROPERTY (-Dmightylib.viewCulling2D=on|off)
     * overrides it.
     */
    public EngineConfig setViewCulling2D(boolean viewCulling2D) {
        this.viewCulling2D = viewCulling2D;
        return this;
    }

    public static final String VIEW_CULLING_2D_PROPERTY = "mightylib.viewCulling2D";

    /**
     * The debug drawings (IView.debugDraw) recorded and drawn at the start (true by default, in release too), then
     * changed by the command debugdraw. The system property DEBUG_DRAW_PROPERTY (-Dmightylib.debugDraw=on|off)
     * overrides it.
     */
    public EngineConfig setDebugDraw(boolean debugDraw) {
        this.debugDraw = debugDraw;
        return this;
    }

    public static final String DEBUG_DRAW_PROPERTY = "mightylib.debugDraw";

    /**
     * Development: a folder on the disk replaces the classpath source of the engine (its files in the jar, under
     * "assets/mightylib/"), so that they can be edited and reloaded from the repository (ex.
     * "mightylib/src/main/resources/assets/mightylib"). Null (the default): the classpath. The system property
     * ENGINE_RESOURCES_PROPERTY (-Dmightylib.resources.engine=folder) overrides it.
     */
    public EngineConfig setEngineResourcesFolder(String engineResourcesFolder) {
        this.engineResourcesFolder = engineResourcesFolder;
        return this;
    }

    public static final String ENGINE_RESOURCES_PROPERTY = "mightylib.resources.engine";

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
    // shown or hidden: the program output in the console at the start (see setConsoleLogsShown)
    public static final String CONSOLE_LOGS_PROPERTY = "mightylib.console.logs";
    // shown or hidden: the data of the command results in the console at the start (see setConsoleDataShown)
    public static final String CONSOLE_DATA_PROPERTY = "mightylib.console.data";
    // silent: the silent sound service, whatever plugin provides the sound (see isSoundForcedSilent)
    public static final String SOUND_PROPERTY = "mightylib.sound";

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
     * By default, DEV in debug mode, none in release: a released game opens the access itself (USER for a chat or
     * the options, CHEAT...). The console opens with ², F2 puts it in a window of its own.
     * The option -Dmightylib.commands.access=none|user|cheat|dev overrides it (a developer mode for the modders),
     * still bounded by the maximum.
     */
    public EngineConfig setCommandAccess(ECommandLevel level) {
        this.commandAccessSet = true;
        this.commandAccess = level;
        return this;
    }

    /**
     * Highest level of the build: the session never goes above it (Commands.setSessionLevel). By default, DEV in debug
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

    // Font of a Text that has no setFont, among the fonts loaded at start (the fallback font otherwise)
    public EngineConfig setDefaultFont(String defaultFont) {
        this.defaultFont = defaultFont;
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

    /**
     * The program output (stdout, stderr) shown in the console at the start (true by default; seen in DEV only), the
     * command "console logs on|off" changes it. The option -Dmightylib.console.logs=shown|hidden overrides it.
     */
    public EngineConfig setConsoleLogsShown(boolean consoleLogsShown) {
        this.consoleLogsShown = consoleLogsShown;
        return this;
    }

    /**
     * The data of the command results (JSON) shown in the console at the start (false by default; seen in DEV only),
     * the command "console data on|off" changes it. The option -Dmightylib.console.data=shown|hidden overrides it.
     */
    public EngineConfig setConsoleDataShown(boolean consoleDataShown) {
        this.consoleDataShown = consoleDataShown;
        return this;
    }

    /**
     * What the game (or a mod) adds to the engine: loaders, commands... registered at the start, in the order of the
     * calls, after the content of the engine. One plugin per namespace.
     */
    public EngineConfig addPlugin(IPlugin plugin) {
        plugins.add(Objects.requireNonNull(plugin, "plugin"));
        return this;
    }

    public EngineConfig setResourcesLoadingMethod(Resources.LoadingMethod resourcesLoadingMethod) {
        this.resourcesLoadingMethod = resourcesLoadingMethod;
        return this;
    }

    /**
     * Folder of the files of the game, relative to the working directory ("resources/" by default): a source of
     * resources above the engine's, below those of the plugins (EngineRegistry.resources().source).
     */
    public EngineConfig setResourcesFolder(String resourcesFolder) {
        this.resourcesFolder = Objects.requireNonNull(resourcesFolder, "resourcesFolder");
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

    // Without window (headless engine), there are no scenes to start
    void validate(boolean withScenes) {
        if (withScenes && startScene == null)
            throw new IllegalStateException("EngineConfig: no start scene, call setStartScene");
        if (withScenes && loadingScene == null)
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

    // Null: the classpath source of the engine; the system property wins over the configuration
    public String getEngineResourcesFolder() {
        String property = System.getProperty(ENGINE_RESOURCES_PROPERTY);
        return property != null && !property.isBlank() ? property.trim() : engineResourcesFolder;
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

        switch (property.trim().toLowerCase(Locale.ROOT)) {
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
            } catch (NumberFormatException e) {
                Logger.Warn(Logger.CATEGORY_CONFIG, WATCH_INTERVAL_PROPERTY + " : not a number ->" + property + "<-, the configured interval is kept");
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
                Logger.Warn(Logger.CATEGORY_CONFIG, COMMAND_ACCESS_PROPERTY + " : " + e.getMessage());
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

    // The in-game console exists when the player has access to the commands
    public boolean isConsoleEnabled() { return getCommandAccess() != null; }

    public boolean isCheatCommand() { return cheatCommand; }
    public String getDefaultFont() { return defaultFont; }
    public String getConsoleFont() { return consoleFont; }
    public int getConsoleLogCapacity() { return consoleLogCapacity; }

    // The launch option wins; an unknown value keeps the configuration, with a line in the log
    public boolean isConsoleLogsShown() {
        return ShownOption(CONSOLE_LOGS_PROPERTY, consoleLogsShown);
    }

    // The launch option wins; an unknown value keeps the configuration, with a line in the log
    public boolean isConsoleDataShown() {
        return ShownOption(CONSOLE_DATA_PROPERTY, consoleDataShown);
    }

    // A launch option shown|hidden, or the configured value when it is absent or unknown (with a line in the log)
    private static boolean ShownOption(String name, boolean configured) {
        String property = System.getProperty(name);
        if (property == null)
            return configured;

        Optional<Boolean> shown = ParseShown(property);
        if (shown.isEmpty()) {
            Logger.Warn(Logger.CATEGORY_CONFIG, name + " : unknown value ->" + property + "<-, expected shown or hidden");
            return configured;
        }

        return shown.get();
    }

    // shown: true, hidden: false, anything else: empty
    static Optional<Boolean> ParseShown(String value) {
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "shown":
                return Optional.of(true);
            case "hidden":
                return Optional.of(false);
            default:
                return Optional.empty();
        }
    }

    // The property wins over the configuration; an unknown value is ignored, with a line in the log
    public ERenderColorSpace getColorSpace() {
        String property = System.getProperty(COLOR_SPACE_PROPERTY);
        if (property == null)
            return colorSpace;

        ERenderColorSpace value = ERenderColorSpace.parse(property);
        if (value == ERenderColorSpace.LINEAR || value == ERenderColorSpace.GAMMA)
            return value;

        Logger.Warn(Logger.CATEGORY_CONFIG, COLOR_SPACE_PROPERTY + " : unknown value ->" + property + "<-, expected linear or gamma");
        return colorSpace;
    }

    // The property wins over the configuration; an unknown value is ignored, with a line in the log
    public EBatch2DMode getBatch2DMode() {
        String property = System.getProperty(BATCH_2D_PROPERTY);
        if (property == null)
            return batch2DMode;

        EBatch2DMode value = EBatch2DMode.parse(property);
        if (value != null)
            return value;

        Logger.Warn(Logger.CATEGORY_CONFIG, BATCH_2D_PROPERTY + " : unknown value ->" + property
                + "<-, expected off, orphan, ring or instanced");
        return batch2DMode;
    }

    // The property wins over the configuration; an unknown value is ignored, with a line in the log
    public boolean isViewCulling2D() {
        String property = System.getProperty(VIEW_CULLING_2D_PROPERTY);
        if (property == null)
            return viewCulling2D;

        switch (property.trim().toLowerCase(Locale.ROOT)) {
            case "on":
                return true;
            case "off":
                return false;
            default:
                Logger.Warn(Logger.CATEGORY_CONFIG, VIEW_CULLING_2D_PROPERTY + " : unknown value ->" + property
                        + "<-, expected on or off");
                return viewCulling2D;
        }
    }

    // The property wins over the configuration; an unknown value is ignored, with a line in the log
    public boolean isDebugDraw() {
        String property = System.getProperty(DEBUG_DRAW_PROPERTY);
        if (property == null)
            return debugDraw;

        switch (property.trim().toLowerCase(Locale.ROOT)) {
            case "on":
                return true;
            case "off":
                return false;
            default:
                Logger.Warn(Logger.CATEGORY_CONFIG, DEBUG_DRAW_PROPERTY + " : unknown value ->" + property
                        + "<-, expected on or off");
                return debugDraw;
        }
    }

    // Launch option -Dmightylib.sound=silent: no device is opened; another value is ignored, with a line in the log
    public boolean isSoundForcedSilent() {
        String property = System.getProperty(SOUND_PROPERTY);
        if (property == null)
            return false;

        if (property.trim().equalsIgnoreCase("silent"))
            return true;

        Logger.Warn(Logger.CATEGORY_CONFIG, SOUND_PROPERTY + " : unknown value ->" + property + "<-, expected silent");
        return false;
    }

    // In the order of addPlugin, read only
    public List<IPlugin> getPlugins() { return Collections.unmodifiableList(plugins); }
    public Resources.LoadingMethod getResourcesLoadingMethod() { return resourcesLoadingMethod; }
    public String getResourcesFolder() { return resourcesFolder; }
    public int getMaxThreads() { return maxThreads; }
    public int getMaxGLUploadMillisecondsPerFrame() { return maxGLUploadMillisecondsPerFrame; }
    public float getInGameLoadingShare() { return inGameLoadingShare; }
}
