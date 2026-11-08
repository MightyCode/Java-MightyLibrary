package io.github.mightycode.mightylibdemo.main;

import static org.lwjgl.glfw.GLFW.*;

import io.github.mightycode.mightylib.algorithms.wavefunctioncollapse.WaveCollapseRulesLoader;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisBinding;
import io.github.mightycode.mightylib.inputs.inputType.AxisButtons;
import io.github.mightycode.mightylib.inputs.inputType.AxisGamepad;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisMouse;
import io.github.mightycode.mightylib.inputs.inputType.AxisSum;
import io.github.mightycode.mightylib.inputs.inputType.EInputType;
import io.github.mightycode.mightylib.inputs.inputType.IAction;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadAxis;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadButton;
import io.github.mightycode.mightylib.inputs.inputType.InputOr;
import io.github.mightycode.mightylib.inputs.inputType.InputSimple;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.main.procedures.IProjectLoading;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.map.TileMapLoader;
import io.github.mightycode.mightylib.resources.map.TileSetLoader;
import io.github.mightycode.mightylib.resources.models.ModelLoader;
import io.github.mightycode.mightylib.resources.sound.SoundLoader;
import io.github.mightycode.mightylib.resources.texture.TextureDataAtlasLoader;

class ProjectLoading implements IProjectLoading {
    @Override
    public void init(Resources resources) {
        resources.Loaders.add(new TileSetLoader());
        resources.Loaders.add(new TileMapLoader());
        resources.Loaders.add(new SoundLoader());
        resources.Loaders.add(new ModelLoader());
        resources.Loaders.add(new TextureDataAtlasLoader());
        resources.Loaders.add(new WaveCollapseRulesLoader());
    }

    @Override
    public void contextLoading(WindowContext context){
        InputManager inputManager = context.getInputManager();

        ActionInput[] configurations = {
                new ActionInput(ActionId.ESCAPE, "ESCAPE",
                        new InputOr(
                                new InputSimple(GLFW_KEY_ESCAPE, EInputType.Keyboard),
                                new InputGamepadButton(GLFW_GAMEPAD_BUTTON_BACK))),

                // 3D moves : left stick, triggers to go up and down, left stick click to go faster
                new ActionInput(ActionId.MOVE_LEFT, "MOVE_LEFT",
                        new InputOr(
                                new InputSimple(GLFW_KEY_A, EInputType.Keyboard),
                                new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.NEGATIVE))),
                new ActionInput(ActionId.MOVE_RIGHT, "MOVE_RIGHT",
                        new InputOr(
                                new InputSimple(GLFW_KEY_D, EInputType.Keyboard),
                                new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.POSITIVE))),
                new ActionInput(ActionId.MOVE_FORWARD, "MOVE_FORWARD",
                        new InputOr(
                                new InputSimple(GLFW_KEY_W, EInputType.Keyboard),
                                new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.NEGATIVE))),
                new ActionInput(ActionId.MOVE_BACKWARD, "MOVE_BACKWARD",
                        new InputOr(
                                new InputSimple(GLFW_KEY_S, EInputType.Keyboard),
                                new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.POSITIVE))),
                new ActionInput(ActionId.MOVE_UP, "MOVE_UP",
                        new InputOr(
                                new InputSimple(GLFW_KEY_SPACE, EInputType.Keyboard),
                                new InputGamepadAxis(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER, InputGamepadAxis.POSITIVE))),
                new ActionInput(ActionId.MOVE_DOWN, "MOVE_DOWN",
                        new InputOr(
                                new InputSimple(GLFW_KEY_LEFT_CONTROL, EInputType.Keyboard),
                                new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER, InputGamepadAxis.POSITIVE))),
                new ActionInput(ActionId.SHIFT, "SHIFT",
                        new InputOr(
                                new InputSimple(GLFW_KEY_LEFT_SHIFT, EInputType.Keyboard),
                                new InputGamepadButton(GLFW_GAMEPAD_BUTTON_LEFT_THUMB))),

                new ActionInput(ActionId.MOVE_LEFT_2D, "MOVE_LEFT_2D",
                        new InputOr(
                                new InputOr(
                                        new InputSimple(GLFW_KEY_A, EInputType.Keyboard),
                                        new InputSimple(GLFW_KEY_LEFT, EInputType.Keyboard)),
                                gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_LEFT, GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.NEGATIVE))),
                new ActionInput(ActionId.MOVE_RIGHT_2D, "MOVE_RIGHT_2D",
                        new InputOr(
                                new InputOr(
                                        new InputSimple(GLFW_KEY_D, EInputType.Keyboard),
                                        new InputSimple(GLFW_KEY_RIGHT, EInputType.Keyboard)),
                                gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.POSITIVE))),
                new ActionInput(ActionId.MOVE_UP_2D, "MOVE_UP_2D",
                        new InputOr(
                                new InputOr(
                                        new InputSimple(GLFW_KEY_W, EInputType.Keyboard),
                                        new InputSimple(GLFW_KEY_UP, EInputType.Keyboard)),
                                gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_UP, GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.NEGATIVE))),
                new ActionInput(ActionId.MOVE_DOWN_2D, "MOVE_DOWN_2D",
                        new InputOr(
                                new InputOr(
                                        new InputSimple(GLFW_KEY_S, EInputType.Keyboard),
                                        new InputSimple(GLFW_KEY_DOWN, EInputType.Keyboard)),
                                gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_DOWN, GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.POSITIVE))),

                new ActionInput(ActionId.TAB,
                        "TAB", new InputSimple(GLFW_KEY_TAB, EInputType.Keyboard)),
                new ActionInput(ActionId.ENTER,
                        "ENTER", new InputOr(
                                new InputSimple(GLFW_KEY_ENTER, EInputType.Keyboard),
                                new InputGamepadButton(GLFW_GAMEPAD_BUTTON_A))),

                new ActionInput(ActionId.SELECT_UP,
                        "SELECT_UP", new InputOr(
                                new InputSimple(GLFW_KEY_W, EInputType.Keyboard),
                                gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_UP, GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.NEGATIVE))),
                new ActionInput(ActionId.SELECT_DOWN,
                        "SELECT_DOWN", new InputOr(
                                new InputSimple(GLFW_KEY_S, EInputType.Keyboard),
                                gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_DOWN, GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.POSITIVE))),

                new ActionInput(ActionId.LEFT_CLICK,
                        "LEFT_CLICK", new InputSimple(GLFW_MOUSE_BUTTON_1, EInputType.Mouse)),
                new ActionInput(ActionId.RIGHT_CLICK,
                        "RIGHT_CLICK", new InputSimple(GLFW_MOUSE_BUTTON_2, EInputType.Mouse)),
        };

        // 3D moves from the actions above (keys or left stick and triggers, progressive) : no double counting
        AxisInput[] axes = {
                new AxisInput(AxisId.MOVE_RIGHT_3D, "MOVE_RIGHT_3D", new AxisButtons(
                        find(configurations, ActionId.MOVE_LEFT), find(configurations, ActionId.MOVE_RIGHT))),
                new AxisInput(AxisId.MOVE_FORWARD_3D, "MOVE_FORWARD_3D", new AxisButtons(
                        find(configurations, ActionId.MOVE_BACKWARD), find(configurations, ActionId.MOVE_FORWARD))),
                new AxisInput(AxisId.MOVE_UP_3D, "MOVE_UP_3D", new AxisButtons(
                        find(configurations, ActionId.MOVE_DOWN), find(configurations, ActionId.MOVE_UP))),

                // Degrees for this tick : the mouse in pixels and the right stick (pixels per second when fully pushed)
                // added, then multiplied by the same sensitivity. Up is positive : the y of both are inverted.
                new AxisInput(AxisId.LOOK_RIGHT, "LOOK_RIGHT", new AxisSum(
                        new AxisMouse(AxisMouse.X),
                        lookStick(GLFW_GAMEPAD_AXIS_RIGHT_X)).scale(LOOK_SENSITIVITY)),
                new AxisInput(AxisId.LOOK_UP, "LOOK_UP", new AxisSum(
                        new AxisMouse(AxisMouse.Y),
                        lookStick(GLFW_GAMEPAD_AXIS_RIGHT_Y)).scale(-LOOK_SENSITIVITY)),
        };

        inputManager.init(configurations, axes);
    }

    // Degrees per mouse pixel, not adjustable yet
    private static final float LOOK_SENSITIVITY = 0.05f;
    // A stick fully pushed turns like a mouse moving this number of pixels per second (150 degrees per second)
    private static final float LOOK_STICK_PIXELS_PER_SECOND = 3000;

    // Curve 2 : precise near the center, full speed fully pushed
    private static AxisBinding lookStick(int axis) {
        return new AxisGamepad(axis).curve(2).scale(LOOK_STICK_PIXELS_PER_SECOND).perSecond();
    }

    private static ActionInput find(ActionInput[] actions, int actionId) {
        for (ActionInput action : actions) {
            if (action.actionId() == actionId)
                return action;
        }

        throw new IllegalArgumentException("No action " + actionId);
    }

    // A direction of any gamepad : its d-pad button or its left stick pushed beyond the half
    private static IAction gamepadDirection(int dpadButton, int stickAxis, int direction) {
        return new InputOr(new InputGamepadButton(dpadButton), new InputGamepadAxis(stickAxis, direction));
    }
}