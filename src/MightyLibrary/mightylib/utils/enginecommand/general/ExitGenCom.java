package MightyLibrary.mightylib.utils.enginecommand.general;

import MightyLibrary.mightylib.main.ListError;
import MightyLibrary.mightylib.scenes.SceneManager;
import MightyLibrary.mightylib.utils.enginecommand.BaseCommand;
import MightyLibrary.mightylib.utils.enginecommand.CommandResult;

public class ExitGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "exit";
    private final SceneManager sceneManager;

    public ExitGenCom(SceneManager sceneManager){
        super(COMMAND_NAME, GENERAL_COMMAND);
        this.sceneManager = sceneManager;
    }

    @Override
    public CommandResult execute(String[] args) {
        sceneManager.requestExit(ListError.NO_ERROR);

        return CommandResult.ok("Exiting the program");
    }

    @Override
    public String help() {
        return super.help() + "\nClose the program properly (scenes and resources are unloaded)";
    }
}
