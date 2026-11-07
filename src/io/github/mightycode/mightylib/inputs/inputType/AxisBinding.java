package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.main.utils.GameTime;

/**
 * An axis read from a source (raw value), then shaped, in this order :
 * curve (sign kept, |v|^exponent : 2 gives precision near the center of a stick), scale (negative to invert),
 * per second (multiplied by the duration of the tick : a speed becomes a movement for this tick).
 *
 * Example, look of a camera in degrees for this tick, the mouse and the right stick at the same sensitivity :
 * new AxisSum(new AxisMouse(AxisMouse.X), new AxisGamepad(GLFW_GAMEPAD_AXIS_RIGHT_X).curve(2).scale(1500).perSecond())
 *         .scale(0.1f)
 * (the stick fully pushed turns like the mouse moved 1500 pixels per second, 0.1 degree per pixel).
 */
public abstract class AxisBinding implements IAxis {
    private float curve = 1;
    private float scale = 1;
    private boolean perSecond;

    // Raw value of the source
    protected abstract float raw(InputManager inputManager);

    @Override
    public final float getAxis(InputManager inputManager) {
        float value = raw(inputManager);
        if (value == 0)
            return 0;

        if (curve != 1)
            value = Math.copySign((float) Math.pow(Math.abs(value), curve), value);

        value *= scale;

        if (perSecond)
            value *= GameTime.DeltaTime();

        return value;
    }

    public AxisBinding curve(float exponent) {
        if (exponent <= 0)
            throw new IllegalArgumentException("Curve exponent must be positive : " + exponent);

        curve = exponent;
        return this;
    }

    public AxisBinding scale(float scale) {
        this.scale = scale;
        return this;
    }

    public AxisBinding perSecond() {
        perSecond = true;
        return this;
    }

    public float getCurve() { return curve; }
    public float getScale() { return scale; }
    public boolean isPerSecond() { return perSecond; }
}
