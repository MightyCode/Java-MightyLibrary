package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

import java.util.function.Consumer;

// Shortcuts of the command tests
public final class TestCommands {
    private TestCommands() {}

    // A command of the tests: namespace "test"
    public static Identifier Id(String path) {
        return Identifier.of("test", path);
    }

    // A command of the game: kept until the end, like in a real game
    public static void add(Commands commands, BaseCommand command) {
        commands.register(command, CommandOwner.GAME);
    }

    // A command of the tests declared by its syntax
    public static BaseCommand Syntax(String path, ECommandLevel level, String description, Consumer<CommandNode> syntax) {
        return new BaseCommand(Id(path), level, description) {
            @Override
            protected void syntax(CommandNode root) {
                syntax.accept(root);
            }
        };
    }
}
