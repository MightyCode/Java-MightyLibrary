package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.graphics.utils.Screenshot;
import io.github.mightycode.mightylib.resources.texture.Icon;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * A GLFW window with its OpenGL context, shown as its DisplaySettings say (windowed, exclusive fullscreen or
 * borderless). Changing the display keeps the window, its context, its objects and its input callbacks.
 */
public final class Window {
    private final WindowInfo info;

    private GLContext glContext;

    // Window this one belongs to, 0 for none
    private long parentWindowId;
    // Display applied to the window (its own copy)
    private DisplaySettings display;
    // Last position in WINDOWED mode, to come back there from another mode; null: centered
    private Vector2i windowedPosition;
    // The display was changed while the window was minimized: placed when it is restored
    private boolean displayWaitsRestore;
    // Draws the window again from its refresh callback (Context.redrawWindow), null before it joins a context
    private Runnable redrawAction;

    public WindowInfo getInfo() { return info; }

    Window() {
        info = new WindowInfo();
        info.windowId = 0;

        info.size = new Vector2i(1, 1);
        info.ratio = 1;
        info.virtualSize = new Vector2i(1,1);
        info.virtualRatio = 1;

        info.windowName = "";
        display = new DisplaySettings();
        info.mode = display.getMode();
    }

    Window(WindowCreationInfo wci){
        info = new WindowInfo();
        info.windowId = 0;

        info.windowName = wci.WindowName;
        parentWindowId = wci.ParentWindowId;
        display = wci.Display.copy();
        info.mode = display.getMode();
        info.monitor = display.getMonitor();

        setSizeInfo(display.sizeOn(ScreenSize(monitorHandle())));
        info.virtualSize = wci.VirtualSize != null ? new Vector2i(wci.VirtualSize) : new Vector2i(info.size);
        info.virtualRatio = (float) info.virtualSize.x / (float) info.virtualSize.y;
    }

    // ---- Screen ----

    // Screen of the given settings: the chosen one, else the primary one
    static long ScreenHandle(DisplaySettings display) {
        return display.getMonitor() != null ? display.getMonitor().getHandle() : glfwGetPrimaryMonitor();
    }

    private long monitorHandle() {
        return ScreenHandle(display);
    }

    // Current video mode of the screen, null if unknown
    private static Vector2i ScreenSize(long monitorHandle) {
        GLFWVidMode mode = monitorHandle == NULL ? null : glfwGetVideoMode(monitorHandle);
        return mode == null ? null : new Vector2i(mode.width(), mode.height());
    }

    // Refresh rate asked in fullscreen: the given one, else the current one of the screen
    private int refreshRate(long monitorHandle) {
        if (display.getRefreshRate() > 0)
            return display.getRefreshRate();

        GLFWVidMode mode = monitorHandle == NULL ? null : glfwGetVideoMode(monitorHandle);
        return mode == null ? GLFW_DONT_CARE : mode.refreshRate();
    }

    // ---- Creation ----

    /**
     * Create the window and its OpenGL context, shared with the given one (the engine's hidden root context)
     * so that textures, buffers and programs are usable here too. On failure the window stays not created.
     */
    public void createNewWindow(GLContext shareWith){
        if (info.windowId != 0)
            destroyWindow();

        long monitorHandle = monitorHandle();
        boolean fullscreen = display.getMode() == EWindowMode.FULLSCREEN;

        // Configure GLFW for this window
        GLContext.applyWindowHints();
        applyModeHints(display);
        if (fullscreen)
            glfwWindowHint(GLFW_REFRESH_RATE, refreshRate(monitorHandle));

        // Active AntiAliasing
        glfwWindowHint(GLFW_SAMPLES, 4);

        long share = shareWith != null ? shareWith.getWindowHandle() : NULL;
        info.windowId = glfwCreateWindow(info.size.x, info.size.y, info.windowName, fullscreen ? monitorHandle : NULL, share);

        if (info.windowId == NULL) {
            System.err.println("--Window \"" + info.windowName + "\" can't be created (see the GLFW error above)");
            return;
        }

        System.out.println("\nWindow with id : " + info.windowId + " created (" + display + ")");

        // Placed while still hidden (on Wayland the compositor places it)
        if (!fullscreen && WindowPlatform.CanPlaceWindows()) {
            Vector2i position = position(monitorHandle);
            glfwSetWindowPos(info.windowId, position.x, position.y);
        }

        // Above its parent, not in the task bar
        if (parentWindowId != 0)
            NativeWindowOwner.set(info.windowId, parentWindowId);

        // Resized by the user (resizable window) or by the system: the viewport follows on the next frame. The size
        // is read again, the one given can be older than a change made meanwhile (nested resizing)
        glfwSetFramebufferSizeCallback(info.windowId, (window, width, height) -> readFramebufferSize());
        glfwSetWindowIconifyCallback(info.windowId, (window, iconified) -> onIconified(iconified));
        // The system asks for a new image (resizing, uncovering) while the loop may be waiting
        glfwSetWindowRefreshCallback(info.windowId, window -> {
            if (redrawAction != null)
                redrawAction.run();
        });

        // Make the OpenGL context current, with the default state of the engine's contexts
        glContext = GLContext.ofWindow(info.windowName, info.windowId);
        glContext.setVsync(display.isVsync());

        // Make the window visible
        glfwShowWindow(info.windowId);

        readFramebufferSize();
        setViewPort(info.size.x, info.size.y);
    }

    // Border, resizing and minimizing of a window about to be created
    private static void applyModeHints(DisplaySettings display) {
        boolean windowed = display.getMode() == EWindowMode.WINDOWED;
        glfwWindowHint(GLFW_DECORATED, windowed && display.isDecorated() ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, windowed && display.isResizable() ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, display.minimizesOnFocusLoss() ? GLFW_TRUE : GLFW_FALSE);
    }

    // The same on an existing window
    private void applyModeAttributes() {
        boolean windowed = display.getMode() == EWindowMode.WINDOWED;
        glfwSetWindowAttrib(info.windowId, GLFW_DECORATED, windowed && display.isDecorated() ? GLFW_TRUE : GLFW_FALSE);
        glfwSetWindowAttrib(info.windowId, GLFW_RESIZABLE, windowed && display.isResizable() ? GLFW_TRUE : GLFW_FALSE);
        glfwSetWindowAttrib(info.windowId, GLFW_AUTO_ICONIFY, display.minimizesOnFocusLoss() ? GLFW_TRUE : GLFW_FALSE);
    }

    /**
     * Position of the window out of exclusive fullscreen: the corner of its screen when borderless; in a window its
     * last windowed position, else centered on its parent, else centered in the work area (without the task bar).
     */
    private Vector2i position(long monitorHandle) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer x = stack.mallocInt(1), y = stack.mallocInt(1);
            IntBuffer width = stack.mallocInt(1), height = stack.mallocInt(1);

            if (display.getMode() == EWindowMode.BORDERLESS) {
                glfwGetMonitorPos(monitorHandle, x, y);
                return new Vector2i(x.get(0), y.get(0));
            }

            if (windowedPosition != null)
                return new Vector2i(windowedPosition);

            if (parentWindowId != 0) {
                glfwGetWindowPos(parentWindowId, x, y);
                glfwGetWindowSize(parentWindowId, width, height);
                return new Vector2i(x.get(0) + (width.get(0) - info.size.x) / 2, y.get(0) + (height.get(0) - info.size.y) / 2);
            }

            glfwGetMonitorWorkarea(monitorHandle, x, y, width, height);
            return new Vector2i(x.get(0) + Math.max(0, (width.get(0) - info.size.x) / 2),
                    y.get(0) + Math.max(0, (height.get(0) - info.size.y) / 2));
        }
    }

    // ---- Display ----

    // A copy of the display applied to the window
    public DisplaySettings getDisplay() { return display.copy(); }

    /**
     * Show the window as the settings say, at once: its OpenGL context, its objects and its input callbacks stay
     * (recreating the window would lose them all). The virtual size doesn't change. Called by
     * WindowContext.setDisplay, which keeps a single exclusive fullscreen window per screen.
     */
    void applyDisplay(DisplaySettings wanted) {
        DisplaySettings previous = display;
        display = wanted.copy();
        info.mode = display.getMode();
        info.monitor = display.getMonitor();

        long monitorHandle = monitorHandle();
        setSizeInfo(display.sizeOn(ScreenSize(monitorHandle)));

        if (!info.isWindowCreated())
            return;

        glContext.setVsync(display.isVsync());

        boolean minimized = glfwGetWindowAttrib(info.windowId, GLFW_ICONIFIED) == GLFW_TRUE;

        // Where the window was, to come back there (a minimized one has no real position); a new screen centers it
        if (ScreenHandle(previous) != monitorHandle)
            windowedPosition = null;
        else if (previous.getMode() == EWindowMode.WINDOWED && !minimized && WindowPlatform.CanPlaceWindows())
            windowedPosition = currentPosition();

        if (minimized) {
            // The system gives a restored window its former placement back, after any change made now: the display
            // is placed once the window is restored (onIconified). An exclusive fullscreen window is minimized
            // as soon as it loses the focus, so this happens with several windows.
            displayWaitsRestore = true;
            glfwRestoreWindow(info.windowId);
            return;
        }

        place();
    }

    // Border, screen, position and size of the window as its display says
    private void place() {
        long monitorHandle = monitorHandle();
        // Computed again: info.size follows the real window, which a restoring changes meanwhile
        setSizeInfo(display.sizeOn(ScreenSize(monitorHandle)));
        applyModeAttributes();

        if (display.getMode() == EWindowMode.FULLSCREEN) {
            glfwSetWindowMonitor(info.windowId, monitorHandle, 0, 0, info.size.x, info.size.y, refreshRate(monitorHandle));
        } else {
            // Wayland ignores the position, the parent is not read there
            Vector2i position = WindowPlatform.CanPlaceWindows() ? position(monitorHandle) : new Vector2i();
            glfwSetWindowMonitor(info.windowId, NULL, position.x, position.y, info.size.x, info.size.y, GLFW_DONT_CARE);
        }

        // The screen may give a close resolution only
        readFramebufferSize();
        System.out.println("--Window \"" + info.windowName + "\" : " + display + ", " + info.size.x + "x" + info.size.y);
    }

    // Called while the system restores the window: placed out of it, at the next frame
    private void onIconified(boolean iconified) {
        if (iconified || !displayWaitsRestore)
            return;

        displayWaitsRestore = false;
        MainThread.execute(() -> {
            if (info.isWindowCreated())
                place();
        });
    }

    private Vector2i currentPosition() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer x = stack.mallocInt(1), y = stack.mallocInt(1);
            glfwGetWindowPos(info.windowId, x, y);
            return new Vector2i(x.get(0), y.get(0));
        }
    }

    private void readFramebufferSize() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1), height = stack.mallocInt(1);
            glfwGetFramebufferSize(info.windowId, width, height);

            // A minimized window has an empty framebuffer: the last size stays
            if (width.get(0) > 0 && height.get(0) > 0)
                setSizeInfo(new Vector2i(width.get(0), height.get(0)));
        }
    }

    // In place: scenes and cameras keep the reference of getSizeRef
    private void setSizeInfo(Vector2i size) {
        info.size.set(size);
        info.ratio = (float) size.x / (float) size.y;
    }

    // ---- Frame ----

    void setRedrawAction(Runnable redrawAction) {
        this.redrawAction = redrawAction;
    }

    public boolean wantExit(){
        return glfwWindowShouldClose(info.windowId) && info.isWindowCreated();
    }


    // The OpenGL context of the window must be current; the events are polled once for all the windows (glfwPollEvents)
    public void swapBuffers(){
        glfwSwapBuffers(info.windowId);
    }

    public void swapBuffersAndPollEvents(){
        glfwSwapBuffers(info.windowId);
        glfwPollEvents();
    }


    public void setTitle(String title){
        if (info.isWindowCreated()){
            info.windowName = title;
            glfwSetWindowTitle(info.windowId, info.windowName);
        }
    }

    public void setIcon(Icon icon){
        // Wayland takes the icon from the .desktop file of the application
        if (!WindowPlatform.CanSetWindowIcon())
            return;
        glfwSetWindowIcon(info.windowId, icon.getBuffer());
    }

    // The OpenGL context of this window, where the objects that are not shared (vertex arrays, framebuffers) live
    public GLContext getGLContext() { return glContext; }

    // Make the OpenGL context of this window the current one
    public void bindWindow() {
        glContext.makeCurrent();
    }


    /**
     * Destroy the current window and its OpenGL context.
     */
    public void destroyWindow(){
        Screenshot.cancelRequests(info.windowId);

        if (glContext != null) {
            glContext.destroy();
            glContext = null;
        }

        if (info.isWindowCreated()) {
            // Callbacks set on this window (inputs, framebuffer size): their native part is freed with it
            Callbacks.glfwFreeCallbacks(info.windowId);
            glfwDestroyWindow(info.windowId);
            System.out.println("Window with id : " + info.windowId + " deleted");
            info.windowId = 0;
        }
    }


    public void setSize(Vector2f size){
        setSize(size.x, size.y);
    }
    public void setSize(float width, float height){
        setSize((int)width, (int)height);
    }
    public void setWidth(int width){
        setSize(width, info.size.y);
    }
    public void setHeight(int height){
        setSize(info.size.x, height);
    }


    /** The size in WINDOWED mode: applied now in a window, kept for later in the other modes. */
    public void setSize(int width, int height){
        display.setWindowedSize(width, height);
        if (display.getMode() != EWindowMode.WINDOWED)
            return;

        setSizeInfo(new Vector2i(width, height));
        if (info.isWindowCreated()) {
            glfwSetWindowSize(info.windowId, width, height);
            readFramebufferSize();
        }
    }


    public void setVirtualSize(int virtualWidth, int virtualHeight){
        info.virtualSize.x = virtualWidth;
        info.virtualSize.y = virtualHeight;
        info.virtualRatio = (float) info.virtualSize.x / (float) info.virtualSize.y;
    }

    public void setVirtualViewport(){
        setViewPort(info.virtualSize.x, info.virtualSize.y);
    }


    public void setRealViewport(){
        int x = 0, y = 0, width = info.size.x, height = info.size.y;

        if (!info.virtualSize.equals(info.size)){
            if (info.virtualRatio > info.ratio){
                float toMultiply = (float)info.size.x / (float)info.virtualSize.x;
                height = (int)(info.virtualSize.y * toMultiply);
                y = (info.size.y - height) / 2;
            } else {
                float toMultiply = (float)info.size.y / (float)info.virtualSize.y;
                width = (int)(info.virtualSize.x * toMultiply);
                x = (info.size.x - width) / 2;
            }
        }

        setViewPort(x, y, width, height);
    }

    /**
     * Set the 2D view.
     */
    public static void glEnable2D() {
        int[] vPort = new int[4];

        glGetIntegerv(GL_VIEWPORT, vPort);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        glOrtho(0, vPort[2], vPort[3], 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
    }

    /**
     * Set the 3D view.
     */
    public static void glDisable2D() {
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    private void setViewPort(int width, int height){
        setViewPort(0, 0, width, height);
    }

    private void setViewPort(int x, int y, int w, int h){
        glViewport(x, y, w, h);
    }
}
