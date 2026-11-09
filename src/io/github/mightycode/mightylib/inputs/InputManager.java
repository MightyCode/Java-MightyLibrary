package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;
import io.github.mightycode.mightylib.inputs.inputType.EInputType;
import io.github.mightycode.mightylib.inputs.inputType.InputSimple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Inputs of a window : the bindings of the registered actions and axes (InputRegistry), read by handle.
 *
 * @author MightyCode
 * @version 1.0
 */
public class InputManager {

    private final KeyboardManager keyManager;

    public KeyboardManager getKeyboardManager() { return keyManager; }
    public MouseManager getMouseManager() { return mouseManager; }

    private final MouseManager mouseManager;

    // Shared by all the contexts, updated by the ContextManager
    private final GamepadManager gamepadManager;
    public GamepadManager getGamepadManager() { return gamepadManager; }

    private final Map<InputAction, ActionInput> actionConfigurations;

    // Simulated actions (commands, scripts), combined with the real inputs, by InputAction.index()
    private final InputSimulation simulation;


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
            this.actionConfigurations.put(actionInput.action(), actionInput);
        }

        for (AxisInput axisInput : axisInputs) {
            this.axisConfigurations.put(axisInput.axis(), axisInput);
        }
    }

    /// Axes : signed values, not bounded, their meaning is given by their bindings (AxisBinding)

    private final Map<InputAxis, AxisInput> axisConfigurations;
    // Simulated axes (commands, scripts) : the value replaces the real one while it is held, by InputAxis.index()
    private final InputSimulation axisSimulation;
    private final Map<Integer, Float> pendingAxisValues;
    private final Map<Integer, Float> simulatedAxisValues;

    // A blocked manager (its window waits for a modal window) answers nothing pressed, no value, no axis
    private boolean blocked;

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
        keyManager.setBlocked(blocked);
        mouseManager.setBlocked(blocked);
    }

    public boolean isBlocked() { return blocked; }

    // Value of the axis for this tick, 0 if not bound
    public float getAxis(InputAxis axis){
        if (blocked)
            return 0;

        AxisInput axisInput = axisConfigurations.get(axis);
        if (axisInput == null)
            return 0;

        if (axisSimulation.getState(axis.index()))
            return simulatedAxisValues.getOrDefault(axis.index(), 0f);

        return axisInput.axisBinding().getAxis(this);
    }

    // Set the axis to a value during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick
    public void simulateAxis(InputAxis axis, float value, int ticks){
        if (!axisConfigurations.containsKey(axis))
            throw new IllegalArgumentException("Axis " + axis + " is not bound");

        axisSimulation.press(axis.index(), ticks);
        pendingAxisValues.put(axis.index(), value);
    }

    public void simulateAxisRelease(InputAxis axis){
        axisSimulation.release(axis.index());
    }

    // Bound axes matching the name (full identifier, or path alone case insensitive) : one = found, several = ambiguous
    public List<AxisInput> matchAxes(String name) {
        List<AxisInput> result = new ArrayList<>();
        for (InputAxis axis : InputRegistry.matchAxes(name)) {
            AxisInput axisInput = axisConfigurations.get(axis);
            if (axisInput != null)
                result.add(axisInput);
        }
        return result;
    }

    // The only bound axis matching the name, null if unknown or ambiguous
    public AxisInput findAxis(String name) {
        List<AxisInput> axes = matchAxes(name);
        return axes.size() == 1 ? axes.get(0) : null;
    }

    public Collection<AxisInput> getAxes() {
        return axisConfigurations.values();
    }

    public boolean getState(InputAction action){
        if (blocked)
            return false;

        ActionInput actionInput = actionConfigurations.get(action);
        if (actionInput == null)
            return false;

        return actionInput.getState(this) || simulation.getState(action.index());
    }


    /**
     * Strength of the action, from 0 to 1 : 1 when a key or a button is held (or the action is simulated),
     * progressive for a gamepad axis. For a speed, a movement : value * maxSpeed.
     */
    public float getValue(InputAction action){
        if (blocked)
            return 0;

        ActionInput actionInput = actionConfigurations.get(action);
        if (actionInput == null)
            return 0;

        return simulation.getState(action.index()) ? 1 : actionInput.getValue(this);
    }


    public boolean inputPressed(InputAction action){
        if (blocked)
            return false;

        ActionInput actionInput = actionConfigurations.get(action);
        if (actionInput == null)
            return false;

        return actionInput.actionPressed(this) || simulation.pressed(action.index());
    }


    public boolean inputReleased(InputAction action){
        if (blocked)
            return false;

        ActionInput actionInput = actionConfigurations.get(action);
        if (actionInput == null)
            return false;

        return actionInput.actionReleased(this) || simulation.released(action.index());
    }

    /** Same as getState (Press, Release) or inputPressed/inputReleased (Pressed, Released), chosen by the enum. */
    public boolean isInput(InputAction action, EInputState state) {
        // An action not bound is never in any state
        if (action == null || !actionConfigurations.containsKey(action))
            return false;

        switch (state) {
            case Press:
                return getState(action);
            case Release:
                return !getState(action);
            case Pressed:
                return inputPressed(action);
            case Released:
                return inputReleased(action);
            default:
                return false;
        }
    }

    // Bound actions matching the name (full identifier, or path alone case insensitive) : one = found, several = ambiguous
    public List<ActionInput> matchActions(String name) {
        List<ActionInput> result = new ArrayList<>();
        for (InputAction action : InputRegistry.matchActions(name)) {
            ActionInput actionInput = actionConfigurations.get(action);
            if (actionInput != null)
                result.add(actionInput);
        }
        return result;
    }

    // The only bound action matching the name, null if unknown or ambiguous
    public ActionInput findAction(String name) {
        List<ActionInput> actions = matchActions(name);
        return actions.size() == 1 ? actions.get(0) : null;
    }

    public Collection<ActionInput> getActions() {
        return actionConfigurations.values();
    }

    // Press the action during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick
    public void simulatePress(InputAction action, int ticks) {
        simulation.press(action.index(), ticks);
    }

    // Release the simulated action from the next tick
    public void simulateRelease(InputAction action) {
        simulation.release(action.index());
    }

    public boolean isSimulated(InputAction action) {
        return simulation.isHeld(action.index());
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

    // Package-private for the tests (no keyboard nor mouse there)
    void updateActionSimulation(){
        simulation.update();
    }


    private void initLibraryInputs(){
        actionConfigurations.put(EngineActions.OPEN_COMMAND,
                new ActionInput(EngineActions.OPEN_COMMAND, new InputSimple(GLFW_KEY_F1, EInputType.Keyboard)));

        actionConfigurations.put(EngineActions.RELOAD_TEXTURE,
                new ActionInput(EngineActions.RELOAD_TEXTURE, new InputSimple(GLFW_KEY_F5, EInputType.Keyboard)));
    }
}
