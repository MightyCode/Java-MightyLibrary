package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandSource;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.enumOf;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.commandLine;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;

/**
 * "as user help": runs a line at a lower level, to check without the player what he sees (answers, errors, help).
 * The level can only go down: Min(asked, level of the source).
 */
public class AsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "as";
    private final Commands commands;

    public AsGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Run a command line at a lower level, to see what a player sees");
        this.commands = commands;
    }

    @Override
    protected void syntax(CommandNode root) {
        root.then(argument("level", enumOf(ECommandLevel.class), "Level of the line, never above the one of the source")
                .then(argument("line", commandLine(), "The command line, its options included")
                        .executes("Run the line at the level: its answer, error or help",
                                context -> commands.execute(context.get("line"),
                                        ICommandSource.Lowered(context.source(), context.get("level"))))));
    }
}
