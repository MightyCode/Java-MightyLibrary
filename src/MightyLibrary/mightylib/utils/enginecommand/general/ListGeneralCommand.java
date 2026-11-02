package MightyLibrary.mightylib.utils.enginecommand.general;

import MightyLibrary.mightylib.scenes.SceneManager;
import MightyLibrary.mightylib.utils.enginecommand.Commands;

public abstract class ListGeneralCommand {
    public static void addCommands(Commands commands, SceneManager sceneManager){
        if (commands.getCommands().size() != 0) {
            System.err.println("Error, commands list is not empty.\n It must be empty to add general commands");
            return;
        }

        commands.addCommand(new ReloadGenCom());
        commands.addCommand(new HelpGenCom(commands));
        commands.addCommand(new ExitGenCom(sceneManager));
        commands.addCommand(new ChangeSoundGainCom());
        commands.addCommand(new StatsGenCom());
        commands.addCommand(new ScreenshotGenCom());
        commands.addCommand(new SceneGenCom(sceneManager));
    }
}
