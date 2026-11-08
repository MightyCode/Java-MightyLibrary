package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.resources.ResourceSelection;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;

public class ResourcesGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "resources";

    public ResourcesGenCom() {
        super(COMMAND_NAME);
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length > 1 && args[1].equals("batch")) {
            if (args.length < 3)
                return CommandResult.error("resources batch <name>");

            return CommandResult.ok("What the batch loads", Resources.getInstance().describeBatch(args[2]));
        }

        // For tests and debugging: load a batch in advance, then cancel it
        if (args.length > 1 && (args[1].equals("preload") || args[1].equals("unpreload"))) {
            if (args.length < 3)
                return CommandResult.error("resources " + args[1] + " <batch>");

            if (args[1].equals("preload"))
                Resources.getInstance().preloadBatch(args[2]);
            else
                Resources.getInstance().cancelPreloadBatch(args[2]);

            return CommandResult.ok(args[1] + " " + args[2]);
        }

        if (args.length > 1) {
            ResourceSelection selection;
            try {
                selection = ResourceSelection.parse(args, 1);
            } catch (IllegalArgumentException e) {
                return CommandResult.error(e.getMessage());
            }

            return CommandResult.ok("Detail of the selected data", Resources.getInstance().describe(selection));
        }

        return CommandResult.ok("Resources by type and state of the loading pipeline", Resources.getInstance().describe());
    }

    @Override
    public String help() {
        return super.help() +
                "\nresources : by type, number of loaded, preloaded and referenced data, names of the data loaded but not" +
                "\nreferenced (leak) or referenced but not loaded (loading or failed), and state of the loading pipeline." +
                "\nresources batch <name> : what the batch loads (known dependencies, loaded, estimated memory)." +
                "\nresources preload|unpreload <batch> : load a batch in advance (after what is wanted now), or cancel it." +
                "\nresources [type|all] [name1,name2] [re:regex] : detail of the selected data (state, version," +
                "\nreferences, sources), 100 at most.";
    }
}
