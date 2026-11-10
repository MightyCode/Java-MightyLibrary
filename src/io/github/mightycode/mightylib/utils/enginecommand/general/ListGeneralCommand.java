package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.utils.enginecommand.CommandRegistry;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;

public abstract class ListGeneralCommand {
    /**
     * The general commands, owned by the engine.
     * @param cheatCommand the command "cheats on|off" (the build allows the cheats and the game wants the command)
     */
    public static void addCommands(CommandRegistry registry, Commands commands, boolean cheatCommand){
        registry.register(new ReloadGenCom());
        registry.register(new WatchGenCom());
        registry.register(new HelpGenCom(commands));
        registry.register(new ExitGenCom());
        registry.register(new ChangeSoundGainCom());
        registry.register(new StatsGenCom());
        registry.register(new ScreenshotGenCom());
        registry.register(new SceneGenCom());
        registry.register(new GLGenCom());
        registry.register(new TextureUploadGenCom());
        registry.register(new ResourcesGenCom());
        registry.register(new InputGenCom());
        registry.register(new GamepadGenCom());
        registry.register(new CameraGenCom());
        registry.register(new MonitorsGenCom());
        registry.register(new WindowsGenCom());
        registry.register(new WindowGenCom());
        registry.register(new WaitGenCom(commands));
        registry.register(new ExecGenCom(commands));
        registry.register(new ConsoleGenCom(commands));

        if (cheatCommand)
            registry.register(new CheatsGenCom(commands));
    }

    // Identifier of a general command
    static Identifier Id(String path) {
        return Identifier.of(Identifier.ENGINE_NAMESPACE, path);
    }

    // The scenes of the context targeted by the commands (see Engine.commandWindow)
    static SceneManager targetScenes() {
        return Views.engine().commandContext().getSceneManager();
    }
}
