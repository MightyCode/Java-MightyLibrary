package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HelpGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "help";
    private final Commands commands;

    public HelpGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.USER);
        this.commands = commands;
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        if (args.length != 2)
            return Collections.emptyList();

        return commands.commandNames(args[1]);
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length > 1) {
            List<BaseCommand> matching = commands.matchCommands(args[1]);

            if (matching.isEmpty())
                return unknownArgumentError(args[1]);
            // The same command in several windows: the same help
            if (matching.size() > 1 && !Commands.SameIdentifier(matching))
                return CommandResult.error("Ambiguous command ->" + args[1] + "<-, write its identifier (namespace:name)");

            // The generated help and its JSON, the same as "<cmd> --help"
            return matching.get(0).helpResult();
        }

        StringBuilder result = new StringBuilder("Available commands :");
        for (BaseCommand command : commands.getVisibleCommands()) {
            result.append("\n- ").append(command.getId()).append(" (").append(command.getOwner()).append(')');
            if (command.getDescription() != null)
                result.append(" : ").append(command.getDescription());
        }

        result.append("\nWrite a command by its name alone (help) unless another command has it (mightylib:help)");
        result.append("\nWrite -> help [command] <- or -> [command] --help <- to know how to use a command");
        if (hasLevel(ECommandLevel.DEV))
            result.append("\nWrite a word with spaces between double quotes : screenshot \"my folder/a.png\"");

        return CommandResult.ok(result.toString());
    }

    @Override
    public String help(){
        return super.help() + "\nhelp : list all commands\nhelp [command] : display tips about the command (as [command] --help)";
    }
}
