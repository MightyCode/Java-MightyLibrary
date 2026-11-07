package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Several sources of one axis added (the mouse and a stick can be used together), then shaped as a whole :
 * a scale on the sum is a sensitivity shared by all its sources.
 */
public class AxisSum extends AxisBinding {
    private final List<IAxis> axes;

    public AxisSum(IAxis... axes) {
        this.axes = Collections.unmodifiableList(Arrays.asList(axes.clone()));
    }

    public List<IAxis> axes() { return axes; }

    @Override
    protected float raw(InputManager inputManager) {
        float sum = 0;
        for (IAxis axis : axes)
            sum += axis.getAxis(inputManager);

        return sum;
    }
}
