package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import org.joml.Vector2i;

/**
 * How a window is created: the main window of a context (Engine.openContext) or a window opened by it
 * (Context.openWindow). Every setter returns the configuration.
 */
public final class WindowConfig {
    private String title = "";
    private DisplaySettings display = new DisplaySettings();
    // Null: the size of the window when it is created
    private Vector2i virtualSize;
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

    /** Mode and fine settings of the display (a copy is kept). */
    public WindowConfig setDisplay(DisplaySettings display) {
        this.display = display.copy();
        return this;
    }

    // Shortcut of DisplaySettings.setMode
    public WindowConfig setWindowMode(EWindowMode mode) {
        display.setMode(mode);
        return this;
    }

    // Shortcut of DisplaySettings.setWindowedSize: the size in WINDOWED mode
    public WindowConfig setSize(int width, int height) {
        display.setWindowedSize(width, height);
        return this;
    }

    // Shortcut of DisplaySettings.setMonitor, null for the primary screen
    public WindowConfig setMonitor(MonitorInfo monitor) {
        display.setMonitor(monitor);
        return this;
    }

    public WindowConfig setVirtualSize(int width, int height) {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("The virtual size of a window must be positive: " + width + "x" + height);

        this.virtualSize = new Vector2i(width, height);
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

    // A copy
    public DisplaySettings getDisplay() { return display.copy(); }

    // Null when not given: the size of the window when it is created
    public Vector2i getVirtualSize() { return virtualSize != null ? new Vector2i(virtualSize) : null; }

    public KeyboardLanguage getKeyboardLanguage() { return keyboardLanguage; }

    public WindowContext getParent() { return parent; }

    public boolean isModal() { return modal; }

    public boolean isPersistent() { return persistent; }

    public Boolean getClosesEngine() { return closesEngine; }

    public boolean hasProjectInputs() { return projectInputs; }

    // The display may differ from the asked one (ContextManager.allowedDisplay)
    WindowCreationInfo toCreationInfo(long parentWindowId, DisplaySettings display) {
        WindowCreationInfo info = new WindowCreationInfo();
        info.Display = display.copy();
        info.VirtualSize = getVirtualSize();
        info.WindowName = title;
        info.KeyboardLanguage = keyboardLanguage;
        info.ParentWindowId = parentWindowId;
        return info;
    }
}
