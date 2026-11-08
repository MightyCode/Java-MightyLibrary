package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.inputs.GamepadManager;
import io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import io.github.mightycode.mightylib.inputs.keyboardlanguage.QWERTKeyboardLanguage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ContextManager {
    private String mainContext;
    private final Map<String, Context> contexts;
    private GamepadManager gamepadManager;
    // Hidden context that every window shares its objects with, created with the first window
    private GLContext rootGLContext;

    ContextManager(){
        contexts = new HashMap<>();
        mainContext = "Main";
    }


    GLContext rootGLContext() {
        if (rootGLContext == null)
            rootGLContext = GLContext.createRoot();

        return rootGLContext;
    }

    // The window settings come from EngineConfig (MainLoop): WindowCreationInfo stays internal
    void createDefaultContext(WindowCreationInfo wci){
        createNewContext("Main", wci);
    }


    public boolean createNewContext(String context){
        return createNewContext(context, null);
    }

    boolean createNewContext(String context, WindowCreationInfo wci){
        Window window;
        if (wci == null) window = new Window();
        else window = new Window(wci);

        window.createNewWindow(rootGLContext());

        KeyboardLanguage keyboardLanguage = (wci != null && wci.KeyboardLanguage != null) ?
                wci.KeyboardLanguage : QWERTKeyboardLanguage.getInstance();

        KeyboardManager keyboardManager = new KeyboardManager(window.getInfo(), keyboardLanguage);
        MouseManager mouseManager = new MouseManager(window.getInfo());
        InputManager inputManager = new InputManager(keyboardManager, mouseManager, getGamepadManager());
        inputManager.init(new ActionInput[]{});

        return contexts.put(context, new Context(window, inputManager, keyboardManager, mouseManager)) != null;
    }


    public Context getContext(String contextName){
        return contexts.get(contextName);
    }

    public Context getMainContext(){
        return contexts.get(mainContext);
    }


    public Collection<Context> getAllContext(){
        return contexts.values();
    }


    public boolean deleteContext(String contextName){
        if (contextName.compareTo(mainContext) == 0) return false;

        return contexts.remove(contextName) != null;
    }


    public boolean setNewMainContext(String contextName){
        if (contextName.compareTo(mainContext) == 0) return false;

        if (!contexts.containsKey(contextName)) return false;

        mainContext = contextName;

        return true;
    }

    // Gamepads don't belong to a window : one manager for all the contexts, created with the first one (GLFW initialized)
    public GamepadManager getGamepadManager(){
        if (gamepadManager == null) {
            gamepadManager = new GamepadManager();
            gamepadManager.init();
        }

        return gamepadManager;
    }

    public void postUpdate(){
        if (gamepadManager != null)
            gamepadManager.postUpdate();

        for (Context context : contexts.values()){
            context.postUpdate();
        }
    }

    public void unload(){
        for (Context context : contexts.values()){
            context.unload();
        }

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
