package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;
import io.github.mightycode.mightylib.inputs.inputType.EInputType;
import io.github.mightycode.mightylib.inputs.inputType.InputSimple;

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

    // Shared by all the contexts, updated by the ContextManager
    private final GamepadManager gamepadManager;
    public GamepadManager getGamepadManager() { return gamepadManager; }

    private final Map<Integer, ActionInput> actionConfigurations;

    // Simulated actions (commands, scripts), combined with the real inputs
    private final InputSimulation simulation;
    public InputSimulation getSimulation() { return simulation; }


    /**
     * Input manager class.
     * Instance the class, set the input and its types.
     */
    public InputManager(KeyboardManager keyManager, MouseManager mouseManager, GamepadManager gamepadManager){
        this.keyManager = keyManager;
        this.mouseManager = mouseManager;
        this.gamepadManager = gamepadManager;

        actionConfigurations = new HashMap<>();
        simulation = new InputSimulation();
        axisConfigurations = new HashMap<>();
        axisSimulation = new InputSimulation();
        pendingAxisValues = new HashMap<>();
        simulatedAxisValues = new HashMap<>();
    }


    public void init(ActionInput[] actionInputs){
        init(actionInputs, new AxisInput[0]);
    }

    public void init(ActionInput[] actionInputs, AxisInput[] axisInputs){
        actionConfigurations.clear();
        axisConfigurations.clear();
        initLibraryInputs();

        for (ActionInput actionInput : actionInputs) {
            this.actionConfigurations.put(actionInput.actionId(), actionInput);
        }

        for (AxisInput axisInput : axisInputs) {
            if (actionConfigurations.containsKey(axisInput.axisId()))
                throw new IllegalArgumentException("Id " + axisInput.axisId() + " of the axis " + axisInput.axisName() + " is already an action");

            this.axisConfigurations.put(axisInput.axisId(), axisInput);
        }
    }

    /// Axes : signed values, not bounded, their meaning is given by their bindings (AxisBinding)

    private final Map<Integer, AxisInput> axisConfigurations;
    // Simulated axes (commands, scripts) : the value replaces the real one while it is held
    private final InputSimulation axisSimulation;
    private final Map<Integer, Float> pendingAxisValues;
    private final Map<Integer, Float> simulatedAxisValues;

    // Value of the axis for this tick, 0 if unknown
    // A blocked manager (its window waits for a modal window) answers nothing pressed, no value, no axis
    private boolean blocked;

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
        keyManager.setBlocked(blocked);
        mouseManager.setBlocked(blocked);
    }

    public boolean isBlocked() { return blocked; }

    public float getAxis(int axisId){
        if (blocked)
            return 0;

        AxisInput axisInput = axisConfigurations.get(axisId);
        if (axisInput == null)
            return 0;

        if (axisSimulation.getState(axisId))
            return simulatedAxisValues.getOrDefault(axisId, 0f);

        return axisInput.axis().getAxis(this);
    }

    // Set the axis to a value during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick
    public void simulateAxis(int axisId, float value, int ticks){
        if (!axisConfigurations.containsKey(axisId))
            throw new IllegalArgumentException("Unknown axis " + axisId);

        axisSimulation.press(axisId, ticks);
        pendingAxisValues.put(axisId, value);
    }

    public void simulateAxisRelease(int axisId){
        axisSimulation.release(axisId);
    }

    // Find a configured axis by its name (case insensitive) or its numeric id, null if unknown
    public AxisInput findAxis(String nameOrId) {
        for (AxisInput axisInput : axisConfigurations.values()) {
            if (axisInput.axisName().equalsIgnoreCase(nameOrId))
                return axisInput;
        }

        try {
            return axisConfigurations.get(Integer.parseInt(nameOrId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Collection<AxisInput> getAxes() {
        return axisConfigurations.values();
    }

    public boolean getState(int inputID){
        if (blocked)
            return false;

        ActionInput actionInput = actionConfigurations.get(inputID);
        if (actionInput == null)
            return false;

        return actionInput.getState(this) || simulation.getState(inputID);
    }


    /**
     * Strength of the action, from 0 to 1 : 1 when a key or a button is held (or the action is simulated),
     * progressive for a gamepad axis. For a speed, a movement : value * maxSpeed.
     */
    public float getValue(int inputID){
        if (blocked)
            return 0;

        ActionInput actionInput = actionConfigurations.get(inputID);
        if (actionInput == null)
            return 0;

        return simulation.getState(inputID) ? 1 : actionInput.getValue(this);
    }


    public boolean inputPressed(int inputId){
        if (blocked)
            return false;

        ActionInput actionInput = actionConfigurations.get(inputId);
        if (actionInput == null)
            return false;

        return actionInput.actionPressed(this) || simulation.pressed(inputId);
    }


    public boolean inputReleased(int inputID){
        if (blocked)
            return false;

        ActionInput actionInput = actionConfigurations.get(inputID);
        if (actionInput == null)
            return false;

        return actionInput.actionReleased(this) || simulation.released(inputID);
    }

    /** Same as getState (Press, Release) or inputPressed/inputReleased (Pressed, Released), chosen by the enum. */
    public boolean isInput(int inputID, EInputState state) {
        // An unknown action is never in any state
        if (!actionConfigurations.containsKey(inputID))
            return false;

        switch (state) {
            case Press:
                return getState(inputID);
            case Release:
                return !getState(inputID);
            case Pressed:
                return inputPressed(inputID);
            case Released:
                return inputReleased(inputID);
            default:
                return false;
        }
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
    public void postUpdate(){
        keyManager.postUpdate();
        mouseManager.postUpdate();
        simulation.update();
        updateAxisSimulation();
    }

    // Package-private for the tests (no keyboard nor mouse there)
    void updateAxisSimulation(){
        axisSimulation.update();
        simulatedAxisValues.putAll(pendingAxisValues);
        pendingAxisValues.clear();
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
