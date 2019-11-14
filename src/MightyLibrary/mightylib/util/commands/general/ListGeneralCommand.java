package MightyLibrary.mightylib.util.commands.general;

import MightyLibrary.mightylib.util.commands.Commands;

public abstract class ListGeneralCommand {
    public static void addCommands(Commands commands){
        if (commands.getCommands().size() != 0) {
            System.err.println("Error, commands list is not empty.\n It must be empty to add general commands");
            return;
        }

        commands.addCommand(new ReloadGenCom());
        commands.addCommand(new HelpGenCom(commands));
        commands.addCommand(new ExitGenCom(commands));
    }
}