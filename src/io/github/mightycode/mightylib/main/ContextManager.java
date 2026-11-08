package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.inputs.GamepadManager;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import io.github.mightycode.mightylib.inputs.keyboardlanguage.QWERTKeyboardLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The contexts of the engine, the gamepads they share and the hidden OpenGL context that all their windows share
 * their objects with.
 */
public final class ContextManager {
    // Copy on write: a scene can open a context while the engine goes through them
    private final List<Context> contexts = new CopyOnWriteArrayList<>();
    private Context mainContext;
    private GamepadManager gamepadManager;
    private int lastWindowId;
    // Hidden context that every window shares its objects with, created with the first window
    private GLContext rootGLContext;

    ContextManager() {}

    GLContext rootGLContext() {
        if (rootGLContext == null)
            rootGLContext = GLContext.createRoot();

        return rootGLContext;
    }

    // ---- Creation ----

    /** The context of the engine configuration, with its main window. */
    Context createMainContext(Engine engine, WindowConfig windowConfig) {
        if (mainContext != null)
            throw new IllegalStateException("The main context already exists");

        mainContext = createContext(engine, "Main", true, windowConfig);
        return mainContext;
    }

    // Null when the main window can't be created
    Context createContext(Engine engine, String name, boolean main, WindowConfig windowConfig) {
        for (Context existing : contexts) {
            if (existing.getName().equals(name))
                throw new IllegalArgumentException("A context is already called " + name);
        }

        Context context = new Context(engine, name, main);

        boolean closesEngine = windowConfig.getClosesEngine() != null ? windowConfig.getClosesEngine() : main;
        WindowContext window = createWindowContext(context, windowConfig.getTitle(), closesEngine, null, windowConfig, 0);
        if (window == null)
            return null;

        context.setMainWindow(window);
        contexts.add(context);
        return context;
    }

    // Null when the window can't be created. The inputs of the project are bound by the engine afterward.
    WindowContext createWindowContext(Context owner, String name, boolean closesEngine, WindowView view,
                                      WindowConfig config, long parentWindowId) {
        Window window = new Window(config.toCreationInfo(parentWindowId));
        window.createNewWindow(rootGLContext());
        if (!window.getInfo().isWindowCreated())
            return null;

        KeyboardLanguage keyboardLanguage = config.getKeyboardLanguage() != null ?
                config.getKeyboardLanguage() : QWERTKeyboardLanguage.getInstance();

        KeyboardManager keyboardManager = new KeyboardManager(window.getInfo(), keyboardLanguage);
        MouseManager mouseManager = new MouseManager(window.getInfo());
        InputManager inputManager = new InputManager(keyboardManager, mouseManager, getGamepadManager());
        inputManager.init(new ActionInput[]{});

        return new WindowContext(++lastWindowId, owner, name, closesEngine, view, window, inputManager, keyboardManager, mouseManager);
    }

    // ---- Access ----

    public Context getMainContext() { return mainContext; }

    // The main window of the main context
    public WindowContext getMainWindow() {
        return mainContext == null ? null : mainContext.getMainWindow();
    }

    public Context getContext(String name) {
        for (Context context : contexts) {
            if (context.getName().equals(name))
                return context;
        }

        return null;
    }

    public List<Context> getContexts() { return new ArrayList<>(contexts); }

    // Gamepads don't belong to a window : one manager for all the contexts, created with the first one (GLFW initialized)
    public GamepadManager getGamepadManager(){
        if (gamepadManager == null) {
            gamepadManager = new GamepadManager();
            gamepadManager.init();
        }

        return gamepadManager;
    }

    // ---- Run by the engine ----

    // Gamepads, then the inputs of all the windows: the "pressed" states of the tick are renewed
    void postUpdate() {
        if (gamepadManager != null)
            gamepadManager.postUpdate();

        for (Context context : contexts) {
            for (WindowContext window : context.getWindows())
                window.postUpdate();
        }
    }

    /**
     * Close what the users closed.
     *
     * @return true when a window that stops the engine was closed
     */
    boolean processClosures() {
        boolean stopEngine = false;
        for (Context context : contexts) {
            if (context.processClosedWindows())
                stopEngine = true;
        }

        for (Context context : contexts) {
            if (context.isCloseRequested() && context != mainContext)
                closeContext(context);
        }

        return stopEngine;
    }

    // The scene, the windows and the batches of the scene of a secondary context are freed, the others go on
    private void closeContext(Context context) {
        System.out.println("--Close context " + context.getName());
        contexts.remove(context);

        context.unloadViews();
        context.getSceneManager().releaseSceneResources();
        context.destroyMainWindow();

        // Back to the main window: the code that goes on expects an OpenGL context
        WindowContext main = getMainWindow();
        if (main != null && main.getWindow().getInfo().isWindowCreated())
            main.getWindow().bindWindow();
    }

    // What every context shows is unloaded, each with its window's OpenGL context current
    void unloadViews() {
        for (Context context : contexts)
            context.unloadViews();
    }

    // The windows and the shared OpenGL context are destroyed after everything they show
    void unload() {
        for (Context context : contexts)
            context.destroyMainWindow();

        // After the windows: the shared objects live as long as the root context
        if (rootGLContext != null) {
            rootGLContext.destroyRoot();
            rootGLContext = null;
        }

        if (gamepadManager != null) {
            gamepadManager.unload();
            gamepadManager = null;
        }
    }
}
