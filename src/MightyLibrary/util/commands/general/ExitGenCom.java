package MightyLibrary.util.commands.general;

import MightyLibrary.util.commands.BaseCommand;
import MightyLibrary.util.commands.Commands;

public class ExitGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "exit";
    private Commands commands;

    public ExitGenCom(Commands command){
        super(COMMAND_NAME, GENERAL_COMMAND);
        this.commands = command;
    }

    @Override
    public void sendCommand(String command) {
        commands.cancelWritingCommand(this);
    }

    @Override
    public void help() {
        super.help();
        System.out.println("Exit the commands terminal");
    }
}
