package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.inputs.Gamepad;
import io.github.mightycode.mightylib.inputs.GamepadManager;
import io.github.mightycode.mightylib.inputs.InputSimulation;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.choice;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.decimal;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.integer;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.suggest;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.text;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class GamepadGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "gamepad";

    public GamepadGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Gamepads (shared by all the windows), connect virtual ones, simulate buttons and axes. Changes apply"
                        + " when the inputs of the next tick are computed");
    }

    // The id of a gamepad, completed from the connected ones
    private static ArgumentType<Integer> gamepadId() {
        return new ArgumentType<Integer>() {
            @Override
            public Integer parse(String word, CommandContext context) {
                try {
                    return Integer.parseInt(word);
                } catch (NumberFormatException e) {
                    throw CommandSyntaxException.expected("a gamepad id (see gamepad list)", word);
                }
            }

            @Override
            public Collection<String> complete(String start, CommandContext context) {
                return connectedIds(manager());
            }

            @Override
            public String describe() {
                return "gamepad id";
            }
        };
    }

    private static List<String> connectedIds(GamepadManager manager) {
        List<String> ids = new ArrayList<>();
        for (Gamepad gamepad : manager.getConnectedGamepads())
            ids.add(String.valueOf(gamepad.getId()));
        return ids;
    }

    private static GamepadManager manager() {
        return Views.engine().contexts().getGamepadManager();
    }

    // A handler whose mistakes (unknown gamepad, button or axis) are errors of the command
    private static ICommandHandler guarded(ICommandHandler handler) {
        return context -> {
            try {
                return handler.run(context);
            } catch (IllegalArgumentException e) {
                return CommandResult.error(e.getMessage());
            }
        };
    }

    @Override
    protected void syntax(CommandNode root) {
        ArgumentType<Integer> id = gamepadId();
        ArgumentType<String> buttons = suggest(Gamepad::buttonNames);
        ArgumentType<String> axes = suggest(Gamepad::axisNames);

        root.executes("Connected gamepads (buttons held, axes out of their dead zone) and players",
                        context -> list(manager()))
                .then(literal("list", "The same as gamepad alone").executes(context -> list(manager())))
                .then(literal("connect", "Connect a virtual gamepad (tests, several players with one gamepad), gives its id")
                        .executes(context -> {
                            Gamepad gamepad = manager().connectVirtual();
                            if (gamepad == null)
                                return CommandResult.error("All the virtual slots are used");

                            JSONObject data = new JSONObject().put("id", gamepad.getId());
                            return CommandResult.ok("Virtual gamepad " + gamepad.getId() + " connected from the next tick", data);
                        }))
                .then(literal("disconnect", "Disconnect a virtual gamepad")
                        .then(argument("id", id, "Id of the virtual gamepad").executes(guarded(context -> {
                            int number = context.get("id");
                            manager().disconnectVirtual(gamepad(manager(), number, false));
                            return CommandResult.ok("Virtual gamepad " + number + " disconnected from the next tick");
                        }))))
                .then(literal("assign", "Give a player to a gamepad (swapped if taken)")
                        .then(argument("id", id, "Id of the gamepad")
                                .then(argument("player", integer(0, Integer.MAX_VALUE), "Number of the player")
                                        .executes(guarded(context -> {
                                            Gamepad gamepad = gamepad(manager(), context.get("id"), true);
                                            manager().assignPlayer(gamepad, context.get("player"));
                                            return CommandResult.ok(gamepad.toString());
                                        })))))
                .then(literal("unassign", "Remove the player of a gamepad")
                        .then(argument("id", id, "Id of the gamepad").executes(guarded(context -> {
                            Gamepad gamepad = gamepad(manager(), context.get("id"), true);
                            manager().unassignPlayer(gamepad);
                            return CommandResult.ok(gamepad.toString());
                        }))))
                .then(literal("autoassign", "New gamepads take the lowest free player or none")
                        .then(argument("state", choice("on", "off"), "Assign the new gamepads or not")
                                .executes(context -> {
                                    manager().setAutoAssign(context.get("state").equals("on"));
                                    return CommandResult.ok("Auto assignment " + (manager().isAutoAssign() ? "on" : "off"));
                                })))
                .then(literal("mapping", "Add an SDL_GameControllerDB mapping for an unknown joystick")
                        .then(argument("line", text(), "The mapping line, between quotes")
                                .executes(context -> manager().addMappings(context.get("line"))
                                        ? CommandResult.ok("Mapping added") : CommandResult.error("Invalid mapping"))))
                // gamepad ID ... : buttons and axes, -1 to 1 for a stick (y positive downward), 0 to 1 for a trigger
                .then(argument("id", id, "Id of the gamepad, to simulate its buttons and axes")
                        .then(literal("button", "Press a button during some ticks (1 by default)")
                                .then(argument("button", buttons, "A, B, X, Y, LEFT_BUMPER, RIGHT_BUMPER, BACK, START, GUIDE,"
                                        + " LEFT_THUMB, RIGHT_THUMB, DPAD_UP, DPAD_RIGHT, DPAD_DOWN, DPAD_LEFT (or CROSS,"
                                        + " CIRCLE, SQUARE, TRIANGLE)")
                                        .executes(guarded(context -> pressButton(context, 1)))
                                        .then(argument("ticks", integer(1, Integer.MAX_VALUE), "Number of ticks")
                                                .executes(guarded(context -> pressButton(context, context.get("ticks")))))))
                        .then(literal("hold", "Hold a button until it is released")
                                .then(argument("button", buttons, "The button").executes(guarded(context -> {
                                    int button = Gamepad.parseButton(context.get("button"));
                                    gamepad(manager(), context.get("id"), true).simulateButton(button, InputSimulation.UNTIL_RELEASE);
                                    return CommandResult.ok(Gamepad.buttonName(button) + " held from the next tick");
                                }))))
                        .then(literal("release", "Release a button")
                                .then(argument("button", buttons, "The button").executes(guarded(context -> {
                                    int button = Gamepad.parseButton(context.get("button"));
                                    gamepad(manager(), context.get("id"), true).simulateButtonRelease(button);
                                    return CommandResult.ok(Gamepad.buttonName(button) + " released from the next tick");
                                }))))
                        .then(literal("axis", "Set an axis during some ticks, hold it or release it")
                                .then(argument("axis", axes, "LEFT_X, LEFT_Y, RIGHT_X, RIGHT_Y, LEFT_TRIGGER, RIGHT_TRIGGER")
                                        .then(literal("hold", "Hold the axis at a value until it is released")
                                                .then(argument("value", decimal(-1, 1), "Value of the axis")
                                                        .executes(guarded(context -> moveAxis(context, InputSimulation.UNTIL_RELEASE)))))
                                        .then(literal("release", "Release the axis").executes(guarded(context -> {
                                            int axis = Gamepad.parseAxis(context.get("axis"));
                                            gamepad(manager(), context.get("id"), true).simulateAxisRelease(axis);
                                            return CommandResult.ok(Gamepad.axisName(axis) + " released from the next tick");
                                        })))
                                        .then(argument("value", decimal(-1, 1), "Value of the axis")
                                                .executes(guarded(context -> moveAxis(context, 1)))
                                                .then(argument("ticks", integer(1, Integer.MAX_VALUE), "Number of ticks")
                                                        .executes(guarded(context -> moveAxis(context, context.get("ticks")))))))));
    }

    private static CommandResult pressButton(CommandContext context, int ticks) {
        int button = Gamepad.parseButton(context.get("button"));
        gamepad(manager(), context.get("id"), true).simulateButton(button, ticks);
        return CommandResult.ok(Gamepad.buttonName(button) + " pressed from the next tick during " + ticks + " tick(s)");
    }

    private static CommandResult moveAxis(CommandContext context, int ticks) {
        int axis = Gamepad.parseAxis(context.get("axis"));
        float value = context.<Double>get("value").floatValue();
        gamepad(manager(), context.get("id"), true).simulateAxis(axis, value, ticks);

        String name = Gamepad.axisName(axis);
        if (ticks == InputSimulation.UNTIL_RELEASE)
            return CommandResult.ok(name + " held at " + value + " from the next tick");
        return CommandResult.ok(name + " at " + value + " from the next tick during " + ticks + " tick(s)");
    }

    private static Gamepad gamepad(GamepadManager manager, int id, boolean mustBeConnected) {
        Gamepad gamepad = manager.getGamepad(id);
        if (gamepad == null || (mustBeConnected && !gamepad.isConnected()))
            throw new IllegalArgumentException("No connected gamepad " + id + ", write -> gamepad list <-");

        return gamepad;
    }

    private CommandResult list(GamepadManager manager) {
        JSONArray gamepads = new JSONArray();
        for (Gamepad gamepad : manager.getConnectedGamepads())
            gamepads.put(describe(gamepad));

        JSONArray players = new JSONArray();
        for (int player = 0; player < manager.getPlayerSlots(); ++player) {
            Gamepad gamepad = manager.getPlayerGamepad(player);
            players.put(gamepad == null ? JSONObject.NULL : gamepad.getId());
        }

        JSONObject data = new JSONObject()
                .put("gamepads", gamepads)
                .put("players", players)
                .put("autoAssign", manager.isAutoAssign());

        return CommandResult.ok(manager.getConnectedGamepads().size() + " connected gamepad(s), state of the current tick", data);
    }

    private static JSONObject describe(Gamepad gamepad) {
        JSONObject data = new JSONObject()
                .put("id", gamepad.getId())
                .put("name", gamepad.getName())
                .put("guid", gamepad.getGuid())
                .put("mapped", gamepad.isMapped())
                .put("virtual", gamepad.isVirtual())
                .put("player", gamepad.getPlayer());

        JSONArray buttons = new JSONArray();
        for (int button = 0; button < Gamepad.BUTTONS; ++button) {
            if (gamepad.getButton(button))
                buttons.put(Gamepad.buttonName(button));
        }
        data.put("buttons", buttons);

        JSONObject axes = new JSONObject();
        for (int axis = 0; axis < Gamepad.AXES; ++axis) {
            float value = gamepad.getAxis(axis);
            if (value != 0)
                axes.put(Gamepad.axisName(axis), Math.round(value * 1000) / 1000.0);
        }
        data.put("axes", axes);

        return data;
    }
}
