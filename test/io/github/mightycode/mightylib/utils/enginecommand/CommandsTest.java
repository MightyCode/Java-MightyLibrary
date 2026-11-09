package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CommandsTest {
    // Records the words it receives
    private static class EchoCommand extends BaseCommand {
        String[] lastArgs;

        EchoCommand(String name) {
            super(TestCommands.Id(name));
        }

        @Override
        public CommandResult execute(String[] args) {
            lastArgs = args;
            return CommandResult.ok(String.join("|", args));
        }

        @Override
        public String help() {
            return "echo help";
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

    @Test
    void helpArgumentAnswersTheCommandHelp() {
        Commands commands = newCommands();
        EchoCommand echo = new EchoCommand("echo");
        TestCommands.add(commands, echo);

        CommandResult result = commands.execute("echo help");
        assertTrue(result.isSuccess());
        assertEquals("echo help", result.getMessage());
        assertNull(echo.lastArgs);

        assertEquals("echo help", commands.execute("help echo").getMessage());
        assertTrue(commands.execute("help").getMessage().contains("- test:echo (game)"));
        assertTrue(commands.execute("help").getMessage().contains("- mightylib:help (engine)"));
    }

    @Test
    void exceptionOfACommandBecomesAnError() {
        Commands commands = newCommands();
        TestCommands.add(commands, new BaseCommand(TestCommands.Id("boom")) {
            @Override
            public CommandResult execute(String[] args) {
                throw new IllegalStateException("broken");
            }
        });

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
        commands.register(new BaseCommand(TestCommands.Id("where")) {
            @Override
            public CommandResult execute(String[] args) {
                seen[0] = Views.current();
                return CommandResult.ok("");
            }
        }, CommandOwner.Of(view, "scene"));

        assertTrue(commands.execute("where").isSuccess());
        assertSame(view, seen[0]);
    }

    @Test
    void aCommandIsRegisteredOnce() {
        Commands commands = newCommands();
        EchoCommand echo = new EchoCommand("echo");
        TestCommands.add(commands, echo);
        commands.removeOwnedBy(CommandOwner.GAME);

        assertThrows(IllegalArgumentException.class, () -> TestCommands.add(commands, echo));
    }

    @Test
    void commandExceptionIsTheAnswer() {
        Commands commands = newCommands();
        TestCommands.add(commands, new BaseCommand(TestCommands.Id("loading")) {
            @Override
            public CommandResult execute(String[] args) {
                requireLaunched(null);
                return CommandResult.ok("launched");
            }
        });

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
        TestCommands.add(commands, new BaseCommand(TestCommands.Id("later")) {
            @Override
            public CommandResult execute(String[] args) {
                return CommandResult.later(later);
            }
        });

        CompletableFuture<CommandResult> future = commands.submit("later");
        commands.processPendingCommands();
        assertFalse(future.isDone());

        later.complete(CommandResult.ok("done"));
        assertEquals("done", future.get(1, TimeUnit.SECONDS).getMessage());
    }

    // Completes its first argument with fixed values
    private static class ChoiceCommand extends BaseCommand {
        ChoiceCommand(String name) {
            super(TestCommands.Id(name));
        }

        @Override
        public CommandResult execute(String[] args) {
            return CommandResult.ok("");
        }

        @Override
        public Collection<String> completeArgument(String[] args) {
            if (args.length == 2)
                return List.of("Scene2D", "Scene3D", "scene3DBis", "Menu Scene");
            throw new IllegalStateException("No completion here");
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
    void completeArgumentsIgnoresCaseAddsHelpAndQuotes() {
        Commands commands = newCommands();
        TestCommands.add(commands, new ChoiceCommand("zzchoice"));

        CommandCompletion completion = commands.complete("zzchoice sce");
        assertEquals(List.of("Scene2D", "Scene3D", "scene3DBis"), completion.getCandidates());
        assertEquals("Scene", completion.replacement());
        assertEquals(9, completion.getStart());

        assertTrue(commands.complete("zzchoice ").getCandidates().contains("help"));
        assertEquals("\"Menu Scene\" ", commands.complete("zzchoice Me").replacement());
        assertEquals("\"Menu Scene\" ", commands.complete("zzchoice \"Me").replacement());

        // A failing completion gives nothing instead of throwing
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
        TestCommands.add(commands, new BaseCommand(TestCommands.Id("source")) {
            @Override
            public CommandResult execute(String[] args) {
                return CommandResult.ok(commands.getCurrentSource().name());
            }
        });

        List<String> seen = new ArrayList<>();
        commands.addListener(new ICommandListener() {
            @Override
            public void executed(ICommandSource source, String line) {
                seen.add(source.name() + ">" + line);
            }

            @Override
            public void answered(ICommandSource source, String line, CommandResult result) {
                seen.add(source.name() + "<" + result.getMessage());
            }
        });

        CompletableFuture<CommandResult> future = commands.submit("source", ICommandSource.Named("server"));
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
