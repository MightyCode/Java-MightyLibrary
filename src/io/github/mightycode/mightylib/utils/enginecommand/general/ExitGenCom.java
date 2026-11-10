package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.ListError;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

public class ExitGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "exit";

    public ExitGenCom(){
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Close the program properly (scenes and resources are unloaded)");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes(context -> {
            ListGeneralCommand.targetScenes().requestExit(ListError.NO_ERROR);

            return CommandResult.ok("Exiting the program");
        });
    }
}
