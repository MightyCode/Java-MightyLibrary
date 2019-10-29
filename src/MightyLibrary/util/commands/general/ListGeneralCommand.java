package MightyLibrary.util.commands.general;

import MightyLibrary.util.commands.BaseCommand;

import java.util.ArrayList;

public abstract class ListGeneralCommand {
    public static void addCommand(ArrayList<BaseCommand> commands){
        if (commands.size() != 0) {
            System.err.println("Error, commands list is not empty.\n It must be empty to add general commands");
            return;
        }

        commands.add(new ReloadGenCom());
    }
}
