package io.github.mightycode.mightylib.inputs.inputType;

/**
 * A named axis of the game (MOVE_X, LOOK_X...) and its binding, read with InputManager.getAxis(id).
 * Ids come from InputManager.getAndIncrementId(), like the actions.
 */
public class AxisInput {
    private final int axisId;
    private final String axisName;
    private final IAxis axis;

    public AxisInput(int axisId, String axisName, IAxis axis) {
        this.axisId = axisId;
        this.axisName = axisName;
        this.axis = axis;
    }

    public int axisId() { return axisId; }
    public String axisName() { return axisName; }
    public IAxis axis() { return axis; }
}
