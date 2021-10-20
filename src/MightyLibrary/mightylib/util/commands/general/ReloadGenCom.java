package MightyLibrary.mightylib.util.commands.general;

import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.util.commands.BaseCommand;

public class ReloadGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "reload";

    public ReloadGenCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public void sendCommand(String command) {
        String[] sub = command.split(" ");
        if(checkArgumentHelp(sub)) return;

        if (sub.length > 1) {
            if (sub[1].equals("texture")) {
                System.out.println("Reload texture resources");

                ManagerContainer.getInstance().resources.reload(Texture.class);
            } else if (sub[1].equals("shader")) {
                System.out.println("Reload shader resources");

                ManagerContainer.getInstance().shadManager.reload();
            } else if (sub[1].equals("all")) {
                System.out.println("Reload all resources");

                ManagerContainer.getInstance().resources.reload();
            } else {
                unknownArgumentError(sub[1]);
            }
        } else {
            noEnoughArgumentError();
        }
    }


    public void help() {
        super.help();
        System.out.println("3 uses : ");
        System.out.println("1. reload texture : refresh/reload all texture from the json textures in resources");
        System.out.println("2. reload shader : reload all shaders");
        System.out.println("3. reload help : display help's tips about the command");
    }
}
