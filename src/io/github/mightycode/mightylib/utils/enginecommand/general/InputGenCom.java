package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.InputSimulation;
import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.main.ContextManager;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONObject;

public class InputGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "input";

    public InputGenCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 2)
            return noEnoughArgumentError();

        InputManager inputManager = ContextManager.getInstance().getMainContext().getInputManager();
        String mode = args[1].toLowerCase();

        switch (mode) {
            case "list":
                return list(inputManager);
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
            default:
                // Shortcut : input ACTION [ticks]
                return press(inputManager, args[1], args.length > 2 ? args[2] : "1");
        }
    }

    private CommandResult press(InputManager inputManager, String actionName, String ticksText) {
        ActionInput action = inputManager.findAction(actionName);
        if (action == null)
            return CommandResult.error("Unknown action ->" + actionName + "<-, write -> input list <- to list them");

        int ticks = InputSimulation.UNTIL_RELEASE;
        if (ticksText != null) {
            try {
                ticks = Integer.parseInt(ticksText);
            } catch (NumberFormatException e) {
                return CommandResult.error("Number of ticks must be an integer : " + ticksText);
            }

            if (ticks <= 0)
                return CommandResult.error("Number of ticks must be positive : " + ticks);
        }

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

        return CommandResult.ok("Actions with their id", data);
    }

    @Override
    public String help() {
        return super.help() +
                "\nSimulate actions of the main context, applied when the inputs of the next tick are computed." +
                "\nAn action is designated by its name (case insensitive) or its id." +
                "\ninput [action] [ticks] : press the action during ticks (1 by default)" +
                "\ninput press [action] [ticks] : same as above" +
                "\ninput hold [action] : press the action until it is released" +
                "\ninput release [action] : release a held action" +
                "\ninput list : list the actions with their id";
    }
}
