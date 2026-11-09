package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.ListError;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;

public class ExitGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "exit";

    public ExitGenCom(){
        super(ListGeneralCommand.Id(COMMAND_NAME));
    }

    @Override
    public CommandResult execute(String[] args) {
        ListGeneralCommand.targetScenes().requestExit(ListError.NO_ERROR);

        return CommandResult.ok("Exiting the program");
    }

    @Override
    public String help() {
        return super.help() + "\nClose the program properly (scenes and resources are unloaded)";
    }
}
