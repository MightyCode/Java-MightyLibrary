package MightyLibrary.mightylib.main;

import MightyLibrary.mightylib.inputs.inputType.ActionInput;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.KeyboardManager;
import MightyLibrary.mightylib.inputs.MouseManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ContextManager {
    private static ContextManager singletonInstance = null;
    public static ContextManager getInstance(){
        if (singletonInstance == null) singletonInstance = new ContextManager();

        return singletonInstance;
    }

    private String mainContext;
    private final Map<String, Context> contexts;

    private ContextManager(){
        contexts = new HashMap<>();
        mainContext = "Main";
    }


    public void createDefaultContext(WindowCreationInfo wci){
        createNewContext("Main", wci);
    }


    public boolean createNewContext(String context){
        return createNewContext(context, null);
    }

    public boolean createNewContext(String context, WindowCreationInfo wci){
        Window window;
        if (wci == null) window = new Window();
        else window = new Window(wci);

        window.createNewWindow();

        KeyboardManager keyboardManager = new KeyboardManager(window.getInfo());
        MouseManager mouseManager = new MouseManager(window.getInfo());
        InputManager inputManager = new InputManager(keyboardManager, mouseManager);
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

    public void dispose(){
        for (Context context : contexts.values()){
            context.dispose();
        }
    }

    public void unload(){
        for (Context context : contexts.values()){
            context.unload();
        }
    }
}
