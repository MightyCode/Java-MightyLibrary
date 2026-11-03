package MightyLibrary.mightylib.utils.enginecommand.general;

import MightyLibrary.mightylib.utils.enginecommand.BaseCommand;
import MightyLibrary.mightylib.utils.enginecommand.CommandResult;
import MightyLibrary.mightylib.utils.enginecommand.Commands;

public class HelpGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "help";
    private final Commands commands;

    public HelpGenCom(Commands commands) {
        super(COMMAND_NAME, GENERAL_COMMAND);
        this.commands = commands;
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
        for (BaseCommand command : commands.getCommands())
            result.append("\n- ").append(command.getName());

        result.append("\nWrite -> [command] help <- to know how to use a command");

        return CommandResult.ok(result.toString());
    }

    @Override
    public String help(){
        return super.help() + "\nhelp : list all commands\nhelp [command] : display tips about the command";
    }
}
