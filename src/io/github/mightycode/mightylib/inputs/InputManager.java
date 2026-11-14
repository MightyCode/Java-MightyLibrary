package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Inputs of a window : the bindings of the actions and axes declared in the InputRegistry of the engine, read by key.
 * Using a key that is not declared is an error (IllegalStateException); a declared key without binding is never
 * pressed.
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

    // The declared actions and axes, shared by all the windows of the engine
    private final InputRegistry registry;
    public InputRegistry registry() { return registry; }

    private final Map<InputAction, ActionInput> actionConfigurations;

    // Simulated actions (commands, scripts), combined with the real inputs, by InputRegistry.index
    private final InputSimulation simulation;


    /**
     * Without binding: bindDefaults or init binds the actions and axes.
     */
    public InputManager(KeyboardManager keyManager, MouseManager mouseManager, GamepadManager gamepadManager,
                        InputRegistry registry){
        this.keyManager = keyManager;
        this.mouseManager = mouseManager;
        this.gamepadManager = gamepadManager;
        this.registry = Objects.requireNonNull(registry, "registry");

        actionConfigurations = new HashMap<>();
        simulation = new InputSimulation();
        axisConfigurations = new HashMap<>();
        axisSimulation = new InputSimulation();
        pendingAxisValues = new HashMap<>();
        simulatedAxisValues = new HashMap<>();
    }


    /**
     * The default bindings of the registry: those of the engine (console...), and those of the plugins when the window
     * has the inputs of the game (WindowConfig.hasProjectInputs; not the detached console).
     */
    public void bindDefaults(boolean pluginInputs) {
        actionConfigurations.clear();
        axisConfigurations.clear();
        for (ActionInput actionInput : registry.defaultActions(pluginInputs))
            actionConfigurations.put(actionInput.action(), actionInput);
        for (AxisInput axisInput : registry.defaultAxes(pluginInputs))
            axisConfigurations.put(axisInput.axis(), axisInput);
    }

    public void init(ActionInput[] actionInputs){
        init(actionInputs, new AxisInput[0]);
    }

    /**
     * The default bindings of the engine, then the given ones (they replace a default one of the same key).
     * @throws IllegalStateException if a key is not declared in the registry
     */
    public void init(ActionInput[] actionInputs, AxisInput[] axisInputs){
        bindDefaults(false);

        for (ActionInput actionInput : actionInputs) {
            registry.index(actionInput.action());
            this.actionConfigurations.put(actionInput.action(), actionInput);
        }

        for (AxisInput axisInput : axisInputs) {
            registry.index(axisInput.axis());
            this.axisConfigurations.put(axisInput.axis(), axisInput);
        }
    }

    /// Axes : signed values, not bounded, their meaning is given by their bindings (AxisBinding)

    private final Map<InputAxis, AxisInput> axisConfigurations;
    // Simulated axes (commands, scripts) : the value replaces the real one while it is held, by InputRegistry.index
    private final InputSimulation axisSimulation;
    private final Map<Integer, Float> pendingAxisValues;
    private final Map<Integer, Float> simulatedAxisValues;

    // A blocked manager (its window waits for a modal window) answers nothing pressed, no value, no axis
    private boolean blocked;

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
        if (keyManager != null)
            keyManager.setBlocked(blocked);
        if (mouseManager != null)
            mouseManager.setBlocked(blocked);
    }

    public boolean isBlocked() { return blocked; }

    /*
     * The focus: while an owner holds it (the console is typing in this window, a text field), the scene sees
     * neither the keyboard, nor the mouse, nor any action, except the actions of the engine (namespace mightylib:
     * open the console...). Read at each query, so it applies at once; the owner reads the raw values
     * (getRawKeyEvents, getRawTypedText...).
     * A change of the holder drops what was pressed during the current tick for everyone: the key that gives the
     * focus back is not a press for the next holder.
     */
    private final InputFocus focus = new InputFocus(this::focusChanged);
    // Keys, text and actions only: a click that gives the focus up is for the scene (InputFocus.releaseForClick)
    // The presses and releases of this tick are not seen (a focus change happened during it)
    private boolean edgesDropped;

    public InputFocus focus() { return focus; }

    private void focusChanged(boolean mousePressPasses) {
        boolean held = focus.isHeld();
        // The actions bound to the mouse button would be lost with the others: the managers drop their own edges
        if (!mousePressPasses)
            edgesDropped = true;
        if (keyManager != null) {
            keyManager.setCaptured(held);
            keyManager.dropEdges();
        }
        if (mouseManager != null) {
            mouseManager.setCaptured(held);
            if (!mousePressPasses)
                mouseManager.dropEdges();
        }
    }

    private static boolean isEngineAction(InputAction action) {
        return Identifier.ENGINE_NAMESPACE.equals(action.identifier().namespace());
    }

    // The action is not seen at all
    private boolean hides(InputAction action) {
        return blocked || (focus.isHeld() && !isEngineAction(action));
    }

    // An engine action read while the focus is held: the keyboard and the mouse answer for it
    private boolean beginRead(InputAction action) {
        boolean raw = focus.isHeld() && isEngineAction(action);
        if (raw)
            setRawRead(true);
        return raw;
    }

    private void endRead(boolean raw) {
        if (raw)
            setRawRead(false);
    }

    private void setRawRead(boolean rawRead) {
        if (keyManager != null)
            keyManager.setRawRead(rawRead);
        if (mouseManager != null)
            mouseManager.setCaptured(!rawRead);
    }

    // Value of the axis for this tick, 0 if not bound
    public float getAxis(InputAxis axis){
        int index = registry.index(axis);
        if (blocked || focus.isHeld())
            return 0;

        AxisInput axisInput = axisConfigurations.get(axis);
        if (axisInput == null)
            return 0;

        if (axisSimulation.getState(index))
            return simulatedAxisValues.getOrDefault(index, 0f);

        return axisInput.axisBinding().getAxis(this);
    }

    // Set the axis to a value during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick
    public void simulateAxis(InputAxis axis, float value, int ticks){
        int index = registry.index(axis);
        if (!axisConfigurations.containsKey(axis))
            throw new IllegalArgumentException("Axis " + axis + " is not bound");

        axisSimulation.press(index, ticks);
        pendingAxisValues.put(index, value);
    }

    public void simulateAxisRelease(InputAxis axis){
        axisSimulation.release(registry.index(axis));
    }

    // Bound axes matching the name (full identifier, or path alone case insensitive) : one = found, several = ambiguous
    public List<AxisInput> matchAxes(String name) {
        List<AxisInput> result = new ArrayList<>();
        for (InputAxis axis : registry.matchAxes(name)) {
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
        int index = registry.index(action);
        if (hides(action))
            return false;

        ActionInput actionInput = actionConfigurations.get(action);
        if (actionInput == null)
            return false;

        boolean raw = beginRead(action);
        try {
            return actionInput.getState(this) || simulation.getState(index);
        } finally {
            endRead(raw);
        }
    }


    /**
     * Strength of the action, from 0 to 1 : 1 when a key or a button is held (or the action is simulated),
     * progressive for a gamepad axis. For a speed, a movement : value * maxSpeed.
     */
    public float getValue(InputAction action){
        int index = registry.index(action);
        if (hides(action))
            return 0;

        ActionInput actionInput = actionConfigurations.get(action);
        if (actionInput == null)
            return 0;

        boolean raw = beginRead(action);
        try {
            return simulation.getState(index) ? 1 : actionInput.getValue(this);
        } finally {
            endRead(raw);
        }
    }


    public boolean inputPressed(InputAction action){
        int index = registry.index(action);
        if (hides(action) || edgesDropped)
            return false;

        ActionInput actionInput = actionConfigurations.get(action);
        if (actionInput == null)
            return false;

        boolean raw = beginRead(action);
        try {
            return actionInput.actionPressed(this) || simulation.pressed(index);
        } finally {
            endRead(raw);
        }
    }


    public boolean inputReleased(InputAction action){
        int index = registry.index(action);
        if (hides(action) || edgesDropped)
            return false;

        ActionInput actionInput = actionConfigurations.get(action);
        if (actionInput == null)
            return false;

        boolean raw = beginRead(action);
        try {
            return actionInput.actionReleased(this) || simulation.released(index);
        } finally {
            endRead(raw);
        }
    }

    /** Same as getState (Press, Release) or inputPressed/inputReleased (Pressed, Released), chosen by the enum. */
    public boolean isInput(InputAction action, EInputState state) {
        // An action not bound is never in any state
        if (action == null)
            return false;
        registry.index(action);
        if (!actionConfigurations.containsKey(action))
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
        for (InputAction action : registry.matchActions(name)) {
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
        simulation.press(registry.index(action), ticks);
    }

    // Release the simulated action from the next tick
    public void simulateRelease(InputAction action) {
        simulation.release(registry.index(action));
    }

    public boolean isSimulated(InputAction action) {
        return simulation.isHeld(registry.index(action));
    }


    // Compute the inputs of the next tick
    public void postUpdate(){
        resetDroppedEdges();
        keyManager.postUpdate();
        mouseManager.postUpdate();
        simulation.update();
        updateAxisSimulation();
    }

    // Package-private for the tests (no keyboard nor mouse there)
    void resetDroppedEdges(){
        edgesDropped = false;
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
}
