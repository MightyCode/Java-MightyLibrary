package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputRegistry;

// Actions of the demo, bound in ProjectLoading
public class ActionId {
    public static final InputAction MOVE_RIGHT = InputRegistry.action("demo:move_right");
    public static final InputAction MOVE_LEFT = InputRegistry.action("demo:move_left");
    public static final InputAction MOVE_FORWARD = InputRegistry.action("demo:move_forward");
    public static final InputAction MOVE_BACKWARD = InputRegistry.action("demo:move_backward");
    public static final InputAction MOVE_UP = InputRegistry.action("demo:move_up");
    public static final InputAction MOVE_DOWN = InputRegistry.action("demo:move_down");

    public static final InputAction MOVE_RIGHT_2D = InputRegistry.action("demo:move_right_2d");
    public static final InputAction MOVE_LEFT_2D = InputRegistry.action("demo:move_left_2d");
    public static final InputAction MOVE_UP_2D = InputRegistry.action("demo:move_up_2d");
    public static final InputAction MOVE_DOWN_2D = InputRegistry.action("demo:move_down_2d");

    public static final InputAction SHIFT = InputRegistry.action("demo:shift");
    public static final InputAction LEFT_CLICK = InputRegistry.action("demo:left_click");
    public static final InputAction RIGHT_CLICK = InputRegistry.action("demo:right_click");
    public static final InputAction ESCAPE = InputRegistry.action("demo:escape");
    public static final InputAction ENTER = InputRegistry.action("demo:enter");
    public static final InputAction TAB = InputRegistry.action("demo:tab");

    public static final InputAction SELECT_UP = InputRegistry.action("demo:select_up");
    public static final InputAction SELECT_DOWN = InputRegistry.action("demo:select_down");
}
