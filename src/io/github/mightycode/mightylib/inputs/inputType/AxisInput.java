package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputAxis;

/**
 * Binding of a registered axis (MOVE_X, LOOK_X...) in an IInputManager, read with IInputManager.getAxis(axis).
 */
public final class AxisInput {
    private final InputAxis axis;
    private final IAxis binding;

    public AxisInput(InputAxis axis, IAxis binding) {
        this.axis = axis;
        this.binding = binding;
    }

    public InputAxis axis() { return axis; }
    public IAxis axisBinding() { return binding; }
}
