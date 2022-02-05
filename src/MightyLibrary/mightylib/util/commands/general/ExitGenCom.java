package MightyLibrary.mightylib.util.commands.general;

import MightyLibrary.mightylib.util.commands.BaseCommand;
import MightyLibrary.mightylib.util.commands.Commands;

public class ExitGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "exit";
    private final Commands commands;

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
