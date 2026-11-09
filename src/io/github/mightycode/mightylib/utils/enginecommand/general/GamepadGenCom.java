package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.inputs.Gamepad;
import io.github.mightycode.mightylib.inputs.GamepadManager;
import io.github.mightycode.mightylib.inputs.InputSimulation;
import io.github.mightycode.mightylib.main.ContextManager;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GamepadGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "gamepad";

    public GamepadGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME));
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        GamepadManager manager = Views.engine().contexts().getGamepadManager();

        if (args.length == 2) {
            List<String> values = new ArrayList<>(Arrays.asList(
                    "list", "connect", "disconnect", "assign", "unassign", "autoassign", "mapping"));
            values.addAll(connectedIds(manager));
            return values;
        }

        String mode = args[1].toLowerCase();
        if (args.length == 3) {
            switch (mode) {
                case "disconnect":
                case "assign":
                case "unassign":
                    return connectedIds(manager);
                case "autoassign":
                    return Arrays.asList("on", "off");
                default:
                    // gamepad ID ...
                    return Arrays.asList("button", "hold", "release", "axis");
            }
        }

        if (args.length == 4) {
            switch (args[2].toLowerCase()) {
                case "button":
                case "hold":
                case "release":
                    return Gamepad.buttonNames();
                case "axis":
                    return Gamepad.axisNames();
                default:
                    return Collections.emptyList();
            }
        }

        if (args.length == 5 && args[2].equalsIgnoreCase("axis"))
            return Arrays.asList("hold", "release");

        return Collections.emptyList();
    }

    private static List<String> connectedIds(GamepadManager manager) {
        List<String> ids = new ArrayList<>();
        for (Gamepad gamepad : manager.getConnectedGamepads())
            ids.add(String.valueOf(gamepad.getId()));
        return ids;
    }

    @Override
    public CommandResult execute(String[] args) {
        GamepadManager manager = Views.engine().contexts().getGamepadManager();

        try {
            if (args.length == 1 || args[1].equalsIgnoreCase("list"))
                return list(manager);

            switch (args[1].toLowerCase()) {
                case "connect": {
                    Gamepad gamepad = manager.connectVirtual();
                    if (gamepad == null)
                        return CommandResult.error("All the virtual slots are used");

                    JSONObject data = new JSONObject().put("id", gamepad.getId());
                    return CommandResult.ok("Virtual gamepad " + gamepad.getId() + " connected from the next tick", data);
                }
                case "disconnect":
                    if (args.length != 3)
                        return CommandResult.error("Usage : gamepad disconnect ID (virtual gamepad)");
                    manager.disconnectVirtual(gamepad(manager, args[2], false));
                    return CommandResult.ok("Virtual gamepad " + args[2] + " disconnected from the next tick");
                case "assign": {
                    if (args.length != 4)
                        return CommandResult.error("Usage : gamepad assign ID PLAYER");
                    Gamepad gamepad = gamepad(manager, args[2], true);
                    manager.assignPlayer(gamepad, parseInt(args[3]));
                    return CommandResult.ok(gamepad.toString());
                }
                case "unassign": {
                    if (args.length != 3)
                        return CommandResult.error("Usage : gamepad unassign ID");
                    Gamepad gamepad = gamepad(manager, args[2], true);
                    manager.unassignPlayer(gamepad);
                    return CommandResult.ok(gamepad.toString());
                }
                case "autoassign":
                    if (args.length != 3 || !(args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("off")))
                        return CommandResult.error("Usage : gamepad autoassign on|off");
                    manager.setAutoAssign(args[2].equalsIgnoreCase("on"));
                    return CommandResult.ok("Auto assignment " + (manager.isAutoAssign() ? "on" : "off"));
                case "mapping":
                    if (args.length != 3)
                        return CommandResult.error("Usage : gamepad mapping \"SDL_GameControllerDB line\"");
                    return manager.addMappings(args[2]) ? CommandResult.ok("Mapping added")
                            : CommandResult.error("Invalid mapping");
                default:
                    return simulate(manager, args);
            }
        } catch (IllegalArgumentException e) {
            return CommandResult.error(e.getMessage());
        }
    }

    // gamepad ID button NAME [ticks] | gamepad ID hold NAME | gamepad ID release NAME
    // gamepad ID axis NAME VALUE [ticks] | gamepad ID axis NAME hold VALUE | gamepad ID axis NAME release
    private CommandResult simulate(GamepadManager manager, String[] args) {
        if (args.length < 4)
            return unknownArgumentError(String.join(" ", args));

        Gamepad gamepad = gamepad(manager, args[1], true);
        String mode = args[2].toLowerCase();

        switch (mode) {
            case "button": {
                int button = Gamepad.parseButton(args[3]);
                int ticks = args.length > 4 ? parseTicks(args[4]) : 1;
                gamepad.simulateButton(button, ticks);
                return CommandResult.ok(Gamepad.buttonName(button) + " pressed from the next tick during " + ticks + " tick(s)");
            }
            case "hold": {
                int button = Gamepad.parseButton(args[3]);
                gamepad.simulateButton(button, InputSimulation.UNTIL_RELEASE);
                return CommandResult.ok(Gamepad.buttonName(button) + " held from the next tick");
            }
            case "release": {
                int button = Gamepad.parseButton(args[3]);
                gamepad.simulateButtonRelease(button);
                return CommandResult.ok(Gamepad.buttonName(button) + " released from the next tick");
            }
            case "axis": {
                if (args.length < 5)
                    return CommandResult.error("Usage : gamepad ID axis NAME VALUE [ticks] | hold VALUE | release");

                int axis = Gamepad.parseAxis(args[3]);
                String name = Gamepad.axisName(axis);
                if (args[4].equalsIgnoreCase("release")) {
                    gamepad.simulateAxisRelease(axis);
                    return CommandResult.ok(name + " released from the next tick");
                }

                if (args[4].equalsIgnoreCase("hold")) {
                    if (args.length != 6)
                        return CommandResult.error("Usage : gamepad ID axis NAME hold VALUE");
                    gamepad.simulateAxis(axis, parseFloat(args[5]), InputSimulation.UNTIL_RELEASE);
                    return CommandResult.ok(name + " held at " + args[5] + " from the next tick");
                }

                int ticks = args.length > 5 ? parseTicks(args[5]) : 1;
                gamepad.simulateAxis(axis, parseFloat(args[4]), ticks);
                return CommandResult.ok(name + " at " + args[4] + " from the next tick during " + ticks + " tick(s)");
            }
            default:
                return unknownArgumentError(args[2]);
        }
    }

    private static Gamepad gamepad(GamepadManager manager, String text, boolean mustBeConnected) {
        Gamepad gamepad = manager.getGamepad(parseInt(text));
        if (gamepad == null || (mustBeConnected && !gamepad.isConnected()))
            throw new IllegalArgumentException("No connected gamepad " + text + ", write -> gamepad list <-");

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

    private static int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not an integer : " + text);
        }
    }

    private static int parseTicks(String text) {
        int ticks = parseInt(text);
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

    @Override
    public String help() {
        return super.help() +
                "\nGamepads (shared by all the windows), changes applied when the inputs of the next tick are computed." +
                "\nButtons : A, B, X, Y, LEFT_BUMPER, RIGHT_BUMPER, BACK, START, GUIDE, LEFT_THUMB, RIGHT_THUMB," +
                " DPAD_UP, DPAD_RIGHT, DPAD_DOWN, DPAD_LEFT (or CROSS, CIRCLE, SQUARE, TRIANGLE)" +
                "\nAxes : LEFT_X, LEFT_Y, RIGHT_X, RIGHT_Y (-1 to 1, y positive downward), LEFT_TRIGGER, RIGHT_TRIGGER (0 to 1)" +
                "\ngamepad [list] : connected gamepads (buttons held, axes out of their dead zone) and players" +
                "\ngamepad connect : connect a virtual gamepad (tests, several players with one gamepad), gives its id" +
                "\ngamepad disconnect ID : disconnect a virtual gamepad" +
                "\ngamepad ID button NAME [ticks] : press a button during ticks (1 by default)" +
                "\ngamepad ID hold NAME / gamepad ID release NAME : hold or release a button" +
                "\ngamepad ID axis NAME VALUE [ticks] : set an axis during ticks (1 by default)" +
                "\ngamepad ID axis NAME hold VALUE / gamepad ID axis NAME release : hold or release an axis" +
                "\ngamepad assign ID PLAYER / gamepad unassign ID : give or remove a player (swapped if taken)" +
                "\ngamepad autoassign on|off : new gamepads take the lowest free player or none" +
                "\ngamepad mapping \"line\" : add an SDL_GameControllerDB mapping for an unknown joystick";
    }
}
