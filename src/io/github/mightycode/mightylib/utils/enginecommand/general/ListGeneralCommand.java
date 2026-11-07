package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;

public abstract class ListGeneralCommand {
    public static void addCommands(Commands commands, SceneManager sceneManager){
        if (commands.getCommands().size() != 0) {
            System.err.println("Error, commands list is not empty.\n It must be empty to add general commands");
            return;
        }

        commands.addCommand(new ReloadGenCom());
        commands.addCommand(new WatchGenCom());
        commands.addCommand(new HelpGenCom(commands));
        commands.addCommand(new ExitGenCom(sceneManager));
        commands.addCommand(new ChangeSoundGainCom());
        commands.addCommand(new StatsGenCom());
        commands.addCommand(new ScreenshotGenCom());
        commands.addCommand(new SceneGenCom(sceneManager));
        commands.addCommand(new GLGenCom());
        commands.addCommand(new ResourcesGenCom());
        commands.addCommand(new InputGenCom());
        commands.addCommand(new GamepadGenCom());
        commands.addCommand(new CameraGenCom(sceneManager));
        commands.addCommand(new MonitorsGenCom());
        commands.addCommand(new WaitGenCom(commands, sceneManager));
        commands.addCommand(new ExecGenCom(commands));
    }
}
