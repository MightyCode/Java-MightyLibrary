package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.graphics.utils.Screenshot;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwFocusWindow;

/**
 * A scene and the windows that show it: the main window, drawn by the scene, and the windows opened from it
 * (dialogs, tool windows), each drawn by a WindowView. An engine can run several contexts, each with its own scene
 * changes and loading screen, sharing the resources.
 * <p>
 * The main context is the one of EngineConfig: closing its main window stops the engine. Closing the main window of
 * another context, or a scene of it asking to exit, closes only that context.
 */
public final class Context implements IView {
    private final Engine engine;
    private final String name;
    private final boolean main;

    private final WindowTree<WindowContext> windows = new WindowTree<>();
    private WindowContext mainWindow;
    private SceneManager sceneManager;

    private boolean closeRequested;

    Context(Engine engine, String name, boolean main) {
        this.engine = engine;
        this.name = name;
        this.main = main;
    }

    // ---- IView: the code that runs for the context outside its scene (creation of the first scene) ----

    @Override
    public Engine engine() { return engine; }

    @Override
    public WindowContext context() { return mainWindow; }

    @Override
    public Camera2D camera2D() {
        throw new IllegalStateException("The context has no camera: create it from a scene");
    }

    @Override
    public Camera3D camera3D() {
        throw new IllegalStateException("The context has no camera: create it from a scene");
    }

    // ---- Access ----

    public String getName() { return name; }

    // True for the context of the engine configuration
    public boolean isMain() { return main; }

    public WindowContext getMainWindow() { return mainWindow; }

    public SceneManager getSceneManager() { return sceneManager; }

    // The windows of the context in the order they were opened, the main window first
    public List<WindowContext> getWindows() {
        List<WindowContext> all = new ArrayList<>();
        for (WindowTree.Node<WindowContext> node : windows.all())
            all.add(node.value);

        return all;
    }

    public boolean isCloseRequested() { return closeRequested; }

    /** Ask to close the context (its windows and its scene) at the end of the frame. */
    public void requestClose() { closeRequested = true; }

    void setMainWindow(WindowContext window) {
        mainWindow = window;
        window.setNode(windows.addRoot(window));
    }

    void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    // ---- Windows ----

    /**
     * Open a window drawn by the view, from the code of a scene (or of a view). The window belongs to the scene
     * that opens it: it is closed when that scene is left, unless the configuration makes it persistent.
     *
     * @param args given to WindowView.launch
     * @throws IllegalStateException when the window can't be created
     */
    public WindowContext openWindow(WindowConfig config, WindowView view, String... args) {
        if (view == null)
            throw new IllegalArgumentException("A window needs a view");
        if (closeRequested || mainWindow == null)
            throw new IllegalStateException("The context " + name + " is closed");

        WindowContext parent = config.getParent() != null ? config.getParent() : mainWindow;
        WindowTree.Node<WindowContext> parentNode = windows.find(parent);
        if (parentNode == null)
            throw new IllegalArgumentException("The parent window does not belong to the context " + name);

        // What opens the window owns it: a scene closes its windows when it is left
        IView opener = Views.currentOrNull();

        GLContext previous = GLContext.currentOrNull();

        WindowContext window = engine.contexts().createWindowContext(this, config.getTitle(),
                config.getClosesEngine() != null && config.getClosesEngine(), view, config, parent.getWindow().getInfo().getWindowId());
        if (window == null)
            throw new IllegalStateException("The window \"" + config.getTitle() + "\" can't be created");

        WindowTree.Node<WindowContext> node = windows.add(window, parentNode, config.isModal(), config.isPersistent(), opener);
        window.setNode(node);
        refreshBlocking();

        try {
            engine.initWindowInputs(window, config);
            window.getWindow().bindWindow();
            view.attach(engine, window, args);
        } catch (RuntimeException e) {
            closeWindow(window);
            restore(previous);
            throw e;
        }

        if (config.isModal())
            glfwFocusWindow(window.getWindow().getInfo().getWindowId());

        restore(previous);
        return window;
    }

    /** Close the window and the ones opened below it; closing the main window closes the context. */
    public void closeWindow(WindowContext window) {
        WindowTree.Node<WindowContext> node = windows.find(window);
        if (node == null)
            return;

        if (node == windows.root()) {
            requestClose();
            return;
        }

        GLContext previous = GLContext.currentOrNull();

        for (WindowTree.Node<WindowContext> closing : windows.closeOrder(node))
            destroySubWindow(closing.value);

        windows.remove(node);
        refreshBlocking();
        restore(previous);
    }

    // Windows opened by this scene (or view) and not persistent
    public void closeWindowsOpenedBy(Object opener) {
        for (WindowTree.Node<WindowContext> node : windows.openedBy(opener)) {
            if (windows.contains(node))
                closeWindow(node.value);
        }
    }

    private void destroySubWindow(WindowContext window) {
        // Its view frees its GL objects in the context of its window
        window.getWindow().bindWindow();
        window.getView().unloadByContext();
        window.unload();
    }

    // The window below a modal one waits: no keyboard, mouse or action
    private void refreshBlocking() {
        for (WindowTree.Node<WindowContext> node : windows.all())
            node.value.getInputManager().setBlocked(windows.isBlocked(node));
    }

    // Back to the OpenGL context that was current, or to the main window when it is gone
    private void restore(GLContext previous) {
        if (previous != null && !previous.isDestroyed())
            previous.makeCurrent();
        else if (mainWindow != null && mainWindow.getWindow().getInfo().isWindowCreated())
            mainWindow.getWindow().bindWindow();
    }

    // ---- Run by the engine ----

    void update() {
        sceneManager.update();

        for (WindowContext window : subWindows()) {
            window.getWindow().bindWindow();
            window.getView().updateByContext();
        }
    }

    void postUpdate() {
        sceneManager.postUpdate();

        for (WindowContext window : subWindows()) {
            if (windows.find(window) != null) {
                window.getWindow().bindWindow();
                window.getView().postUpdateByContext();
            }
        }
    }

    void displayWindows() {
        for (WindowContext window : getWindows()) {
            if (windows.find(window) == null)
                continue;

            window.getWindow().bindWindow();
            if (window.isMain())
                sceneManager.display();
            else
                window.getView().displayByContext();
        }
    }

    void swapWindows() {
        for (WindowContext window : getWindows()) {
            window.getWindow().bindWindow();
            // The image asked for this window is read from its back buffer, before the swap
            Screenshot.captureIfRequested(window.getWindow().getInfo().getWindowId());
            window.getWindow().swapBuffers();
        }
    }

    private List<WindowContext> subWindows() {
        List<WindowContext> all = getWindows();
        all.remove(mainWindow);
        return all;
    }

    /**
     * Close the windows the user closed (or the code asked to close).
     *
     * @return true when one of them stops the engine
     */
    boolean processClosedWindows() {
        for (WindowContext window : getWindows()) {
            if (windows.find(window) == null || !window.shouldClose())
                continue;

            if (window.closesEngine())
                return true;

            if (window.isMain())
                requestClose();
            else
                closeWindow(window);
        }

        // A secondary context ends with its scene (the main one stops the engine through the loop)
        if (!main && sceneManager.isExitRequested())
            requestClose();

        return false;
    }

    // Unload what the context shows, with each window's OpenGL context current: its windows, then its scene
    void unloadViews() {
        if (mainWindow == null || !mainWindow.getWindow().getInfo().isWindowCreated())
            return;

        for (WindowTree.Node<WindowContext> node : windows.closeOrder(windows.root())) {
            WindowContext window = node.value;
            if (window.isMain())
                continue;

            window.getWindow().bindWindow();
            window.getView().unloadByContext();
            window.unload();
        }

        // Nothing but the main window is left
        for (WindowTree.Node<WindowContext> node : windows.root().children())
            windows.remove(node);

        mainWindow.getWindow().bindWindow();
        if (sceneManager != null)
            sceneManager.unload();
    }

    // The main window and its OpenGL context, once everything it shows is unloaded
    void destroyMainWindow() {
        if (mainWindow != null)
            mainWindow.unload();
    }
}
