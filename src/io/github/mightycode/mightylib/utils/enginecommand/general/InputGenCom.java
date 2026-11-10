package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputAxis;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.InputSimulation;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.ArgumentType;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandSyntaxException;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.ICommandHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.choice;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.decimal;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.integer;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.suggest;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.text;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class InputGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "input";

    private static final String KEY_PREFIX = "GLFW_KEY_";
    private static final List<String> MOUSE_BUTTONS = Arrays.asList("left", "right", "middle");

    public InputGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Simulate inputs of the target window (actions, axes, mouse, keyboard, text), applied when the inputs of"
                        + " the next tick are computed");
    }

    private static InputManager inputManager() {
        return Views.engine().commandWindow().getInputManager();
    }

    // A handler whose mistakes (unknown key or button...) are errors of the command
    private static ICommandHandler guarded(ICommandHandler handler) {
        return context -> {
            try {
                return handler.run(context);
            } catch (IllegalArgumentException e) {
                return CommandResult.error(e.getMessage());
            }
        };
    }

    // The end of a GLFW_KEY_ name (ENTER, A, LEFT_SHIFT...) or the GLFW number of the key
    private static ArgumentType<Integer> keyType() {
        return new ArgumentType<>() {
            @Override
            public Integer parse(String word, CommandContext context) {
                try {
                    return parseKey(word);
                } catch (IllegalArgumentException e) {
                    throw CommandSyntaxException.expected("a key (the end of a GLFW_KEY_ name like ENTER, A, LEFT_SHIFT, or a number)", word);
                }
            }

            @Override
            public Collection<String> complete(String start, CommandContext context) {
                return keyNames();
            }

            @Override
            public String describe() {
                return "key";
            }
        };
    }

    // left, right, middle or the GLFW number of the button
    private static ArgumentType<Integer> mouseButtonType() {
        return new ArgumentType<>() {
            @Override
            public Integer parse(String word, CommandContext context) {
                try {
                    return parseButton(word);
                } catch (IllegalArgumentException e) {
                    throw CommandSyntaxException.expected("a mouse button (left, right, middle or a number)", word);
                }
            }

            @Override
            public Collection<String> complete(String start, CommandContext context) {
                return MOUSE_BUTTONS;
            }

            @Override
            public String describe() {
                return "mouse button";
            }
        };
    }

    @Override
    protected void syntax(CommandNode root) {
        // The actions and axes by their full identifier, or their path alone (accepted when unique), checked when run
        ArgumentType<String> actions = suggest(() -> actionNames(inputManager()));
        ArgumentType<String> axes = suggest(() -> axisNames(inputManager()));
        ArgumentType<Integer> key = keyType();
        ArgumentType<Integer> button = mouseButtonType();

        root.then(literal("list", "List the actions with their id, then the axes")
                        .executes(context -> list(inputManager())))
                .then(literal("state", "Inputs seen by the current tick (mouse, keys, text, actions)")
                        .executes(context -> state(inputManager())))
                .then(literal("press", "Press an action during ticks (1 by default)")
                        .then(pressAction(actions)))
                .then(literal("hold", "Press an action until it is released")
                        .then(argument("action", actions, "Name or id of an action").executes(guarded(context ->
                                press(inputManager(), context.get("action"), InputSimulation.UNTIL_RELEASE)))))
                .then(literal("release", "Release a held action")
                        .then(argument("action", actions, "Name or id of an action").executes(guarded(context ->
                                release(inputManager(), context.get("action"))))))
                // Mouse : positions in window pixels, origin at the top left (like screenshots)
                .then(literal("mouse", "Move the cursor (kept until the real cursor moves), or hold and release a button")
                        .then(literal("hold", "Hold a button (drag)")
                                .executes(guarded(context -> holdButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, true)))
                                .then(argument("button", button, "left, right, middle or a number")
                                        .executes(guarded(context -> holdButton(context.get("button"), true)))))
                        .then(literal("release", "Release a button")
                                .executes(guarded(context -> holdButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, false)))
                                .then(argument("button", button, "left, right, middle or a number")
                                        .executes(guarded(context -> holdButton(context.get("button"), false)))))
                        .then(argument("x", decimal(), "Pixels of the window, from the left")
                                .then(argument("y", decimal(), "Pixels of the window, from the top")
                                        .executes("Move the cursor", guarded(context -> {
                                            float x = context.<Double>get("x").floatValue();
                                            float y = context.<Double>get("y").floatValue();
                                            inputManager().getMouseManager().simulateMove(x, y);
                                            return CommandResult.ok("Cursor at (" + x + ", " + y
                                                    + ") from the next tick, until the real cursor moves");
                                        })))))
                .then(literal("click", "Click (one tick), after moving the cursor if x y are given")
                        .executes(guarded(context -> click(GLFW.GLFW_MOUSE_BUTTON_LEFT, null, null)))
                        .then(argument("x", decimal(), "Pixels of the window, from the left")
                                .then(argument("y", decimal(), "Pixels of the window, from the top")
                                        .executes("Click the left button at a position", guarded(context -> click(
                                                GLFW.GLFW_MOUSE_BUTTON_LEFT, context.<Double>get("x").floatValue(),
                                                context.<Double>get("y").floatValue())))))
                        .then(argument("button", choice("left", "right", "middle"), "The button")
                                .executes(guarded(context -> click(parseButton(context.get("button")), null, null)))
                                .then(argument("x", decimal(), "Pixels of the window, from the left")
                                        .then(argument("y", decimal(), "Pixels of the window, from the top")
                                                .executes("Click a button at a position", guarded(context -> click(
                                                        parseButton(context.get("button")),
                                                        context.<Double>get("x").floatValue(),
                                                        context.<Double>get("y").floatValue())))))))
                .then(literal("scroll", "Scroll, positive y is up")
                        .then(argument("y", decimal(), "Vertical scroll")
                                .executes(guarded(context -> scroll(context.<Double>get("y").floatValue(), 0)))
                                .then(argument("x", decimal(), "Horizontal scroll")
                                        .executes(guarded(context -> scroll(context.<Double>get("y").floatValue(),
                                                context.<Double>get("x").floatValue()))))))
                // Keyboard
                .then(literal("key", "Press a key during ticks (1 by default), hold it or release it")
                        .then(literal("hold", "Hold a key until it is released")
                                .then(argument("key", key, "The key").executes(guarded(context -> {
                                    int code = context.get("key");
                                    inputManager().getKeyboardManager().simulateKey(code, InputSimulation.UNTIL_RELEASE);
                                    return CommandResult.ok("Key " + keyName(code) + " held from the next tick");
                                }))))
                        .then(literal("release", "Release a held key")
                                .then(argument("key", key, "The key").executes(guarded(context -> {
                                    int code = context.get("key");
                                    inputManager().getKeyboardManager().simulateKeyRelease(code);
                                    return CommandResult.ok("Key " + keyName(code) + " released from the next tick");
                                }))))
                        .then(argument("key", key, "The end of a GLFW_KEY_ name (ENTER, BACKSPACE, A, LEFT_SHIFT...) or a number")
                                .executes(guarded(context -> pressKey(context.get("key"), 1)))
                                .then(argument("ticks", integer(1, Integer.MAX_VALUE), "Number of ticks")
                                        .executes(guarded(context -> pressKey(context.get("key"), context.get("ticks")))))))
                .then(literal("text", "Type a text, read by KeyboardManager.getTypedText() at the next tick")
                        .then(argument("text", text(), "The text, between quotes if it has spaces")
                                .executes(guarded(context -> {
                                    String typed = context.get("text");
                                    inputManager().getKeyboardManager().simulateText(typed);
                                    return CommandResult.ok(typed.length() + " character(s) typed at the next tick");
                                }))))
                // Axes : replace the bindings of the axis (LOOK_X...)
                .then(literal("axis", "Set an axis during ticks (1 by default), replacing its bindings, hold it or release it")
                        .then(argument("axis", axes, "Name or id of an axis")
                                .then(literal("hold", "Hold the axis at a value until it is released")
                                        .then(argument("value", decimal(), "Value of the axis")
                                                .executes(guarded(context -> moveAxis(context, InputSimulation.UNTIL_RELEASE)))))
                                .then(literal("release", "Release the axis").executes(guarded(context -> {
                                    InputAxis axis = findAxis(inputManager(), context.get("axis"));
                                    inputManager().simulateAxisRelease(axis);
                                    return CommandResult.ok(axis + " released from the next tick");
                                })))
                                .then(argument("value", decimal(), "Value of the axis")
                                        .executes(guarded(context -> moveAxis(context, 1)))
                                        .then(argument("ticks", integer(1, Integer.MAX_VALUE), "Number of ticks")
                                                .executes(guarded(context -> moveAxis(context, context.get("ticks"))))))))
                // Shortcut : input ACTION [ticks]
                .then(pressAction(actions));
    }

    // <action> [ticks]: press an action (after "press", or directly)
    private static CommandNode pressAction(ArgumentType<String> actions) {
        return argument("action", actions, "Name (case insensitive) or id of an action")
                .executes("Press the action during one tick", guarded(context ->
                        press(inputManager(), context.get("action"), 1)))
                .then(argument("ticks", integer(1, Integer.MAX_VALUE), "Number of ticks")
                        .executes("Press the action during ticks", guarded(context ->
                                press(inputManager(), context.get("action"), context.get("ticks")))));
    }

    private static CommandResult holdButton(int button, boolean hold) {
        MouseManager mouseManager = inputManager().getMouseManager();
        if (hold)
            mouseManager.simulateButton(button, InputSimulation.UNTIL_RELEASE);
        else
            mouseManager.simulateButtonRelease(button);

        return CommandResult.ok("Mouse button " + button + (hold ? " held" : " released") + " from the next tick");
    }

    // x and y null: the cursor is not moved
    private static CommandResult click(int button, Float x, Float y) {
        MouseManager mouseManager = inputManager().getMouseManager();
        if (x != null && y != null)
            mouseManager.simulateMove(x, y);

        mouseManager.simulateButton(button, 1);

        return CommandResult.ok("Mouse button " + button + " clicked at the next tick");
    }

    private static CommandResult scroll(float y, float x) {
        inputManager().getMouseManager().simulateScroll(x, y);

        return CommandResult.ok("Scroll (" + x + ", " + y + ") at the next tick");
    }

    private static CommandResult pressKey(int key, int ticks) {
        inputManager().getKeyboardManager().simulateKey(key, ticks);

        return CommandResult.ok("Key " + keyName(key) + " pressed from the next tick during " + ticks + " tick(s)");
    }

    private static InputAxis findAxis(InputManager inputManager, String name) {
        List<AxisInput> matches = inputManager.matchAxes(name);
        if (matches.size() != 1)
            throw new IllegalArgumentException(notFound("axis", name,
                    matches.stream().map(axis -> axis.axis().toString()).collect(Collectors.toList())).getMessage());

        return matches.get(0).axis();
    }

    private static CommandResult moveAxis(CommandContext context, int ticks) {
        InputAxis axis = findAxis(inputManager(), context.get("axis"));
        float value = context.<Double>get("value").floatValue();
        inputManager().simulateAxis(axis, value, ticks);

        if (ticks == InputSimulation.UNTIL_RELEASE)
            return CommandResult.ok(axis + " held at " + value + " from the next tick");
        return CommandResult.ok(axis + " at " + value + " from the next tick during " + ticks + " tick(s)");
    }

    private static CommandResult press(InputManager inputManager, String actionName, int ticks) {
        List<ActionInput> matches = inputManager.matchActions(actionName);
        if (matches.size() != 1)
            return notFound("action", actionName, matches.stream().map(action -> action.action().toString()).collect(Collectors.toList()));
        InputAction action = matches.get(0).action();

        inputManager.simulatePress(action, ticks);

        return CommandResult.ok(action + " pressed from the next tick"
                + (ticks == InputSimulation.UNTIL_RELEASE ? " until -> input release " + action + " <-" : " during " + ticks + " tick(s)"));
    }

    private static CommandResult release(InputManager inputManager, String actionName) {
        List<ActionInput> matches = inputManager.matchActions(actionName);
        if (matches.size() != 1)
            return notFound("action", actionName, matches.stream().map(action -> action.action().toString()).collect(Collectors.toList()));
        InputAction action = matches.get(0).action();

        inputManager.simulateRelease(action);

        return CommandResult.ok(action + " released from the next tick");
    }

    // Unknown name, or a path shared by several namespaces
    private static CommandResult notFound(String kind, String name, List<String> candidates) {
        if (candidates.isEmpty())
            return CommandResult.error("Unknown " + kind + " ->" + name + "<-, write -> input list <- to list them");

        return CommandResult.error("Ambiguous " + kind + " ->" + name + "<-, write the full identifier : " + String.join(", ", candidates));
    }

    // Identifiers of the bound actions and axes, sorted
    private static CommandResult list(InputManager inputManager) {
        JSONObject data = new JSONObject();

        JSONArray actions = new JSONArray();
        inputManager.getActions().stream().map(action -> action.action().toString()).sorted().forEach(actions::put);
        data.put("actions", actions);

        JSONArray axes = new JSONArray();
        inputManager.getAxes().stream().map(axis -> axis.axis().toString()).sorted().forEach(axes::put);
        data.put("axes", axes);

        return CommandResult.ok("Bound actions and axes (full identifier, or path alone when unique)", data);
    }

    // Inputs seen by the current tick
    private static CommandResult state(InputManager inputManager) {
        MouseManager mouse = inputManager.getMouseManager();
        KeyboardManager keyboard = inputManager.getKeyboardManager();
        JSONObject data = new JSONObject();

        JSONObject mouseData = new JSONObject();
        mouseData.put("position", new JSONArray().put(mouse.posX()).put(mouse.posY()));
        mouseData.put("simulated", mouse.isCursorSimulated());
        // Asked by the game (shown or captured), then what GLFW really does (no capture during the tests)
        mouseData.put("cursorShown", mouse.getCursorState());
        int cursorMode = GLFW.glfwGetInputMode(Views.engine().commandWindow().getWindow().getInfo().getWindowId(), GLFW.GLFW_CURSOR);
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
            if (inputManager.getState(action.action()))
                actions.put(action.action().toString());
        }
        data.put("actions", actions);

        JSONObject axes = new JSONObject();
        for (AxisInput axis : inputManager.getAxes()) {
            float value = inputManager.getAxis(axis.axis());
            if (value != 0)
                axes.put(axis.axis().toString(), value);
        }
        data.put("axes", axes);

        return CommandResult.ok("Inputs of the current tick (mouse position in virtual coordinates)", data);
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

    // Full identifiers and their paths alone (accepted when unique)
    private static List<String> actionNames(InputManager inputManager) {
        List<String> names = new ArrayList<>();
        for (ActionInput action : inputManager.getActions()) {
            names.add(action.action().toString());
            names.add(action.action().identifier().path());
        }
        return names;
    }

    // Full identifiers of the axes and their paths alone (accepted when unique)
    private static List<String> axisNames(InputManager inputManager) {
        List<String> names = new ArrayList<>();
        for (AxisInput axis : inputManager.getAxes()) {
            names.add(axis.axis().toString());
            names.add(axis.axis().identifier().path());
        }
        return names;
    }

    // Names accepted by parseKey
    static List<String> keyNames() {
        List<String> names = new ArrayList<>();
        for (Field field : GLFW.class.getFields()) {
            if (field.getName().startsWith(KEY_PREFIX) && !field.getName().equals("GLFW_KEY_LAST") && field.getType() == int.class)
                names.add(field.getName().substring(KEY_PREFIX.length()));
        }
        return names;
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
}
