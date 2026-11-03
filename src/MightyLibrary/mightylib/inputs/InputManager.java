package MightyLibrary.mightylib.inputs;

import MightyLibrary.mightylib.inputs.inputType.ActionInput;
import MightyLibrary.mightylib.inputs.inputType.EInputType;
import MightyLibrary.mightylib.inputs.inputType.InputSimple;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * This class is the input manager.
 *
 * @author MightyCode
 * @version 1.0
 */
public class InputManager {

    private static final int NUMBER_LIBRARY_INPUTS = 2;

    public static final int COMMAND = 0;
    public static final int RELOAD_TEXTURE = 1;

    private static int CURRENT_ID = NUMBER_LIBRARY_INPUTS;

    public static int getAndIncrementId(){
        return CURRENT_ID++;
    }

    private final KeyboardManager keyManager;

    public KeyboardManager getKeyboardManager() { return keyManager; }
    public MouseManager getMouseManager() { return mouseManager; }

    private final MouseManager mouseManager;

    private final Map<Integer, ActionInput> actionConfigurations;

    // Simulated actions (commands, scripts), combined with the real inputs
    private final InputSimulation simulation;
    public InputSimulation getSimulation() { return simulation; }


    /**
     * Input manager class.
     * Instance the class, set the input and its types.
     */
    public InputManager(KeyboardManager keyManager, MouseManager mouseManager){
        this.keyManager = keyManager;
        this.mouseManager = mouseManager;

        actionConfigurations = new HashMap<>();
        simulation = new InputSimulation();
    }


    public void init(ActionInput[] actionInputs){
        actionConfigurations.clear();
        initLibraryInputs();

        for (ActionInput actionInput : actionInputs) {
            this.actionConfigurations.put(actionInput.actionId(), actionInput);
        }
    }

    public boolean getState(int inputID){
        ActionInput actionInput = actionConfigurations.get(inputID);
        if (actionInput == null)
            return false;

        return actionInput.getState(this) || simulation.getState(inputID);
    }


    public boolean inputPressed(int inputId){
        ActionInput actionInput = actionConfigurations.get(inputId);
        if (actionInput == null)
            return false;

        return actionInput.actionPressed(this) || simulation.pressed(inputId);
    }


    public boolean inputReleased(int inputID){
        ActionInput actionInput = actionConfigurations.get(inputID);
        if (actionInput == null)
            return false;

        return actionInput.actionReleased(this) || simulation.released(inputID);
    }

    // Find a configured action by its name (case insensitive) or its numeric id, null if unknown
    public ActionInput findAction(String nameOrId) {
        for (ActionInput actionInput : actionConfigurations.values()) {
            if (actionInput.actionName().equalsIgnoreCase(nameOrId))
                return actionInput;
        }

        try {
            return actionConfigurations.get(Integer.parseInt(nameOrId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Collection<ActionInput> getActions() {
        return actionConfigurations.values();
    }


    // Compute the inputs of the next tick
    public void dispose(){
        keyManager.dispose();
        mouseManager.dispose();
        simulation.update();
    }


    private void initLibraryInputs(){
        actionConfigurations.put(
                COMMAND,
                new ActionInput(COMMAND, "OPEN_COMMAND" ,new InputSimple(GLFW_KEY_F1, EInputType.Keyboard))
        );

        actionConfigurations.put(
                RELOAD_TEXTURE,
                new ActionInput(RELOAD_TEXTURE, "RELOAD_TEXTURE", new InputSimple(GLFW_KEY_F5, EInputType.Keyboard))
        );
    }
}
