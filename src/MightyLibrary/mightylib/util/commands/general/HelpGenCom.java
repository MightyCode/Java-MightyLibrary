package MightyLibrary.mightylib.util.commands.general;

import MightyLibrary.mightylib.util.commands.BaseCommand;
import MightyLibrary.mightylib.util.commands.Commands;

import java.util.ArrayList;

public class HelpGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "help";
    private Commands commands;

    public HelpGenCom(Commands commands) {
        super(COMMAND_NAME, GENERAL_COMMAND);
        this.commands = commands;
    }

    @Override
    public void sendCommand(String command) {
        String[] sub = command.split(" ");
        if(checkArgumentHelp(sub)) return;

        if(sub.length > 1) {
            ArrayList<BaseCommand> listCommands = this.commands.getCommands();
            boolean found = false;
            int i = 0;
            while (!found && i < listCommands.size()) {
                if (listCommands.get(i).isCommand(sub[1])) {
                    listCommands.get(i).help();
                    found = true;
                }
                ++i;;
            }

            if (!found) {
                unknownArgumentError(sub[1]);
            }
        } else {
            help();
        }
    }

    @Override
    public void help(){
        super.help();
        System.out.println("TODO");
    }
}
