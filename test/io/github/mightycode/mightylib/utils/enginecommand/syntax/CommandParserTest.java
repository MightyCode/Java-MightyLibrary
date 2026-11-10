package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.utils.enginecommand.CommandLine;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.*;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;
import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {
    // Answers the name of the branch
    private static ICommandHandler Branch(String name) {
        return context -> CommandResult.ok(name);
    }

    private static CommandNode Root() {
        return CommandNode.root("cmd", "A command of the tests");
    }

    private static CommandContext Context() {
        return new CommandContext("cmd", ECommandLevel.DEV, ICommandSource.API);
    }

    // The parsed line, words after the name of the command
    private static CommandParser.Parsed Parse(CommandNode root, String line) {
        return new CommandParser(root).parse(CommandLine.Split(line), Context());
    }

    // The name of the branch run
    private static String Run(CommandNode root, String line) {
        CommandParser.Parsed parsed = Parse(root, line);
        assertFalse(parsed.isHelp(), "help instead of a branch for ->" + line + "<-");
        return parsed.getHandler().run(parsed.getContext()).getMessage();
    }

    private static CommandContext Values(CommandNode root, String line) {
        return Parse(root, line).getContext();
    }

    private static String Error(CommandNode root, String line) {
        return assertThrows(CommandSyntaxException.class, () -> Parse(root, line)).getMessage();
    }

    @Test
    void literalsIgnoreCase() {
        CommandNode root = Root().then(literal("mode").then(literal("poll").executes(Branch("poll"))));

        assertEquals("poll", Run(root, "MODE Poll"));
        assertTrue(Error(root, "mode push").startsWith("Unknown argument ->push<-"));
    }

    @Test
    void literalIsTriedBeforeTheArgument() {
        CommandNode root = Root()
                .then(argument("name", word()).executes(Branch("name")))
                .then(literal("clear").executes(Branch("clear")));

        assertEquals("clear", Run(root, "clear"));
        assertEquals("name", Run(root, "other"));
    }

    @Test
    void argumentsAreTriedInTheirOrder() {
        CommandNode root = Root()
                .then(argument("count", integer(1, 10)).executes(Branch("count")))
                .then(argument("name", word()).executes(Branch("name")));

        assertEquals("count", Run(root, "5"));
        assertEquals("name", Run(root, "50"));
    }

    @Test
    void integersAndTheirBounds() {
        CommandNode root = Root().then(argument("count", integer(1, 100)).executes(Branch("count")));

        assertEquals(100, (int) Values(root, "100").get("count"));
        assertTrue(Error(root, "abc").startsWith("Expected an integer between 1 and 100 for <count>, got ->abc<-"));
        assertTrue(Error(root, "101").startsWith("Expected an integer between 1 and 100 for <count>, got ->101<-"));
        assertTrue(Error(root, "0").contains("Usage :\n  cmd <count>"));

        CommandNode unbounded = Root().then(argument("n", integer()).executes(Branch("n")));
        assertEquals(-5, (int) Values(unbounded, "-5").get("n"));
        assertTrue(Error(unbounded, "1.5").startsWith("Expected an integer for <n>"));

        CommandNode longs = Root().then(argument("ms", longValue(1, Long.MAX_VALUE)).executes(Branch("ms")));
        assertEquals(5_000_000_000L, (long) Values(longs, "5000000000").get("ms"));
        assertTrue(Error(longs, "0").startsWith("Expected an integer >= 1 for <ms>"));
    }

    @Test
    void decimalsAndTheirBounds() {
        CommandNode root = Root().then(argument("size", decimal(0.5, 4)).executes(Branch("size")));

        assertEquals(0.5, (double) Values(root, "0.5").get("size"));
        assertEquals(2.0, (double) Values(root, "2").get("size"));
        assertTrue(Error(root, "5").startsWith("Expected a number between 0.5 and 4 for <size>"));
        assertTrue(Error(root, "NaN").startsWith("Expected a number"));
        assertTrue(Error(root, "1f").startsWith("Expected a number"));
    }

    @Test
    void wordsChoicesAndEnums() {
        CommandNode root = Root()
                .then(literal("b").then(argument("on", bool()).executes(Branch("b"))))
                .then(literal("c").then(argument("mode", choice("poll", "service")).executes(Branch("c"))))
                .then(literal("e").then(argument("level", enumOf(ECommandLevel.class)).executes(Branch("e"))))
                .then(literal("w").then(argument("word", word()).executes(Branch("w"))));

        assertEquals(true, Values(root, "b TRUE").get("on"));
        assertTrue(Error(root, "b yes").startsWith("Expected true or false for <on>"));
        // The declared spelling
        assertEquals("service", Values(root, "c SERVICE").get("mode"));
        assertTrue(Error(root, "c push").startsWith("Expected one of : poll, service for <mode>, got ->push<-"));
        assertEquals(ECommandLevel.CHEAT, Values(root, "e Cheat").get("level"));
        assertTrue(Error(root, "e admin").startsWith("Expected one of : user, cheat, dev for <level>"));
        assertEquals("a b", Values(root, "w \"a b\"").get("word"));
    }

    @Test
    void dynamicValuesAreStrictAndSuggestionsFree() {
        List<String> cameras = Arrays.asList("main2D", "minimap");
        CommandNode root = Root()
                .then(literal("one").then(argument("camera", oneOf(() -> cameras)).executes(Branch("one"))))
                .then(literal("any").then(argument("name", suggest(() -> cameras)).executes(Branch("any"))));

        assertEquals("minimap", Values(root, "one MINIMAP").get("camera"));
        assertTrue(Error(root, "one other").startsWith("Expected one of : main2D, minimap for <camera>"));
        assertEquals("other", Values(root, "any other").get("name"));
    }

    @Test
    void identifiersAndPaths() {
        CommandNode root = Root()
                .then(literal("id").then(argument("id", identifier()).executes(Branch("id"))))
                .then(literal("file").then(argument("file", path("screenshots")).executes(Branch("file"))));

        assertEquals(Identifier.of("demo", "enter"), Values(root, "id demo:Enter").get("id"));
        assertTrue(Error(root, "id enter").startsWith("Expected an identifier (namespace:path) for <id>"));
        assertEquals(Paths.get("screenshots", "a.png"), Values(root, "file a.png").get("file"));
        // An absolute path stays absolute
        Path absolute = Paths.get("a.png").toAbsolutePath();
        assertEquals(absolute, Values(root, "file \"" + absolute + "\"").get("file"));
    }

    @Test
    void optionalArgumentCanBeLeftOut() {
        CommandNode root = Root().then(literal("wait")
                .then(argument("ticks", integer(1, 100)).optional().executes(Branch("wait"))));

        assertEquals(1, (int) Values(root, "wait").getOr("ticks", 1));
        assertFalse(Values(root, "wait").has("ticks"));
        assertEquals(5, (int) Values(root, "wait 5").get("ticks"));
    }

    @Test
    void variadicTakesEveryFollowingWord() {
        CommandNode root = Root().then(literal("names").then(argument("names", word()).variadic().executes(Branch("names"))))
                .then(literal("maybe").then(argument("names", word()).variadic().optional().executes(Branch("maybe"))));

        assertEquals(List.of("a", "b", "c"), Values(root, "names a b c").getList("names"));
        assertTrue(Error(root, "names").startsWith("Incomplete command\nUsage :\n  cmd names <names>..."));
        assertEquals(List.of(), Values(root, "maybe").getList("names"));
        assertEquals(List.of("a"), Values(root, "maybe a").getList("names"));
    }

    @Test
    void textTakesTheRestOfTheLine() {
        CommandNode root = Root().option(Option.of("as", "Level").value("level", enumOf(ECommandLevel.class)))
                .then(argument("line", text()).executes(Branch("text")));

        // One word as it is (its quotes removed), several joined to split the same way
        assertEquals("spawn ", Values(root, "\"spawn \"").get("line"));
        assertEquals("screenshot \"a b.png\" --rect 0 0 1 1 -- x", Values(root, "screenshot \"a b.png\" --rect 0 0 1 1 -- x").get("line"));
        // Options before it are options
        CommandContext context = Values(root, "--as user help --help");
        assertEquals(ECommandLevel.USER, context.get("as"));
        assertEquals("help --help", context.get("line"));
    }

    @Test
    void optionsWithValues() {
        CommandNode root = Root()
                .option(Option.of("scale", "Scale").value("factor", decimal(0.1, 1)).defaultValue(1.0))
                .option(Option.of("rect", "Zone").value("x", integer()).value("y", integer()).value("w", integer()).value("h", integer()))
                .option(Option.of("quiet", "No message"))
                .then(argument("path", word()).optional().executes(Branch("shot")));

        CommandContext context = Values(root, "--scale 0.5 a.png --rect 0 0 10 20");
        assertEquals(0.5, (double) context.get("scale"));
        assertEquals("a.png", context.get("path"));
        assertEquals(List.of(0, 0, 10, 20), context.get("rect"));
        assertFalse(context.has("quiet"));

        context = Values(root, "--scale=0.25 --quiet");
        assertEquals(0.25, (double) context.get("scale"));
        assertTrue(context.has("quiet"));
        assertEquals(true, context.get("quiet"));
        assertFalse(context.has("path"));

        // Default value, not given
        context = Values(root, "");
        assertEquals(1.0, (double) context.get("scale"));
        assertFalse(context.has("scale"));

        // Case of the name ignored
        assertTrue(Values(root, "--QUIET").has("quiet"));
    }

    @Test
    void optionErrors() {
        CommandNode root = Root()
                .option(Option.of("scale", "Scale").value("factor", decimal(0.1, 1)))
                .option(Option.of("rect", "Zone").value("x", integer()).value("y", integer()).excludes("corners"))
                .option(Option.of("corners", "Zone").value("x", integer()).value("y", integer()))
                .option(Option.of("quiet", "No message"))
                .option(Option.of("tag", "Tag").value("tag", word()).repeatable())
                .executes(Branch("shot"));

        assertTrue(Error(root, "--zoom 2").startsWith("Unknown option ->--zoom<-\nUsage :\n  cmd [--scale <factor>]"));
        assertTrue(Error(root, "--scale 0.5 --scale 0.2").startsWith("The option --scale is given twice"));
        assertTrue(Error(root, "--scale").startsWith("The option --scale needs its values : --scale <factor>"));
        assertTrue(Error(root, "--rect 1 --quiet").startsWith("The option --rect needs its values : --rect <x> <y>"));
        assertTrue(Error(root, "--scale 2").startsWith("Expected a number between 0.1 and 1 for --scale <factor>, got ->2<-"));
        assertTrue(Error(root, "--quiet=yes").startsWith("The option --quiet takes no value"));
        assertTrue(Error(root, "--rect 0 0 --corners 1 1").startsWith("The options --rect and --corners can't be given together"));
        assertTrue(Error(root, "--corners 0 0 --rect 1 1").startsWith("The options --corners and --rect can't be given together"));

        assertEquals(List.of("a", "b"), Values(root, "--tag a --tag b").getList("tag"));
    }

    @Test
    void optionIsValidInTheSubtreeOfItsNode() {
        CommandNode root = Root()
                .then(literal("mode").then(argument("mode", choice("poll", "service"))
                        .option(Option.of("interval", "Time").value("ms", longValue(1, 60000)).defaultValue(1000L))
                        .executes(Branch("mode"))))
                .then(literal("off").executes(Branch("off")));

        assertEquals(500L, (long) Values(root, "mode poll --interval 500").get("interval"));
        assertEquals(1000L, (long) Values(root, "mode poll").get("interval"));
        // Before its node, or in another branch: unknown
        assertTrue(Error(root, "--interval 500 mode poll").startsWith("Unknown option ->--interval<-"));
        assertTrue(Error(root, "off --interval 5").startsWith("Unknown option ->--interval<-"));
    }

    @Test
    void optionCanBeAnywhereAfterItsNode() {
        CommandNode root = Root().option(Option.of("at", "Position").value("x", decimal()).value("y", decimal()))
                .then(argument("count", integer(1, 100)).then(argument("names", word()).variadic().executes(Branch("spawn"))));

        CommandContext context = Values(root, "3 a --at -1.5 2 b");
        assertEquals(3, (int) context.get("count"));
        assertEquals(List.of("a", "b"), context.getList("names"));
        assertEquals(List.of(-1.5, 2.0), context.get("at"));
    }

    @Test
    void endOfOptionsMakesTheRestPositional() {
        CommandNode root = Root().option(Option.of("quiet", "No message"))
                .then(argument("names", word()).variadic().executes(Branch("names")));

        CommandContext context = Values(root, "a -- --quiet --help");
        assertEquals(List.of("a", "--quiet", "--help"), context.getList("names"));
        assertFalse(context.has("quiet"));
    }

    @Test
    void branchWithoutHandlerIsIncomplete() {
        CommandNode root = Root().then(literal("mode")
                .then(literal("poll").executes(Branch("poll")))
                .then(literal("service").executes(Branch("service"))));

        assertEquals("Incomplete command\nUsage :\n  cmd mode poll\n  cmd mode service", Error(root, "mode"));
    }

    @Test
    void helpIsAskedAnywhereEvenAfterAnError() {
        CommandNode root = Root().then(argument("count", integer(1, 100)).executes(Branch("spawn")));

        assertTrue(Parse(root, "--help").isHelp());
        assertTrue(Parse(root, "5 --HELP").isHelp());
        assertTrue(Parse(root, "500 --help").isHelp());
        assertNull(Parse(root, "--help").getHandler());
    }

    @Test
    void mistakesOfTheSyntaxAreFoundAtOnce() {
        assertThrows(IllegalStateException.class, () -> new CommandParser(Root()
                .then(argument("names", word()).variadic().then(literal("x").executes(Branch("x"))))));
        assertThrows(IllegalStateException.class, () -> new CommandParser(Root()
                .then(argument("a", word()).optional().then(literal("x").executes(Branch("x"))))));
        assertThrows(IllegalStateException.class, () -> new CommandParser(Root().then(literal("dead"))));
        assertThrows(IllegalStateException.class, () -> new CommandParser(Root()
                .then(literal("a").executes(Branch("a"))).then(literal("A").executes(Branch("A")))));
        assertThrows(IllegalStateException.class, () -> new CommandParser(Root()
                .option(Option.of("n", "N").value("n", word()))
                .then(argument("n", word()).executes(Branch("n")))));
        assertThrows(IllegalArgumentException.class, () -> Option.of("Max_Size", "Bad name"));
        assertThrows(IllegalArgumentException.class, () -> Option.of("help", "Reserved"));
    }
}
