package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;

/**
 * A signed value read at each tick, not bounded : a direction (-1 to 1), a mouse movement in pixels,
 * a rotation in degrees for this tick... Its meaning is given by the bindings that make it (AxisBinding).
 */
public interface IAxis {
    float getAxis(InputManager inputManager);
}
