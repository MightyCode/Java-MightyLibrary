package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.utils.enginecommand.CommandLine;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.*;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;
import static org.junit.jupiter.api.Assertions.*;

// Completion, help and JSON generated from the syntax
class CommandHelpTest {
    private static final ICommandHandler OK = context -> CommandResult.ok("ok");

    // spawn <count> [--color] [--size] [--at x y], spawn clear, spawn list (DEV)
    private static CommandParser Spawn() {
        CommandNode root = CommandNode.root("spawn", "Spawn shapes")
                .then(argument("count", integer(1, 100), "Number of shapes")
                        .option(Option.of("color", "Color of the shapes").value("color", choice("red", "green", "blue")))
                        .option(Option.of("size", "Size of the shapes").value("size", decimal(0.5, 4)).defaultValue(1.0))
                        .option(Option.of("at", "Where").value("x", decimal()).value("y", decimal()))
                        .option(Option.of("secret", "Hidden option").level(ECommandLevel.DEV))
                        .executes("Spawn shapes at random", OK))
                .then(literal("clear", "Remove the shapes").executes(OK))
                .then(literal("list", "List the shapes").level(ECommandLevel.DEV).executes(OK));

        return new CommandParser(root);
    }

    private static CommandContext Context(ECommandLevel level) {
        return new CommandContext("spawn", level, ICommandSource.API);
    }

    // Candidates for the beginning of the line typed after "spawn ", sorted like the console
    private static List<String> Complete(CommandParser parser, String beginning, ECommandLevel level) {
        String[] words = CommandLine.SplitPartial(beginning).getWords();
        String typed = words[words.length - 1];
        return new ArrayList<>(io.github.mightycode.mightylib.utils.enginecommand.CommandCompletion
                .Filter(typed, parser.complete(words, Context(level))));
    }

    @Test
    void candidatesByPosition() {
        CommandParser parser = Spawn();

        assertEquals(List.of("clear", "list"), Complete(parser, "", ECommandLevel.DEV));
        assertEquals(List.of("clear"), Complete(parser, "c", ECommandLevel.DEV));
        // Nothing after a branch that ends
        assertEquals(List.of(), Complete(parser, "clear ", ECommandLevel.DEV));
        // A word that can't be read: no branch, nothing
        assertEquals(List.of(), Complete(parser, "abc ", ECommandLevel.DEV));
    }

    @Test
    void optionsAfterADash() {
        CommandParser parser = Spawn();

        assertEquals(List.of("--at", "--color", "--help", "--secret", "--size"), Complete(parser, "5 -", ECommandLevel.DEV));
        assertEquals(List.of("--color"), Complete(parser, "5 --c", ECommandLevel.DEV));
        // Not the ones already given
        assertEquals(List.of("--at", "--help", "--secret", "--size"), Complete(parser, "5 --color red -", ECommandLevel.DEV));
        // Before their node: only --help
        assertEquals(List.of("--help"), Complete(parser, "-", ECommandLevel.DEV));
    }

    @Test
    void valuesOfAnOption() {
        CommandParser parser = Spawn();

        assertEquals(List.of("blue", "green", "red"), Complete(parser, "5 --color ", ECommandLevel.USER));
        assertEquals(List.of("--color=red"), Complete(parser, "5 --color=r", ECommandLevel.USER));
        // The second value of --at: no completion, then the next positional place
        assertEquals(List.of(), Complete(parser, "5 --at 1 ", ECommandLevel.USER));
    }

    @Test
    void nothingHiddenLeaks() {
        CommandParser parser = Spawn();

        assertEquals(List.of("clear"), Complete(parser, "", ECommandLevel.USER));
        assertEquals(List.of("--at", "--color", "--help", "--size"), Complete(parser, "5 -", ECommandLevel.USER));
        assertEquals(List.of(), Complete(parser, "5 --secret ", ECommandLevel.USER));

        String help = parser.helpText("demo:spawn", Context(ECommandLevel.USER));
        assertFalse(help.contains("list"), help);
        assertFalse(help.contains("secret"), help);

        String json = parser.helpJson("demo:spawn", ECommandLevel.CHEAT, Context(ECommandLevel.USER)).toString();
        assertFalse(json.contains("list"), json);
        assertFalse(json.contains("secret"), json);
    }

    @Test
    void helpGivesTheUsageLinesAndDescriptions() {
        String help = Spawn().helpText("demo:spawn", Context(ECommandLevel.DEV));

        assertEquals("demo:spawn : Spawn shapes\n"
                + "Usage :\n"
                + "  spawn <count> [--color <red|green|blue>] [--size <size>] [--at <x> <y>] [--secret] : Spawn shapes at random\n"
                + "  spawn clear : Remove the shapes\n"
                + "  spawn list : List the shapes\n"
                + "Arguments :\n"
                + "  <count> : Number of shapes (integer 1..100)\n"
                + "Options :\n"
                + "  --color <red|green|blue> : Color of the shapes\n"
                + "  --size <size> : Size of the shapes (default 1)\n"
                + "  --at <x> <y> : Where\n"
                + "  --secret : Hidden option\n"
                + "  --help : this help", help);
    }

    @Test
    void helpJsonIsTheVisibleTree() {
        JSONObject json = Spawn().helpJson("demo:spawn", ECommandLevel.CHEAT, Context(ECommandLevel.USER));

        assertEquals("demo:spawn", json.getString("id"));
        assertEquals("cheat", json.getString("level"));
        assertEquals("Spawn shapes", json.getString("description"));

        JSONArray branches = json.getJSONArray("branches");
        assertEquals(2, branches.length());

        JSONObject spawn = branches.getJSONObject(0);
        assertEquals("spawn <count> [--color <red|green|blue>] [--size <size>] [--at <x> <y>]", spawn.getString("usage"));
        assertEquals("Spawn shapes at random", spawn.getString("description"));

        JSONObject count = spawn.getJSONArray("arguments").getJSONObject(0);
        assertEquals("count", count.getString("name"));
        assertEquals("integer 1..100", count.getString("type"));
        assertFalse(count.getBoolean("optional"));
        assertFalse(count.getBoolean("variadic"));

        JSONArray options = spawn.getJSONArray("options");
        assertEquals(3, options.length());
        JSONObject color = options.getJSONObject(0);
        assertEquals("color", color.getString("name"));
        assertEquals(List.of("red", "green", "blue"), color.getJSONArray("values").getJSONObject(0).getJSONArray("values").toList());
        assertFalse(color.getBoolean("repeatable"));
        assertEquals(1.0, options.getJSONObject(1).getDouble("default"));
        assertEquals(2, options.getJSONObject(2).getJSONArray("values").length());

        assertEquals("spawn clear", branches.getJSONObject(1).getString("usage"));
    }

    @Test
    void helpCommandResultHasTheTextAndTheJson() {
        CommandResult result = Spawn().help("demo:spawn", ECommandLevel.CHEAT, Context(ECommandLevel.DEV));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().startsWith("demo:spawn : Spawn shapes"));
        assertEquals(3, result.getData().getJSONArray("branches").length());
    }
}
