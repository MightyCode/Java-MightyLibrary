package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONObject;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

/**
 * Let the player turn the cheats on and off: the level of the session goes from USER to CHEAT and back.
 * Only registered when the game asks for it (EngineConfig.setCheatCommand) and its build allows the cheats.
 * The developer mode (DEV) is not changed: the cheats are part of it.
 */
public class CheatsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "cheats";
    private final Commands commands;

    public CheatsGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.USER, "Turn the cheats on or off");
        this.commands = commands;
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("Tell if the cheats are on", context -> state())
                .then(literal("on", "Turn the cheats on").executes(context -> set(ECommandLevel.CHEAT)))
                .then(literal("off", "Turn the cheats off").executes(context -> set(ECommandLevel.USER)));
    }

    private CommandResult set(ECommandLevel level) {
        if (commands.getSessionLevel() == ECommandLevel.DEV)
            return CommandResult.error("Developer mode: the cheats are always on");

        commands.setSessionLevel(level);
        return state();
    }

    private CommandResult state() {
        boolean on = commands.getSessionLevel().includes(ECommandLevel.CHEAT);
        return CommandResult.ok(on ? "Cheats on" : "Cheats off",
                new JSONObject().put("level", commands.getSessionLevel().name()));
    }
}
