package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Let the player turn the cheats on and off: the level of the session goes from USER to CHEAT and back.
 * Only registered when the game asks for it (EngineConfig.setCheatCommand) and its build allows the cheats.
 * The developer mode (DEV) is not changed: the cheats are part of it.
 */
public class CheatsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "cheats";
    private final Commands commands;

    public CheatsGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.USER);
        this.commands = commands;
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        return args.length == 2 ? Arrays.asList("on", "off") : Collections.emptyList();
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length > 1) {
            if (commands.getSessionLevel() == ECommandLevel.DEV)
                return CommandResult.error("Developer mode: the cheats are always on");

            if (args[1].equalsIgnoreCase("on"))
                commands.setSessionLevel(ECommandLevel.CHEAT);
            else if (args[1].equalsIgnoreCase("off"))
                commands.setSessionLevel(ECommandLevel.USER);
            else
                return unknownArgumentError(args[1]);
        }

        boolean on = commands.getSessionLevel().includes(ECommandLevel.CHEAT);
        return CommandResult.ok(on ? "Cheats on" : "Cheats off",
                new JSONObject().put("level", commands.getSessionLevel().name()));
    }

    @Override
    public String help() {
        return super.help() + "\ncheats : tell if the cheats are on\ncheats on|off : turn the cheats on or off";
    }
}
