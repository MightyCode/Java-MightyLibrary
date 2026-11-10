package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.BatchResources;
import io.github.mightycode.mightylib.resources.ResourceArguments;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.ArgumentType;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.suggest;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class ResourcesGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "resources";

    public ResourcesGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Resources by type and state of the loading pipeline");
    }

    @Override
    protected void syntax(CommandNode root) {
        ArgumentType<String> batches = suggest(() -> Views.engine().resources().getResourceNames(BatchResources.class));

        root.executes("By type, number of loaded, preloaded and referenced data, names of the data loaded but not"
                        + " referenced (leak) or referenced but not loaded (loading or failed), and state of the loading pipeline",
                        context -> CommandResult.ok("Resources by type and state of the loading pipeline",
                                Views.engine().resources().describe()))
                .then(literal("batch", "What a batch loads (known dependencies, loaded, estimated memory)")
                        .then(argument("name", batches, "Name of the batch")
                                .executes(context -> CommandResult.ok("What the batch loads",
                                        Views.engine().resources().describeBatch(context.get("name"))))))
                // For tests and debugging: load a batch in advance, then cancel it
                .then(literal("preload", "Load a batch in advance (after what is wanted now)")
                        .then(argument("batch", batches, "Name of the batch").executes(context -> {
                            String batch = context.get("batch");
                            Views.engine().resources().preloadBatch(batch);
                            return CommandResult.ok("preload " + batch);
                        })))
                .then(literal("unpreload", "Cancel the preload of a batch")
                        .then(argument("batch", batches, "Name of the batch").executes(context -> {
                            String batch = context.get("batch");
                            Views.engine().resources().cancelPreloadBatch(batch);
                            return CommandResult.ok("unpreload " + batch);
                        })))
                .then(ResourceArguments.selection(() -> Views.engine().resources(),
                        "all or the name of a resource type: detail of the selected data (state, version, references,"
                                + " sources), 100 at most",
                        selection -> CommandResult.ok("Detail of the selected data",
                                Views.engine().resources().describe(selection))));
    }
}
