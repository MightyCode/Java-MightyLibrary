package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandCondition;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.integer;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.suggest;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.word;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;
import static org.junit.jupiter.api.Assertions.*;

class CommandsTest {
    // Records the words it receives
    private static class EchoCommand extends BaseCommand {
        String[] lastArgs;

        EchoCommand(String name) {
            this(name, ECommandLevel.USER);
        }

        EchoCommand(String name, ECommandLevel level) {
            super(TestCommands.Id(name), level, "Echoes its words");
        }

        @Override
        protected void syntax(CommandNode root) {
            root.then(argument("words", word(), "Any words").variadic().optional().executes(context -> {
                List<String> words = context.getList("words");
                lastArgs = new String[words.size() + 1];
                lastArgs[0] = context.commandName();
                for (int i = 0; i < words.size(); ++i)
                    lastArgs[i + 1] = words.get(i);
                return CommandResult.ok(String.join("|", lastArgs));
            }));
        }
    }

    // The general commands only reach the engine when executed: no GL needed
    private static Commands newCommands() {
        return new Commands();
    }

    // A view without window: its commands run under it, without OpenGL context to bind
    private static class FakeView implements IView {
        @Override
        public Engine engine() { return null; }

        @Override
        public WindowContext context() { return null; }

        @Override
        public Camera2D camera2D() { return null; }

        @Override
        public Camera3D camera3D() { return null; }
    }

    @Test
    void splitSeparatesOnWhitespace() {
        assertArrayEquals(new String[]{"stats", "fps", "glObjects.Texture"},
                CommandLine.Split("  stats\tfps   glObjects.Texture "));
    }

    @Test
    void splitKeepsQuotedSpacesAndBackslashes() {
        assertArrayEquals(new String[]{"screenshot", "C:\\Moteur de jeu\\a b.png", "scale", "0.5"},
                CommandLine.Split("screenshot \"C:\\Moteur de jeu\\a b.png\" scale 0.5"));
    }

    @Test
    void splitJoinsQuotesInsideAWordAndKeepsEmptyWords() {
        assertArrayEquals(new String[]{"ab cd", ""}, CommandLine.Split("a\"b c\"d \"\""));
    }

    @Test
    void splitOfBlankLineIsEmpty() {
        assertEquals(0, CommandLine.Split("   ").length);
    }

    @Test
    void splitRejectsUnclosedQuote() {
        assertThrows(IllegalArgumentException.class, () -> CommandLine.Split("screenshot \"a b.png"));
    }

    @Test
    void executeGivesQuotedWordsToTheCommand() {
        Commands commands = newCommands();
        EchoCommand echo = new EchoCommand("echo");
        TestCommands.add(commands, echo);

        assertTrue(commands.execute("echo \"a b\" c").isSuccess());
        assertArrayEquals(new String[]{"echo", "a b", "c"}, echo.lastArgs);
    }

    @Test
    void executeReportsBadLines() {
        Commands commands = newCommands();

        assertFalse(commands.execute("").isSuccess());
        assertFalse(commands.execute("unknownCommand").isSuccess());
        assertFalse(commands.execute("help \"not closed").isSuccess());
    }

    // Declares its syntax: greet <name> [--times n], greet fail
    private static class GreetCommand extends BaseCommand {
        GreetCommand() {
            super(TestCommands.Id("greet"), ECommandLevel.USER, "Greets someone");
        }

        @Override
        protected void syntax(CommandNode root) {
            root.then(argument("name", suggest(() -> List.of("Alice", "Bob")), "Who to greet")
                            .option(Option.of("times", "How many times").value("n", integer(1, 3)).defaultValue(1))
                            .executes("Greet once or more", context -> CommandResult.ok(String.join(" ",
                                    Collections.nCopies(context.<Integer>get("times"), "Hello " + context.get("name"))))))
                    .then(literal("fail", "Fails on purpose").executes(context -> {
                        throw new CommandException("Failed on purpose");
                    }));
        }
    }

    @Test
    void helpOptionAnswersTheGeneratedHelp() {
        Commands commands = newCommands();
        TestCommands.add(commands, new GreetCommand());

        CommandResult result = commands.execute("greet --help");
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().startsWith("test:greet : Greets someone\nUsage :\n"
                + "  greet <name> [--times <n>] : Greet once or more\n  greet fail : Fails on purpose"), result.getMessage());
        assertEquals("test:greet", result.getData().getString("id"));

        // Anywhere on the line, and the same as help <cmd>
        assertEquals(result.getMessage(), commands.execute("greet Bob --help").getMessage());
        CommandResult help = commands.execute("help greet");
        assertEquals(result.getMessage(), help.getMessage());
        assertEquals(result.getData().toString(), help.getData().toString());

        String list = commands.execute("help").getMessage();
        assertTrue(list.contains("- test:greet (game) : Greets someone"), list);
        assertTrue(list.contains("- mightylib:help (engine)"), list);
    }

    @Test
    void commandWithSyntaxRunsItsBranchWithTypedValues() {
        Commands commands = newCommands();
        TestCommands.add(commands, new GreetCommand());

        assertEquals("Hello Bob Hello Bob", commands.execute("greet Bob --times 2").getMessage());
        assertEquals("Hello Bob", commands.execute("greet Bob").getMessage());

        CommandResult incomplete = commands.execute("greet");
        assertFalse(incomplete.isSuccess());
        assertEquals("Incomplete command\nUsage :\n  greet <name> [--times <n>]\n  greet fail", incomplete.getMessage());
        assertTrue(commands.execute("greet Bob --times 9").getMessage()
                .startsWith("Expected an integer between 1 and 3 for --times <n>, got ->9<-\nUsage :"));

        // A CommandException of a handler is the answer
        CommandResult failed = commands.execute("greet fail");
        assertFalse(failed.isSuccess());
        assertEquals("Failed on purpose", failed.getMessage());
    }

    @Test
    void commandWithSyntaxCompletesFromIt() {
        Commands commands = newCommands();
        TestCommands.add(commands, new GreetCommand());

        // No "help" in second word: --help is an option
        assertEquals(List.of("Alice", "Bob", "fail"), commands.complete("greet ").getCandidates());
        assertEquals(List.of("--help", "--times"), commands.complete("greet Bob -").getCandidates());
        assertEquals("--times ", commands.complete("greet Bob --t").replacement());
        assertTrue(commands.complete("greet fail -").getCandidates().contains("--help"));
    }

    @Test
    void hiddenConditionOfTheRootHidesTheCommand() {
        Commands commands = newCommands();
        boolean[] host = {false};
        TestCommands.add(commands, TestCommands.Syntax("zzhost", ECommandLevel.USER, "Only for the host",
                root -> root.requires(CommandCondition.hidden(context -> host[0])).executes(context -> CommandResult.ok("host"))));

        assertEquals("No command with its name ->zzhost<-", commands.execute("zzhost").getMessage());
        assertTrue(commands.complete("zzh").isEmpty());
        assertFalse(commands.execute("help").getMessage().contains("zzhost"));

        host[0] = true;
        assertEquals("host", commands.execute("zzhost").getMessage());
        assertEquals(List.of("zzhost"), commands.complete("zzh").getCandidates());
    }

    @Test
    void hiddenConditionOfTheRootIsAskedUnderTheOwner() {
        Commands commands = newCommands();
        FakeView view = new FakeView();
        List<IView> asked = new ArrayList<>();
        commands.register(TestCommands.Syntax("zzview", ECommandLevel.USER, "Only under its view",
                root -> root.requires(CommandCondition.hidden(context -> {
                    asked.add(Views.current());
                    return Views.current() == view;
                })).executes(context -> CommandResult.ok("view"))), CommandOwner.Of(view, "scene"));

        assertEquals("view", commands.execute("zzview").getMessage());
        assertEquals(List.of("zzview"), commands.complete("zzv").getCandidates());
        assertFalse(asked.isEmpty());
        for (IView current : asked)
            assertSame(view, current);
    }

    @Test
    void helpOfACommandNotRegistered() {
        BaseCommand command = TestCommands.Syntax("zzalone", ECommandLevel.USER, "Never registered",
                root -> root.executes(context -> CommandResult.ok("alone")));

        CommandResult help = command.helpResult();
        assertTrue(help.isSuccess());
        assertTrue(help.getMessage().startsWith("test:zzalone : Never registered"));
    }

    @Test
    void mistakeOfASyntaxIsFoundAtRegistration() {
        Commands commands = newCommands();
        BaseCommand broken = TestCommands.Syntax("zzbroken", ECommandLevel.USER, "Broken",
                root -> root.then(literal("dead")));

        assertThrows(IllegalStateException.class, () -> TestCommands.add(commands, broken));
        assertNull(commands.findCommand("zzbroken"));
        assertThrows(IllegalArgumentException.class, () -> TestCommands.Syntax("zz", ECommandLevel.USER, "", root -> {}));
    }

    @Test
    void exceptionOfACommandBecomesAnError() {
        Commands commands = newCommands();
        TestCommands.add(commands, TestCommands.Syntax("boom", ECommandLevel.USER, "Breaks", root -> root.executes(context -> {
            throw new IllegalStateException("broken");
        })));

        CommandResult result = commands.execute("boom");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("broken"));
    }

    @Test
    void duplicateIdentifierIsRefused() {
        Commands commands = newCommands();
        TestCommands.add(commands, new EchoCommand("echo"));

        assertThrows(IllegalArgumentException.class, () -> TestCommands.add(commands, new EchoCommand("echo")));
        // Same path, another namespace: accepted
        TestCommands.add(commands, new EchoCommand("help"));
    }

    @Test
    void invalidNameIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new EchoCommand(""));
        assertThrows(IllegalArgumentException.class, () -> new EchoCommand(" echo"));
        assertThrows(IllegalArgumentException.class, () -> new EchoCommand("a\"b"));
        assertThrows(IllegalArgumentException.class, () -> new EchoCommand("Echo"));
    }

    @Test
    void commandIsFoundByItsPathOrItsIdentifier() {
        Commands commands = newCommands();
        TestCommands.add(commands, new EchoCommand("echo"));

        assertEquals("echo", commands.execute("echo").getMessage());
        assertEquals("ECHO", commands.execute("ECHO").getMessage());
        assertEquals("test:echo", commands.execute("test:echo").getMessage());
        assertEquals("Test:Echo", commands.execute("Test:Echo").getMessage());
        assertFalse(commands.execute("other:echo").isSuccess());
        assertFalse(commands.execute("test:").isSuccess());
        assertTrue(commands.execute("mightylib:help").isSuccess());
    }

    @Test
    void ambiguousPathIsRefusedWithTheIdentifiers() {
        Commands commands = newCommands();
        TestCommands.add(commands, new EchoCommand("help"));

        CommandResult result = commands.execute("help");
        assertFalse(result.isSuccess());
        assertEquals("Ambiguous command ->help<-, write one of : mightylib:help, test:help", result.getMessage());
        assertNull(commands.findCommand("help"));

        assertEquals("test:help", commands.execute("test:help").getMessage());
        assertTrue(commands.execute("mightylib:help").getMessage().startsWith("Available commands"));
        assertTrue(commands.execute("mightylib:help test:help").isSuccess());
    }

    @Test
    void completionAddsTheIdentifiersOfAmbiguousPaths() {
        Commands commands = newCommands();
        TestCommands.add(commands, new EchoCommand("zzunique"));
        TestCommands.add(commands, new EchoCommand("help"));

        assertEquals(List.of("zzunique"), commands.complete("zz").getCandidates());
        // Typed by its path, an ambiguous command is refused: the identifiers are proposed with the paths
        List<String> all = commands.complete("").getCandidates();
        assertTrue(all.containsAll(List.of("help", "mightylib:help", "test:help", "zzunique")));
        assertFalse(all.contains("test:zzunique"));
        assertFalse(all.contains("mightylib:stats"));
        assertEquals(List.of("test:help", "test:zzunique"), commands.complete("test:").getCandidates());
        assertTrue(commands.complete("mightylib:help ").getCandidates().contains("help"));
    }

    @Test
    void commandsAreRemovedByOwner() {
        Commands commands = newCommands();
        int generalCount = commands.getCommands().size();
        CommandOwner scene = CommandOwner.Of(new FakeView(), "scene");
        commands.register(new EchoCommand("echo"), scene);

        assertSame(CommandOwner.ENGINE, commands.findCommand("help").getOwner());
        assertEquals(scene, commands.findCommand("echo").getOwner());

        // Another owner object for the same view and kind: the same owner
        commands.removeOwnedBy(CommandOwner.Of(scene.view(), "scene"));

        assertEquals(generalCount, commands.getCommands().size());
        assertNull(commands.findCommand("echo"));
        assertNotNull(commands.findCommand("help"));

        // The name can be used again by the next scene
        commands.register(new EchoCommand("echo"), CommandOwner.Of(new FakeView(), "scene"));
    }

    // Regression: the commands added by the game were removed at the first scene change
    @Test
    void gameCommandsSurviveTheScenes() {
        Commands commands = newCommands();
        commands.registryFor(CommandOwner.GAME).register(new EchoCommand("gamecommand"));

        // A scene comes and goes, then the next one
        for (int i = 0; i < 2; ++i) {
            CommandOwner scene = CommandOwner.Of(new FakeView(), "scene");
            commands.register(new EchoCommand("scenecommand"), scene);
            commands.removeOwnedBy(scene);
        }

        assertTrue(commands.execute("gamecommand").isSuccess());
        assertNull(commands.findCommand("scenecommand"));
    }

    @Test
    void ownersDoNotRemoveTheCommandsOfEachOther() {
        Commands commands = newCommands();
        FakeView view = new FakeView();
        CommandOwner first = CommandOwner.Of(new FakeView(), "scene");
        CommandOwner second = CommandOwner.Of(new FakeView(), "scene");
        commands.register(new EchoCommand("first"), first);
        commands.register(new EchoCommand("second"), second);
        // Same view, another kind of owner
        commands.register(new EchoCommand("ofwindow"), CommandOwner.Of(view, "window"));

        commands.removeOwnedBy(first);
        commands.removeOwnedBy(CommandOwner.Of(view, "scene"));

        assertNull(commands.findCommand("first"));
        assertNotNull(commands.findCommand("second"));
        assertNotNull(commands.findCommand("ofwindow"));
    }

    @Test
    void commandOfAViewRunsUnderIt() {
        Commands commands = newCommands();
        FakeView view = new FakeView();
        IView[] seen = new IView[1];
        commands.register(TestCommands.Syntax("where", ECommandLevel.USER, "Where it runs", root -> root.executes(context -> {
            seen[0] = Views.current();
            return CommandResult.ok("");
        })), CommandOwner.Of(view, "scene"));

        assertTrue(commands.execute("where").isSuccess());
        assertSame(view, seen[0]);
    }

    @Test
    void aCommandIsRegisteredOnceAtATime() {
        Commands commands = newCommands();
        EchoCommand echo = new EchoCommand("echo");
        CommandOwner scene = CommandOwner.Of(new FakeView(), "scene");
        commands.register(echo, scene);

        assertThrows(IllegalArgumentException.class, () -> commands.register(echo, CommandOwner.Of(new FakeView(), "scene")));
    }

    // Regression: a scene used again (same instance, command kept in a field) could not register its command again
    @Test
    void aRemovedCommandCanBeRegisteredAgain() {
        Commands commands = newCommands();
        EchoCommand echo = new EchoCommand("echo");
        CommandOwner scene = CommandOwner.Of(new FakeView(), "scene");

        for (int i = 0; i < 2; ++i) {
            commands.register(echo, scene);
            assertSame(scene, echo.getOwner());
            commands.removeOwnedBy(scene);
            assertNull(echo.getOwner());
        }
    }

    // Regression: the same scene in two windows could not have its command twice
    @Test
    void theCommandOfSeveralViewsRunsInTheTargetedOne() {
        Commands commands = newCommands();
        FakeView first = new FakeView();
        FakeView second = new FakeView();
        EchoCommand ofFirst = new EchoCommand("spawn");
        EchoCommand ofSecond = new EchoCommand("spawn");
        commands.register(ofFirst, CommandOwner.Of(first, "scene"));
        commands.register(ofSecond, CommandOwner.Of(second, "scene"));

        // No targeted view: refused, with a message about the windows
        CommandResult refused = commands.execute("spawn a");
        assertFalse(refused.isSuccess());
        assertTrue(refused.getMessage().contains("several windows"), refused.getMessage());
        assertNull(commands.findCommand("test:spawn"));
        // Completion: the path once, no identifier (only one command)
        assertEquals(List.of("spawn"), commands.complete("spaw").getCandidates());
        assertTrue(commands.execute("help spawn").isSuccess());

        IView[] target = {second};
        commands.setViewChooser(views -> views.contains(target[0]) ? target[0] : null);
        assertTrue(commands.execute("spawn b").isSuccess());
        assertArrayEquals(new String[]{"spawn", "b"}, ofSecond.lastArgs);
        assertNull(ofFirst.lastArgs);

        target[0] = first;
        assertTrue(commands.execute("test:spawn c").isSuccess());
        assertArrayEquals(new String[]{"test:spawn", "c"}, ofFirst.lastArgs);

        // Not between a view and the game, nor twice in the same view
        assertThrows(IllegalArgumentException.class, () -> TestCommands.add(commands, new EchoCommand("spawn")));
        assertThrows(IllegalArgumentException.class,
                () -> commands.register(new EchoCommand("spawn"), CommandOwner.Of(first, "scene")));
    }

    @Test
    void commandExceptionIsTheAnswer() {
        Commands commands = newCommands();
        TestCommands.add(commands, TestCommands.Syntax("loading", ECommandLevel.USER, "Needs a launched scene",
                root -> root.requires(CommandCondition.launched((Scene) null)).executes(context -> CommandResult.ok("launched"))));

        CommandResult result = commands.execute("loading");
        assertFalse(result.isSuccess());
        assertEquals("Scene still loading", result.getMessage());
    }

    @Test
    void submittedCommandsRunOnProcess() throws Exception {
        Commands commands = newCommands();
        TestCommands.add(commands, new EchoCommand("echo"));

        CompletableFuture<CommandResult> future = commands.submit("echo x");
        assertFalse(future.isDone());

        commands.processPendingCommands();

        assertEquals("echo|x", future.get(1, TimeUnit.SECONDS).getMessage());
    }

    @Test
    void deferredResultCompletesTheSubmittedFuture() throws Exception {
        Commands commands = newCommands();
        CompletableFuture<CommandResult> later = new CompletableFuture<>();
        TestCommands.add(commands, TestCommands.Syntax("later", ECommandLevel.USER, "Answers later",
                root -> root.executes(context -> CommandResult.later(later))));

        CompletableFuture<CommandResult> future = commands.submit("later");
        commands.processPendingCommands();
        assertFalse(future.isDone());

        later.complete(CommandResult.ok("done"));
        assertEquals("done", future.get(1, TimeUnit.SECONDS).getMessage());
    }

    // Completes its first argument with fixed values
    private static class ChoiceCommand extends BaseCommand {
        ChoiceCommand(String name) {
            super(TestCommands.Id(name), ECommandLevel.USER, "Completes its first argument");
        }

        @Override
        protected void syntax(CommandNode root) {
            root.then(argument("scene", suggest(() -> List.of("Scene2D", "Scene3D", "scene3DBis", "Menu Scene")))
                    .optional().executes(context -> CommandResult.ok("")));
        }
    }

    @Test
    void splitPartialGivesTheUnfinishedWordAndItsStart() {
        CommandLine.Partial partial = CommandLine.SplitPartial("scene  Sce");
        assertArrayEquals(new String[]{"scene", "Sce"}, partial.getWords());
        assertEquals(7, partial.getLastStart());

        partial = CommandLine.SplitPartial("scene ");
        assertArrayEquals(new String[]{"scene", ""}, partial.getWords());
        assertEquals(6, partial.getLastStart());

        partial = CommandLine.SplitPartial("screenshot \"a b");
        assertArrayEquals(new String[]{"screenshot", "a b"}, partial.getWords());
        assertEquals(11, partial.getLastStart());

        partial = CommandLine.SplitPartial("");
        assertArrayEquals(new String[]{""}, partial.getWords());
        assertEquals(0, partial.getLastStart());
    }

    @Test
    void completeCommandNames() {
        Commands commands = newCommands();
        TestCommands.add(commands, new ChoiceCommand("zzfirst"));
        TestCommands.add(commands, new ChoiceCommand("zzfoo"));

        CommandCompletion completion = commands.complete("zz");
        assertEquals(List.of("zzfirst", "zzfoo"), completion.getCandidates());
        assertEquals("zzf", completion.replacement());

        completion = commands.complete("ZZFO");
        assertTrue(completion.isUnique());
        assertEquals("zzfoo ", completion.replacement());
        assertEquals(0, completion.getStart());

        assertTrue(commands.complete("unknownPrefix").isEmpty());
        assertNull(commands.complete("unknownPrefix").replacement());
    }

    @Test
    void completeArgumentsIgnoresCaseAndQuotes() {
        Commands commands = newCommands();
        TestCommands.add(commands, new ChoiceCommand("zzchoice"));

        CommandCompletion completion = commands.complete("zzchoice sce");
        assertEquals(List.of("Scene2D", "Scene3D", "scene3DBis"), completion.getCandidates());
        assertEquals("Scene", completion.replacement());
        assertEquals(9, completion.getStart());

        // help is not proposed as a word of the command: --help is
        assertFalse(commands.complete("zzchoice ").getCandidates().contains("help"));
        assertEquals("\"Menu Scene\" ", commands.complete("zzchoice Me").replacement());
        assertEquals("\"Menu Scene\" ", commands.complete("zzchoice \"Me").replacement());

        // Nothing to propose after the last argument, or for an unknown command
        assertTrue(commands.complete("zzchoice a b").isEmpty());
        assertTrue(commands.complete("unknown a").isEmpty());
    }

    @Test
    void helpCompletesCommandNames() {
        Commands commands = newCommands();
        assertTrue(commands.complete("help sta").getCandidates().contains("stats"));
    }

    @Test
    void listenersSeeTheSourceTheCommandAndItsResult() throws Exception {
        Commands commands = newCommands();
        TestCommands.add(commands, TestCommands.Syntax("source", ECommandLevel.USER, "The name of its source",
                root -> root.executes(context -> CommandResult.ok(commands.getCurrentSource().name()))));

        List<String> seen = new ArrayList<>();
        commands.addListener(new ICommandListener() {
            @Override
            public void executed(ICommandSource source, ECommandLevel level, String line) {
                seen.add(source.name() + ">" + line);
            }

            @Override
            public void answered(ICommandSource source, ECommandLevel level, String line, CommandResult result) {
                seen.add(source.name() + "<" + result.getMessage());
            }
        });

        CompletableFuture<CommandResult> future = commands.submit("source", ICommandSource.Named("server", ECommandLevel.DEV));
        commands.processPendingCommands();

        assertEquals("server", future.get(1, TimeUnit.SECONDS).getMessage());
        assertEquals(List.of("server>source", "server<server"), seen);
        // Executed directly: no submitted source
        assertEquals("api", commands.execute("source").getMessage());
    }

    @Test
    void jsonSelectKeepsOnlyGivenPaths() {
        JSONObject source = new JSONObject("{\"fps\":60,\"memory\":1,\"glObjects\":{\"Texture\":3,\"Shader\":2}}");

        JSONObject selected = JsonSelect.Select(source, "fps", "glObjects.Texture", "unknown", "glObjects.unknown.deep");

        assertEquals("{\"fps\":60,\"glObjects\":{\"Texture\":3}}", selected.toString());
    }
}
