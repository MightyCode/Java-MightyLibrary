package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;

public class HelpGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "help";
    private final Commands commands;

    public HelpGenCom(Commands commands) {
        super(COMMAND_NAME);
        this.commands = commands;
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        if (args.length != 2)
            return Collections.emptyList();

        List<String> names = new ArrayList<>();
        for (BaseCommand command : commands.getCommands())
            names.add(command.getName());
        return names;
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length > 1) {
            BaseCommand command = commands.findCommand(args[1]);

            if (command == null)
                return unknownArgumentError(args[1]);

            return CommandResult.ok(command.help());
        }

        StringBuilder result = new StringBuilder("Available commands :");
        for (BaseCommand command : commands.getCommands()) {
            result.append("\n- ").append(command.getName());
            if (commands.isSceneCommand(command))
                result.append(" (scene)");
        }

        result.append("\nWrite -> [command] help <- to know how to use a command");
        result.append("\nWrite a word with spaces between double quotes : screenshot \"my folder/a.png\"");

        return CommandResult.ok(result.toString());
    }

    @Override
    public String help(){
        return super.help() + "\nhelp : list all commands\nhelp [command] : display tips about the command";
    }
}
