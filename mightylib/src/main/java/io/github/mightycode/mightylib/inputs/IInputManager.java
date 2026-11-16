package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;

import java.util.Collection;
import java.util.List;

/**
 * The actions and axes of a window, read from its keyboard, mouse and the gamepads (WindowContext.getInputManager). One
 * per window, created by the factory of the registry (registry.services().inputManager); the engine gives
 * InputManager::new.
 */
public interface IInputManager {
    KeyboardManager getKeyboardManager();

    MouseManager getMouseManager();

    // Shared by all the windows
    GamepadManager getGamepadManager();

    // The declared actions and axes, shared by all the windows of the engine
    InputRegistry registry();

    /**
     * The default bindings of the registry: those of the engine (console...), and those of the plugins when the window
     * has the inputs of the game (WindowConfig.hasProjectInputs; not the detached console).
     */
    void bindDefaults(boolean pluginInputs);

    void init(ActionInput[] actionInputs);

    /**
     * The default bindings of the engine, then the given ones (they replace a default one of the same key).
     * @throws IllegalStateException if a key is not declared in the registry
     */
    void init(ActionInput[] actionInputs, AxisInput[] axisInputs);

    // A blocked manager (its window waits for a modal window) answers nothing pressed, no value, no axis
    void setBlocked(boolean blocked);

    boolean isBlocked();

    /**
     * The focus: while an owner holds it (the console is typing in this window, a text field), the scene sees neither
     * the keyboard, nor the mouse, nor any action, except the actions of the engine (namespace mightylib).
     */
    InputFocus focus();

    // Value of the axis for this tick, 0 if not bound
    float getAxis(InputAxis axis);

    // Set the axis to a value during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick
    void simulateAxis(InputAxis axis, float value, int ticks);

    void simulateAxisRelease(InputAxis axis);

    // Bound axes matching the name (full identifier, or path alone case-insensitive) : one = found, several = ambiguous
    List<AxisInput> matchAxes(String name);

    // The only bound axis matching the name, null if unknown or ambiguous
    AxisInput findAxis(String name);

    Collection<AxisInput> getAxes();

    boolean getState(InputAction action);

    /**
     * Strength of the action, from 0 to 1 : 1 when a key or a button is held (or the action is simulated), progressive
     * for a gamepad axis. For a speed, a movement : value * maxSpeed.
     */
    float getValue(InputAction action);

    boolean inputPressed(InputAction action);

    boolean inputReleased(InputAction action);

    /** Same as getState (Press, Release) or inputPressed/inputReleased (Pressed, Released), chosen by the enum. */
    boolean isInput(InputAction action, EInputState state);

    // Bound actions matching the name (full identifier, or path alone case-insensitive) : one = found, several = ambiguous
    List<ActionInput> matchActions(String name);

    // The only bound action matching the name, null if unknown or ambiguous
    ActionInput findAction(String name);

    Collection<ActionInput> getActions();

    // Press the action during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick
    void simulatePress(InputAction action, int ticks);

    // Release the simulated action from the next tick
    void simulateRelease(InputAction action);

    boolean isSimulated(InputAction action);

    // Compute the inputs of the next tick
    void postUpdate();
}
