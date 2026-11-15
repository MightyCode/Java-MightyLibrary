package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.IInputManager;

/**
 * An axis from two actions (keys, buttons, axes as buttons) : value of the positive one minus value of the
 * negative one, from -1 to 1. Ex : new AxisButtons(key A, key D) for a horizontal move, or
 * new AxisButtons(MOVE_LEFT, MOVE_RIGHT) from two actions, read in the input manager of the window (their bindings
 * there, simulation included).
 */
public final class AxisButtons extends AxisBinding {
    private final IAction negative;
    private final IAction positive;

    public AxisButtons(IAction negative, IAction positive) {
        this.negative = negative;
        this.positive = positive;
    }

    public AxisButtons(InputAction negative, InputAction positive) {
        this(new ActionKey(negative), new ActionKey(positive));
    }

    public IAction negative() { return negative; }
    public IAction positive() { return positive; }

    @Override
    protected float raw(IInputManager inputManager) {
        return positive.getValue(inputManager) - negative.getValue(inputManager);
    }
}
