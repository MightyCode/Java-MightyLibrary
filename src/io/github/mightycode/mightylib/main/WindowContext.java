package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;

/**
 * One window of a context with what belongs to it: its OpenGL context, its inputs and its clipboard.
 * The main window of a context is drawn by the scene of the context, the others by a WindowView.
 */
public class WindowContext {
    private final int id;
    private final Context owner;
    private final String name;
    private final Window window;
    private final InputManager inputManager;
    private final KeyboardManager keyboardManager;
    private final MouseManager mouseManager;

    private final SystemInfo systemInfo;

    private final boolean closesEngine;
    // What draws in the window, null for the main one (drawn by the scene)
    private final WindowView view;
    // Place in the tree of the windows of the context, set when it joins it
    private WindowTree.Node<WindowContext> node;

    private boolean closeRequested;

    WindowContext(int id,
                  Context owner,
                  String name,
                  boolean closesEngine,
                  WindowView view,
                  Window window,
                  InputManager inputManager,
                  KeyboardManager keyboardManager,
                  MouseManager mouseManager){

        this.id = id;
        this.owner = owner;
        this.name = name;
        this.closesEngine = closesEngine;
        this.view = view;
        this.window = window;
        this.inputManager = inputManager;
        this.keyboardManager = keyboardManager;
        this.mouseManager = mouseManager;

        this.systemInfo = new SystemInfo(this.window.getInfo());
    }

    // The context (scene and windows) this window belongs to
    public Context getContext() { return owner; }

    // Number of the window in the engine, never reused: what the commands call it
    public int getId() { return id; }

    public String getName() { return name; }

    public Window getWindow() { return window; }

    public InputManager getInputManager() { return inputManager; }
    public KeyboardManager getKeyboardManager() { return keyboardManager; }
    public MouseManager getMouseManager() { return mouseManager; }

    public SystemInfo getSystemInfo() { return systemInfo; }

    // True for the window drawn by the scene of the context
    public boolean isMain() { return view == null; }

    // The view drawing in this window, null for the main window
    public WindowView getView() { return view; }

    // The window this one belongs to, null for the main window of a context
    public WindowContext getParent() {
        return node == null || node.parent == null ? null : node.parent.value;
    }

    public boolean isModal() { return node != null && node.modal; }

    // A modal window is open below this one: it receives no input
    public boolean isBlocked() { return inputManager.isBlocked(); }

    public boolean closesEngine() { return closesEngine; }

    void setNode(WindowTree.Node<WindowContext> node) { this.node = node; }

    WindowTree.Node<WindowContext> getNode() { return node; }

    /** Ask to close the window: done by the engine at the end of the current frame, with the windows below it. */
    public void requestClose() { closeRequested = true; }

    // The user closed the window, or the code asked for it
    boolean shouldClose() {
        return closeRequested || window.wantExit();
    }

    public Camera3D createCamera(Camera3DCreationInfo info){
        return new Camera3D(window.getInfo(), info.fov, info.initialPosition);
    }


    public void postUpdate(){
        inputManager.postUpdate();
    }


    public void unload(){
        window.destroyWindow();
    }
}
