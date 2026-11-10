package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.sounds.SoundManager;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.decimal;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.word;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class ChangeSoundGainCom extends BaseCommand {
    public static final String COMMAND_NAME = "changegain";

    public ChangeSoundGainCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV, "Change the gain of a gain node");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.then(literal("list", "List all of the gain nodes and their value").executes(context -> {
                    Views.engine().soundManager().printGainTree();

                    return CommandResult.ok("Gain tree printed in the program console");
                }))
                .then(argument("node", word(), "Name of the gain node (see list)")
                        .then(argument("value", decimal(), "New gain")
                                .executes("Change the gain of a gain node by the written value", context -> {
                                    String node = context.get("node");
                                    double value = context.get("value");

                                    if (!Views.engine().soundManager().changeGain(node, (float) value))
                                        return CommandResult.error("Unknown node called " + node);

                                    return CommandResult.ok("Update " + node + " node to a new gain of " + value);
                                })));
    }
}
