package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.MouseManager;

/**
 * Movement of the mouse during the tick, in window pixels (y positive downward), or the scroll (y positive up).
 * Already a movement for this tick : no perSecond.
 */
public class AxisMouse extends AxisBinding {
    public static final int X = 0;
    public static final int Y = 1;
    public static final int SCROLL_X = 2;
    public static final int SCROLL_Y = 3;

    private final int source;

    public AxisMouse(int source) {
        if (source < X || source > SCROLL_Y)
            throw new IllegalArgumentException("Mouse axis must be X, Y, SCROLL_X or SCROLL_Y : " + source);

        this.source = source;
    }

    public int source() { return source; }

    @Override
    protected float raw(InputManager inputManager) {
        MouseManager mouse = inputManager.getMouseManager();
        switch (source) {
            case X:
                return mouse.deltaX();
            case Y:
                return mouse.deltaY();
            case SCROLL_X:
                return mouse.getMouseScroll().x;
            default:
                return mouse.getMouseScroll().y;
        }
    }
}
