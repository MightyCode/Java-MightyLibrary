package MightyLibrary.util.commands.general;

import MightyLibrary.main.ManagerContainer;
import MightyLibrary.util.commands.BaseCommand;

public class ReloadGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "reload";

    public ReloadGenCom() {
        super(COMMAND_NAME);
    }

    @Override
    public void sendCommand(String command) {
        String[] sub = command.split(" ");
        if (sub[0].equals("texture")) {
            ManagerContainer manContainer = ManagerContainer.getInstance();
            manContainer.texManager.reload();
        } else if (sub[0].equals("shader")) {
            ManagerContainer manContainer = ManagerContainer.getInstance();
            manContainer.shadManager.reload();
        } else if (sub[0].equals("help")) {
            help();
        } else {
            error(command);
        }
    }

    @Override
    public void help() {
        System.out.println("Help for Reload General Command");
        System.out.println("3 uses : ");
        System.out.println("1. reload texture : refresh/reload all texture from the json textures in resources");
        System.out.println("2. reload shader : reload all shaders");
        System.out.println("3. reload help : display help's tips about the command");
    }
}
