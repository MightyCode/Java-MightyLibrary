package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import org.joml.Vector2i;

/**
 * How a window is shown: its mode and the fine settings of each mode. Choosing the mode is enough, every other
 * setting has a default and only matters in the modes noted next to it, so they can be set in any order and
 * switching mode loses none of them.
 * <p>
 * WindowConfig, EngineConfig and the windows keep their own copy: changing an instance afterward changes no window,
 * apply it with WindowContext.setDisplay.
 */
public final class DisplaySettings {
    // Windowed size when none is given
    public static final int DEFAULT_WIDTH = 1280, DEFAULT_HEIGHT = 720;

    private EWindowMode mode = EWindowMode.WINDOWED;
    // All modes. Null: for a new window the screen of its parent, else of the main window (whose screen is the one
    // of EngineConfig.setMonitorSelector); for WindowContext.setDisplay the current screen of the window
    private MonitorInfo monitor;

    // WINDOWED. Null: DEFAULT_WIDTH x DEFAULT_HEIGHT
    private Vector2i windowedSize;
    private boolean decorated = true;
    private boolean resizable;

    // FULLSCREEN. Null: the current resolution of the screen, the screen doesn't change of video mode
    private Vector2i resolution;
    // FULLSCREEN. 0: the current refresh rate of the screen (or the closest one)
    private int refreshRate;
    // FULLSCREEN. Minimized when another window takes the focus (GLFW_AUTO_ICONIFY)
    private boolean minimizeOnFocusLoss = true;

    // All modes. Frames wait for the screen refresh (swap interval 1), off by default
    private boolean vsync;

    public DisplaySettings() {}

    public DisplaySettings(EWindowMode mode) {
        setMode(mode);
    }

    public DisplaySettings copy() {
        DisplaySettings copy = new DisplaySettings(mode);
        copy.monitor = monitor;
        copy.windowedSize = windowedSize == null ? null : new Vector2i(windowedSize);
        copy.decorated = decorated;
        copy.resizable = resizable;
        copy.resolution = resolution == null ? null : new Vector2i(resolution);
        copy.refreshRate = refreshRate;
        copy.minimizeOnFocusLoss = minimizeOnFocusLoss;
        copy.vsync = vsync;
        return copy;
    }

    public DisplaySettings setMode(EWindowMode mode) {
        if (mode == null)
            throw new IllegalArgumentException("The window mode can't be null");

        this.mode = mode;
        return this;
    }

    public DisplaySettings setMonitor(MonitorInfo monitor) {
        this.monitor = monitor;
        return this;
    }

    public DisplaySettings setWindowedSize(int width, int height) {
        windowedSize = checkedSize(width, height, "windowed size");
        return this;
    }

    // Back to DEFAULT_WIDTH x DEFAULT_HEIGHT
    public DisplaySettings clearWindowedSize() {
        windowedSize = null;
        return this;
    }

    public DisplaySettings setDecorated(boolean decorated) {
        this.decorated = decorated;
        return this;
    }

    public DisplaySettings setResizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    public DisplaySettings setResolution(int width, int height) {
        resolution = checkedSize(width, height, "fullscreen resolution");
        return this;
    }

    // Back to the current resolution of the screen
    public DisplaySettings clearResolution() {
        resolution = null;
        return this;
    }

    public DisplaySettings setRefreshRate(int refreshRate) {
        if (refreshRate < 0)
            throw new IllegalArgumentException("The refresh rate can't be negative: " + refreshRate);

        this.refreshRate = refreshRate;
        return this;
    }

    public DisplaySettings setMinimizeOnFocusLoss(boolean minimizeOnFocusLoss) {
        this.minimizeOnFocusLoss = minimizeOnFocusLoss;
        return this;
    }

    public DisplaySettings setVsync(boolean vsync) {
        this.vsync = vsync;
        return this;
    }

    private static Vector2i checkedSize(int width, int height, String what) {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("The " + what + " must be positive: " + width + "x" + height);

        return new Vector2i(width, height);
    }

    public EWindowMode getMode() { return mode; }
    public MonitorInfo getMonitor() { return monitor; }
    // Null when not given
    public Vector2i getWindowedSize() { return windowedSize == null ? null : new Vector2i(windowedSize); }
    public boolean isDecorated() { return decorated; }
    public boolean isResizable() { return resizable; }
    // Null when not given
    public Vector2i getResolution() { return resolution == null ? null : new Vector2i(resolution); }
    public int getRefreshRate() { return refreshRate; }
    public boolean minimizesOnFocusLoss() { return minimizeOnFocusLoss; }
    public boolean isVsync() { return vsync; }

    /**
     * Size of the window in its mode, given the current resolution of its screen (null or empty if unknown):
     * the windowed size, the fullscreen resolution or the screen.
     */
    public Vector2i sizeOn(Vector2i screen) {
        boolean knownScreen = screen != null && screen.x > 0 && screen.y > 0;

        switch (mode) {
            case FULLSCREEN:
                if (resolution != null)
                    return new Vector2i(resolution);
                // Fall through: the resolution of the screen
            case BORDERLESS:
                if (knownScreen)
                    return new Vector2i(screen);
                // Fall through: a screen without video mode, the window keeps a usable size
            default:
                return windowedSize != null ? new Vector2i(windowedSize) : new Vector2i(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
    }

    @Override
    public String toString() {
        return describeMode() + (vsync ? ", vsync" : "");
    }

    private String describeMode() {
        switch (mode) {
            case FULLSCREEN:
                return "fullscreen " + (resolution == null ? "screen resolution" : resolution.x + "x" + resolution.y)
                        + (refreshRate == 0 ? "" : "@" + refreshRate + "Hz")
                        + (minimizeOnFocusLoss ? "" : ", stays on focus loss");
            case BORDERLESS:
                return "borderless";
            default:
                return "windowed " + (windowedSize == null ? "default size" : windowedSize.x + "x" + windowedSize.y)
                        + (decorated ? "" : ", no border") + (resizable ? ", resizable" : "");
        }
    }
}
