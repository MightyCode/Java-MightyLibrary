package MightyLibrary.mightylib.util.enginecommand.general;

import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.util.enginecommand.BaseCommand;

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
            Class<?> result = Resources.getClassFromName(sub[1]);
            int incorrectlyReloaded = -1;

            if (result == Object.class){
                switch (sub[1].toLowerCase()) {
                    case "shader":
                        System.out.println("Reload shader resources");

                        ShaderManager.getInstance().reload();
                        break;
                    case "all":
                        System.out.println("Reload all resources");

                        incorrectlyReloaded = Resources.getInstance().reload();
                        break;
                    default:
                        unknownArgumentError(sub[1]);
                        break;
                }
            } else {
                System.out.println("Reload " + sub[1] + " resources");

                incorrectlyReloaded = Resources.getInstance().reload(result);
            }

            if (incorrectlyReloaded > 0)
                System.out.println(incorrectlyReloaded + " incorrectly reloaded.");

        } else {
            noEnoughArgumentError();
        }
    }


    public void help() {
        super.help();
        System.out.println("-> Resources type : Animation, Texture, Sound, Font");
        System.out.println("4 uses : ");
        System.out.println("1. reload [resources type] : reload certain type of resources");
        System.out.println("2. reload all : reload all resources / shader excluded");
        System.out.println("3. reload shader : reload all shaders");
        System.out.println("4. reload help : MightyLibrary.project.display help's tips about the command");
    }
}
