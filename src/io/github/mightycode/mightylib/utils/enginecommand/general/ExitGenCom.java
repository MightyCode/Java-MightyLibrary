package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.ListError;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;

public class ExitGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "exit";
    private final SceneManager sceneManager;

    public ExitGenCom(SceneManager sceneManager){
        super(COMMAND_NAME);
        this.sceneManager = sceneManager;
    }

    @Override
    public CommandResult execute(String[] args) {
        sceneManager.commandTarget().requestExit(ListError.NO_ERROR);

        return CommandResult.ok("Exiting the program");
    }

    @Override
    public String help() {
        return super.help() + "\nClose the program properly (scenes and resources are unloaded)";
    }
}
