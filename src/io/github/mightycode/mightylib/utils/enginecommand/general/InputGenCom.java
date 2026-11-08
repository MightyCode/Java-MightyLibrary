package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.InputSimulation;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;
import io.github.mightycode.mightylib.main.ContextManager;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class InputGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "input";

    private static final String KEY_PREFIX = "GLFW_KEY_";

    public InputGenCom() {
        super(COMMAND_NAME);
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 2)
            return noEnoughArgumentError();

        InputManager inputManager = Views.engine().contexts().getMainWindow().getInputManager();
        String mode = args[1].toLowerCase();

        try {
            switch (mode) {
                case "list":
                    return list(inputManager);
                case "state":
                    return state(inputManager);
                case "press":
                    if (args.length < 3)
                        return noEnoughArgumentError();
                    return press(inputManager, args[2], args.length > 3 ? args[3] : "1");
                case "hold":
                    if (args.length < 3)
                        return noEnoughArgumentError();
                    return press(inputManager, args[2], null);
                case "release":
                    if (args.length < 3)
                        return noEnoughArgumentError();
                    return release(inputManager, args[2]);
                case "mouse":
                    return mouse(inputManager.getMouseManager(), args);
                case "click":
                    return click(inputManager.getMouseManager(), args);
                case "scroll":
                    return scroll(inputManager.getMouseManager(), args);
                case "key":
                    return key(inputManager.getKeyboardManager(), args);
                case "axis":
                    return axis(inputManager, args);
                case "text":
                    if (args.length != 3)
                        return CommandResult.error("Usage : input text \"text to type\"");
                    inputManager.getKeyboardManager().simulateText(args[2]);
                    return CommandResult.ok(args[2].length() + " character(s) typed at the next tick");
                default:
                    // Shortcut : input ACTION [ticks]
                    return press(inputManager, args[1], args.length > 2 ? args[2] : "1");
            }
        } catch (IllegalArgumentException e) {
            return CommandResult.error(e.getMessage());
        }
    }

    // input mouse x y | input mouse hold [button] | input mouse release [button]
    private CommandResult mouse(MouseManager mouseManager, String[] args) {
        if (args.length < 3)
            return noEnoughArgumentError();

        String mode = args[2].toLowerCase();
        if (mode.equals("hold") || mode.equals("release")) {
            int button = parseButton(args.length > 3 ? args[3] : "left");
            if (mode.equals("hold"))
                mouseManager.simulateButton(button, InputSimulation.UNTIL_RELEASE);
            else
                mouseManager.simulateButtonRelease(button);

            return CommandResult.ok("Mouse button " + button + (mode.equals("hold") ? " held" : " released") + " from the next tick");
        }

        if (args.length != 4)
            return CommandResult.error("Usage : input mouse x y (window pixels, origin at the top left)");

        float x = parseFloat(args[2]);
        float y = parseFloat(args[3]);
        mouseManager.simulateMove(x, y);

        return CommandResult.ok("Cursor at (" + x + ", " + y + ") from the next tick, until the real cursor moves");
    }

    // input click [button] [x y]
    private CommandResult click(MouseManager mouseManager, String[] args) {
        int index = 2;
        int button = GLFW.GLFW_MOUSE_BUTTON_LEFT;

        if (args.length == 3 || args.length == 5)
            button = parseButton(args[index++]);

        if (args.length - index == 2) {
            mouseManager.simulateMove(parseFloat(args[index]), parseFloat(args[index + 1]));
        } else if (args.length != index) {
            return CommandResult.error("Usage : input click [button] [x y]");
        }

        mouseManager.simulateButton(button, 1);

        return CommandResult.ok("Mouse button " + button + " clicked at the next tick");
    }

    // input scroll y [x]
    private CommandResult scroll(MouseManager mouseManager, String[] args) {
        if (args.length < 3 || args.length > 4)
            return CommandResult.error("Usage : input scroll y [x] (positive y : up)");

        float y = parseFloat(args[2]);
        float x = args.length > 3 ? parseFloat(args[3]) : 0;
        mouseManager.simulateScroll(x, y);

        return CommandResult.ok("Scroll (" + x + ", " + y + ") at the next tick");
    }

    // input key KEY [ticks] | input key hold KEY | input key release KEY
    private CommandResult key(KeyboardManager keyboardManager, String[] args) {
        if (args.length < 3)
            return noEnoughArgumentError();

        String mode = args[2].toLowerCase();
        if ((mode.equals("hold") || mode.equals("release")) && args.length == 4) {
            int key = parseKey(args[3]);
            if (mode.equals("hold"))
                keyboardManager.simulateKey(key, InputSimulation.UNTIL_RELEASE);
            else
                keyboardManager.simulateKeyRelease(key);

            return CommandResult.ok("Key " + keyName(key) + (mode.equals("hold") ? " held" : " released") + " from the next tick");
        }

        if (args.length > 4)
            return CommandResult.error("Usage : input key KEY [ticks]");

        int key = parseKey(args[2]);
        int ticks = args.length > 3 ? parseTicks(args[3]) : 1;
        keyboardManager.simulateKey(key, ticks);

        return CommandResult.ok("Key " + keyName(key) + " pressed from the next tick during " + ticks + " tick(s)");
    }

    // input axis AXIS VALUE [ticks] | input axis AXIS hold VALUE | input axis AXIS release
    private CommandResult axis(InputManager inputManager, String[] args) {
        if (args.length < 4)
            return CommandResult.error("Usage : input axis AXIS VALUE [ticks] | input axis AXIS hold VALUE | input axis AXIS release");

        AxisInput axis = inputManager.findAxis(args[2]);
        if (axis == null)
            return CommandResult.error("Unknown axis ->" + args[2] + "<-, write -> input list <- to list them");

        String mode = args[3].toLowerCase();
        if (mode.equals("release")) {
            inputManager.simulateAxisRelease(axis.axisId());
            return CommandResult.ok(axis.axisName() + " released from the next tick");
        }

        if (mode.equals("hold")) {
            if (args.length != 5)
                return CommandResult.error("Usage : input axis AXIS hold VALUE");
            inputManager.simulateAxis(axis.axisId(), parseFloat(args[4]), InputSimulation.UNTIL_RELEASE);
            return CommandResult.ok(axis.axisName() + " held at " + args[4] + " from the next tick");
        }

        int ticks = args.length > 4 ? parseTicks(args[4]) : 1;
        inputManager.simulateAxis(axis.axisId(), parseFloat(args[3]), ticks);
        return CommandResult.ok(axis.axisName() + " at " + args[3] + " from the next tick during " + ticks + " tick(s)");
    }

    private CommandResult press(InputManager inputManager, String actionName, String ticksText) {
        ActionInput action = inputManager.findAction(actionName);
        if (action == null)
            return CommandResult.error("Unknown action ->" + actionName + "<-, write -> input list <- to list them");

        int ticks = ticksText == null ? InputSimulation.UNTIL_RELEASE : parseTicks(ticksText);
        inputManager.getSimulation().press(action.actionId(), ticks);

        return CommandResult.ok(action.actionName() + " pressed from the next tick"
                + (ticks == InputSimulation.UNTIL_RELEASE ? " until -> input release " + action.actionName() + " <-" : " during " + ticks + " tick(s)"));
    }

    private CommandResult release(InputManager inputManager, String actionName) {
        ActionInput action = inputManager.findAction(actionName);
        if (action == null)
            return CommandResult.error("Unknown action ->" + actionName + "<-, write -> input list <- to list them");

        inputManager.getSimulation().release(action.actionId());

        return CommandResult.ok(action.actionName() + " released from the next tick");
    }

    private CommandResult list(InputManager inputManager) {
        JSONObject data = new JSONObject();
        for (ActionInput action : inputManager.getActions())
            data.put(action.actionName(), action.actionId());

        JSONObject axes = new JSONObject();
        for (AxisInput axis : inputManager.getAxes())
            axes.put(axis.axisName(), axis.axisId());
        data.put("axes", axes);

        return CommandResult.ok("Actions with their id, then the axes", data);
    }

    // Inputs seen by the current tick
    private CommandResult state(InputManager inputManager) {
        MouseManager mouse = inputManager.getMouseManager();
        KeyboardManager keyboard = inputManager.getKeyboardManager();
        JSONObject data = new JSONObject();

        JSONObject mouseData = new JSONObject();
        mouseData.put("position", new JSONArray().put(mouse.posX()).put(mouse.posY()));
        mouseData.put("simulated", mouse.isCursorSimulated());
        // Asked by the game (shown or captured), then what GLFW really does (no capture during the tests)
        mouseData.put("cursorShown", mouse.getCursorState());
        int cursorMode = GLFW.glfwGetInputMode(Views.engine().contexts().getMainWindow().getWindow().getInfo().getWindowId(), GLFW.GLFW_CURSOR);
        mouseData.put("cursorMode", cursorMode == GLFW.GLFW_CURSOR_DISABLED ? "captured" : cursorMode == GLFW.GLFW_CURSOR_HIDDEN ? "hidden" : "normal");
        JSONArray buttons = new JSONArray();
        for (int button = 0; button < MouseManager.MOUSE_BUTTONS; ++button) {
            if (mouse.getButton(button))
                buttons.put(button);
        }
        mouseData.put("buttons", buttons);
        mouseData.put("scroll", new JSONArray().put(mouse.getMouseScroll().x).put(mouse.getMouseScroll().y));
        data.put("mouse", mouseData);

        JSONArray keys = new JSONArray();
        for (int key = 0; key <= GLFW.GLFW_KEY_LAST; ++key) {
            if (keyboard.getKeyState(key))
                keys.put(keyName(key));
        }
        data.put("keys", keys);
        data.put("text", keyboard.getTypedText());

        JSONArray actions = new JSONArray();
        for (ActionInput action : inputManager.getActions()) {
            if (inputManager.getState(action.actionId()))
                actions.put(action.actionName());
        }
        data.put("actions", actions);

        JSONObject axes = new JSONObject();
        for (AxisInput axis : inputManager.getAxes()) {
            float value = inputManager.getAxis(axis.axisId());
            if (value != 0)
                axes.put(axis.axisName(), value);
        }
        data.put("axes", axes);

        return CommandResult.ok("Inputs of the current tick (mouse position in virtual coordinates)", data);
    }

    private static int parseTicks(String text) {
        int ticks;
        try {
            ticks = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Number of ticks must be an integer : " + text);
        }

        if (ticks <= 0)
            throw new IllegalArgumentException("Number of ticks must be positive : " + ticks);

        return ticks;
    }

    private static float parseFloat(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number : " + text);
        }
    }

    // left, right, middle or the GLFW number of the button
    static int parseButton(String text) {
        switch (text.toLowerCase()) {
            case "left":
                return GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case "right":
                return GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case "middle":
                return GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            default:
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unknown mouse button ->" + text + "<-, use left, right, middle or a number");
                }
        }
    }

    // Name of a GLFW_KEY_ constant without its prefix (ENTER, A, LEFT_SHIFT...) or the GLFW number of the key
    static int parseKey(String text) {
        try {
            Field field = GLFW.class.getField(KEY_PREFIX + text.toUpperCase());
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == int.class)
                return field.getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // Maybe a number
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown key ->" + text + "<-, use the end of a GLFW_KEY_ name (ENTER, A, LEFT_SHIFT...) or a number");
        }
    }

    static String keyName(int key) {
        for (Field field : GLFW.class.getFields()) {
            if (!field.getName().startsWith(KEY_PREFIX) || field.getName().equals("GLFW_KEY_LAST") || field.getType() != int.class)
                continue;

            try {
                if (field.getInt(null) == key)
                    return field.getName().substring(KEY_PREFIX.length());
            } catch (IllegalAccessException ignored) {
                // Public constants
            }
        }

        return String.valueOf(key);
    }

    @Override
    public String help() {
        return super.help() +
                "\nSimulate inputs of the main context, applied when the inputs of the next tick are computed." +
                "\nActions : designated by their name (case insensitive) or their id" +
                "\ninput [action] [ticks] : press the action during ticks (1 by default)" +
                "\ninput press [action] [ticks] : same as above" +
                "\ninput hold [action] : press the action until it is released" +
                "\ninput release [action] : release a held action" +
                "\ninput list : list the actions with their id, then the axes" +
                "\ninput axis AXIS VALUE [ticks] : set an axis (LOOK_X...) during ticks (1 by default), replacing its bindings" +
                "\ninput axis AXIS hold VALUE / input axis AXIS release : hold or release an axis" +
                "\nMouse : positions in window pixels, origin at the top left (like screenshots)," +
                " button = left, right, middle or a number" +
                "\ninput mouse x y : move the cursor, kept until the real cursor moves" +
                "\ninput click [button] [x y] : click (one tick), after moving the cursor if x y are given" +
                "\ninput mouse hold [button] / input mouse release [button] : hold a button (drag)" +
                "\ninput scroll y [x] : scroll, positive y is up" +
                "\nKeyboard : key = end of a GLFW_KEY_ name (ENTER, BACKSPACE, A, LEFT_SHIFT...) or a number" +
                "\ninput key KEY [ticks] : press a key during ticks (1 by default)" +
                "\ninput key hold KEY / input key release KEY : hold or release a key" +
                "\ninput text \"text\" : type a text, read by KeyboardManager.getTypedText() at the next tick" +
                "\ninput state : inputs seen by the current tick (mouse, keys, text, actions)";
    }
}
