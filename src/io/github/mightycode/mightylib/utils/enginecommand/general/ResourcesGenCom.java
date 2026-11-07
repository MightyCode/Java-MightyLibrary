package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;

public class ResourcesGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "resources";

    public ResourcesGenCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public CommandResult execute(String[] args) {
        return CommandResult.ok("Resources by type and state of the loading pipeline", Resources.getInstance().describe());
    }

    @Override
    public String help() {
        return super.help() +
                "\nresources : by type, number of loaded, preloaded and referenced data, names of the data loaded but not" +
                "\nreferenced (leak) or referenced but not loaded (loading or failed), and state of the loading pipeline.";
    }
}
