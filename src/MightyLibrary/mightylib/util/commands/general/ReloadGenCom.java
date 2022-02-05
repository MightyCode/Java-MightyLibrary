package MightyLibrary.mightylib.util.commands.general;

import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.resources.Resources;
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
            switch (sub[1]) {
                case "texture":
                    System.out.println("Reload texture resources");

                    Resources.getInstance().reload(Texture.class);
                    break;
                case "shader":
                    System.out.println("Reload shader resources");

                    ShaderManager.getInstance().reload();
                    break;
                case "all":
                    System.out.println("Reload all resources");

                    Resources.getInstance().reload();
                    break;
                default:
                    unknownArgumentError(sub[1]);
                    break;
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
