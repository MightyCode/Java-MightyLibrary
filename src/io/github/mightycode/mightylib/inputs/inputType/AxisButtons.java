package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;

/**
 * An axis from two actions (keys, buttons, axes as buttons) : value of the positive one minus value of the
 * negative one, from -1 to 1. Ex : new AxisButtons(key A, key D) for a horizontal move.
 */
public class AxisButtons extends AxisBinding {
    private final IAction negative;
    private final IAction positive;

    public AxisButtons(IAction negative, IAction positive) {
        this.negative = negative;
        this.positive = positive;
    }

    public IAction negative() { return negative; }
    public IAction positive() { return positive; }

    @Override
    protected float raw(InputManager inputManager) {
        return positive.getValue(inputManager) - negative.getValue(inputManager);
    }
}
