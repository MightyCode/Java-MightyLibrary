package MightyLibrary.mightylib.main;

import MightyLibrary.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import MightyLibrary.mightylib.inputs.keyboardlanguage.QWERTKeyboardLanguage;
import MightyLibrary.mightylib.main.monitors.IMonitorSelector;
import MightyLibrary.mightylib.main.monitors.MonitorSelectors;
import MightyLibrary.mightylib.main.procedures.IProjectLoading;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.scenes.DefaultLoadingScene;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.scenes.TemplateSceneLoading;
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

    private final Vector2i windowSize = new Vector2i(1280, 720);
    // Null: same as the window size
    private Vector2i virtualSize;

    private String projectName = "MightyLibrary";
    private int fps = 60;
    private int tps = 60;
    private String iconName;
    private boolean fullscreen;
    private IMonitorSelector monitorSelector = MonitorSelectors.Primary();
    private KeyboardLanguage keyboardLanguage = QWERTKeyboardLanguage.getInstance();

    // Null: no gain tree, every sound keeps a gain of 1
    private String gainTreePath;

    private boolean admin;
    private boolean openGLErrorCheck;
    private int debugServerPort = DEFAULT_DEBUG_SERVER_PORT;

    private IProjectLoading projectLoading;

    private int shaderVersion = -1;
    private Resources.LoadingMethod resourcesLoadingMethod = new Resources.AllResourcesMethod();
    private int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    private int maxGLUploadMillisecondsPerFrame = -1;

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

    public EngineConfig setWindowSize(int width, int height) {
        windowSize.set(width, height);
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

    public EngineConfig setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        return this;
    }

    /**
     * Screen on which the window opens, decided before its creation (MonitorSelectors: Primary, Secondary, Index...).
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

    // Enables the console, the commands and the debug server
    public EngineConfig setAdmin(boolean admin) {
        this.admin = admin;
        return this;
    }

    // Check OpenGL errors after each call (glGetError): helps to locate an error, but slows down rendering
    public EngineConfig setOpenGLErrorCheck(boolean openGLErrorCheck) {
        this.openGLErrorCheck = openGLErrorCheck;
        return this;
    }

    // Port of the local debug server receiving commands from other processes, -1 to disable it.
    // The server only starts in admin state.
    public EngineConfig setDebugServerPort(int debugServerPort) {
        this.debugServerPort = debugServerPort;
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

    void validate() {
        if (startScene == null)
            throw new IllegalStateException("EngineConfig: no start scene, call setStartScene");
        if (loadingScene == null)
            throw new IllegalStateException("EngineConfig: no loading scene, keep the default one or call setLoadingScene");
        if (fps <= 0 || tps <= 0)
            throw new IllegalStateException("EngineConfig: fps and tps must be positive");
    }

    public Supplier<Scene> getStartScene() { return startScene; }
    public Supplier<TemplateSceneLoading> getLoadingScene() { return loadingScene; }
    public Vector2i getWindowSize() { return new Vector2i(windowSize); }
    public Vector2i getVirtualSize() { return new Vector2i(virtualSize != null ? virtualSize : windowSize); }
    public String getProjectName() { return projectName; }
    public int getFps() { return fps; }
    public int getTps() { return tps; }
    public String getIconName() { return iconName; }
    public boolean isFullscreen() { return fullscreen; }
    public IMonitorSelector getMonitorSelector() { return monitorSelector; }
    public KeyboardLanguage getKeyboardLanguage() { return keyboardLanguage; }
    public String getGainTreePath() { return gainTreePath; }
    public boolean isAdmin() { return admin; }
    public boolean isOpenGLErrorCheck() { return openGLErrorCheck; }
    public int getDebugServerPort() { return debugServerPort; }
    public IProjectLoading getProjectLoading() { return projectLoading; }
    public int getShaderVersion() { return shaderVersion; }
    public Resources.LoadingMethod getResourcesLoadingMethod() { return resourcesLoadingMethod; }
    public int getMaxThreads() { return maxThreads; }
    public int getMaxGLUploadMillisecondsPerFrame() { return maxGLUploadMillisecondsPerFrame; }
}
