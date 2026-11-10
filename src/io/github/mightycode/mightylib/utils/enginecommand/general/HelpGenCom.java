package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.ArgumentType;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

import java.util.Collection;
import java.util.List;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;

public class HelpGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "help";
    private final Commands commands;

    public HelpGenCom(Commands commands) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.USER, "List the commands, or tell how to use one");
        this.commands = commands;
    }

    // The name or the identifier of a visible command, completed from the commands the source sees
    private ArgumentType<String> commandType() {
        return new ArgumentType<>() {
            @Override
            public String parse(String word, CommandContext context) {
                return word;
            }

            @Override
            public Collection<String> complete(String start, CommandContext context) {
                return commands.commandNames(start);
            }

            @Override
            public String describe() {
                return "command";
            }
        };
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("List the commands the source can use", context -> list(context.currentLevel()))
                .then(argument("command", commandType(), "Name of the command, or its identifier (namespace:name)")
                        .executes("The same as <command> --help: its description, its usage and its options", context -> {
                            String name = context.get("command");
                            List<BaseCommand> matching = commands.matchCommands(name);

                            if (matching.isEmpty())
                                return CommandResult.error("Unknown command ->" + name + "<-");
                            // The same command in several windows: the same help
                            if (matching.size() > 1 && !Commands.SameIdentifier(matching))
                                return CommandResult.error("Ambiguous command ->" + name + "<-, write its identifier (namespace:name)");

                            // The generated help and its JSON, the same as "<cmd> --help"
                            return matching.get(0).helpResult();
                        }));
    }

    private CommandResult list(ECommandLevel level) {
        StringBuilder result = new StringBuilder("Available commands :");
        for (BaseCommand command : commands.getVisibleCommands()) {
            result.append("\n- ").append(command.getId()).append(" (").append(command.getOwner()).append(')');
            if (command.getDescription() != null)
                result.append(" : ").append(command.getDescription());
        }

        result.append("\nWrite a command by its name alone (help) unless another command has it (mightylib:help)");
        result.append("\nWrite -> help [command] <- or -> [command] --help <- to know how to use a command");
        if (level.includes(ECommandLevel.DEV))
            result.append("\nWrite a word with spaces between double quotes : screenshot \"my folder/a.png\"");

        return CommandResult.ok(result.toString());
    }
}
