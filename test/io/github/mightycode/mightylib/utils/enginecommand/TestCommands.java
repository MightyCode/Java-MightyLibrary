package io.github.mightycode.mightylib.utils.enginecommand;

// Shortcuts of the command tests
public final class TestCommands {
    private TestCommands() {}

    // A command of the game: kept until the end, like in a real game
    public static void add(Commands commands, BaseCommand command) {
        commands.register(command, CommandOwner.GAME);
    }
}
