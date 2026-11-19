package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.inputs.InputAction;

// Actions of the demo, declared with their default bindings in DemoPlugin
public class ActionId {
    public static final InputAction MOVE_RIGHT = InputAction.of("demo:move_right");
    public static final InputAction MOVE_LEFT = InputAction.of("demo:move_left");
    public static final InputAction MOVE_FORWARD = InputAction.of("demo:move_forward");
    public static final InputAction MOVE_BACKWARD = InputAction.of("demo:move_backward");
    public static final InputAction MOVE_UP = InputAction.of("demo:move_up");
    public static final InputAction MOVE_DOWN = InputAction.of("demo:move_down");

    public static final InputAction MOVE_RIGHT_2D = InputAction.of("demo:move_right_2d");
    public static final InputAction MOVE_LEFT_2D = InputAction.of("demo:move_left_2d");
    public static final InputAction MOVE_UP_2D = InputAction.of("demo:move_up_2d");
    public static final InputAction MOVE_DOWN_2D = InputAction.of("demo:move_down_2d");

    public static final InputAction SHIFT = InputAction.of("demo:shift");
    public static final InputAction LEFT_CLICK = InputAction.of("demo:left_click");
    public static final InputAction RIGHT_CLICK = InputAction.of("demo:right_click");
    public static final InputAction ESCAPE = InputAction.of("demo:escape");
    public static final InputAction ENTER = InputAction.of("demo:enter");
    public static final InputAction TAB = InputAction.of("demo:tab");

    // F7: switch the color space of the current scene (ColorSpaceToggleService)
    public static final InputAction TOGGLE_COLOR_SPACE = InputAction.of("demo:toggle_color_space");

    public static final InputAction SELECT_UP = InputAction.of("demo:select_up");
    public static final InputAction SELECT_DOWN = InputAction.of("demo:select_down");
}
