package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import org.joml.Vector2i;

/**
 * How a window is created: the main window of a context (Engine.openContext) or a window opened by it
 * (Context.openWindow). Every setter returns the configuration.
 */
public final class WindowConfig {
    // Size of a window without a given size
    public static final int DEFAULT_WIDTH = 800, DEFAULT_HEIGHT = 600;

    private String title = "";
    // Null: the size of the screen in fullscreen, DEFAULT_WIDTH x DEFAULT_HEIGHT in a window
    private Vector2i size;
    // Null: the size of the window
    private Vector2i virtualSize;
    private boolean fullscreen;
    // Null: the primary screen
    private MonitorInfo monitor;
    // Null: the one of the engine
    private KeyboardLanguage keyboardLanguage;

    // Null: the main window of the context
    private WindowContext parent;
    private boolean modal;
    private boolean persistent;
    // Null: only the main window of the main context stops the engine when closed
    private Boolean closesEngine;
    // IProjectLoading.contextLoading binds the inputs of the window
    private boolean projectInputs = true;

    public WindowConfig() {}

    public WindowConfig(String title) {
        this.title = title;
    }

    public WindowConfig setTitle(String title) {
        this.title = title;
        return this;
    }

    public WindowConfig setSize(int width, int height) {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("The size of a window must be positive: " + width + "x" + height);

        this.size = new Vector2i(width, height);
        return this;
    }

    public WindowConfig setVirtualSize(int width, int height) {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("The virtual size of a window must be positive: " + width + "x" + height);

        this.virtualSize = new Vector2i(width, height);
        return this;
    }

    public WindowConfig setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        return this;
    }

    public WindowConfig setMonitor(MonitorInfo monitor) {
        this.monitor = monitor;
        return this;
    }

    public WindowConfig setKeyboardLanguage(KeyboardLanguage keyboardLanguage) {
        this.keyboardLanguage = keyboardLanguage;
        return this;
    }

    /** The window this one belongs to: it stays above it, is centered on it and closes with it. */
    public WindowConfig setParent(WindowContext parent) {
        this.parent = parent;
        return this;
    }

    /** While open, the parent (and its own parents) receive no input. */
    public WindowConfig setModal(boolean modal) {
        this.modal = modal;
        return this;
    }

    /** By default a window is closed when the scene that opened it is left; a persistent one stays until closed. */
    public WindowConfig setPersistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    /** Closing this window stops the engine (true by default for the main window of the main context only). */
    public WindowConfig setClosesEngine(boolean closesEngine) {
        this.closesEngine = closesEngine;
        return this;
    }

    /** False when the window has no action of the project: its inputs stay empty. */
    public WindowConfig setProjectInputs(boolean projectInputs) {
        this.projectInputs = projectInputs;
        return this;
    }

    public String getTitle() { return title; }

    // Null when not given: resolved when the window is created (see size)
    public Vector2i getSize() { return size != null ? new Vector2i(size) : null; }

    // Null when not given: the size of the window
    public Vector2i getVirtualSize() { return virtualSize != null ? new Vector2i(virtualSize) : getSize(); }

    public boolean isFullscreen() { return fullscreen; }

    public MonitorInfo getMonitor() { return monitor; }

    public KeyboardLanguage getKeyboardLanguage() { return keyboardLanguage; }

    public WindowContext getParent() { return parent; }

    public boolean isModal() { return modal; }

    public boolean isPersistent() { return persistent; }

    public Boolean getClosesEngine() { return closesEngine; }

    public boolean hasProjectInputs() { return projectInputs; }

    WindowCreationInfo toCreationInfo(long parentWindowId) {
        WindowCreationInfo info = new WindowCreationInfo();
        info.Size = getSize();
        info.VirtualSize = getVirtualSize();
        info.WindowName = title;
        info.Fullscreen = fullscreen;
        info.Monitor = monitor;
        info.KeyboardLanguage = keyboardLanguage;
        info.ParentWindowId = parentWindowId;
        return info;
    }
}
