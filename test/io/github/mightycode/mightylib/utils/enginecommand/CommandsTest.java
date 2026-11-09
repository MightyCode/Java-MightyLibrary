package io.github.mightycode.mightylib.utils.enginecommand;

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
            super(name);
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

    // General commands only keep a reference to the scene manager, null is enough without GL
    private static Commands newCommands() {
        return new Commands(null);
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
        commands.addCommand(echo);

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
        commands.addCommand(echo);

        CommandResult result = commands.execute("echo help");
        assertTrue(result.isSuccess());
        assertEquals("echo help", result.getMessage());
        assertNull(echo.lastArgs);

        assertEquals("echo help", commands.execute("help echo").getMessage());
        assertTrue(commands.execute("help").getMessage().contains("- echo (scene)"));
    }

    @Test
    void exceptionOfACommandBecomesAnError() {
        Commands commands = newCommands();
        commands.addCommand(new BaseCommand("boom") {
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
    void duplicateNameIsRefused() {
        Commands commands = newCommands();
        commands.addCommand(new EchoCommand("echo"));

        assertThrows(IllegalArgumentException.class, () -> commands.addCommand(new EchoCommand("echo")));
        assertThrows(IllegalArgumentException.class, () -> commands.addCommand(new EchoCommand("help")));
    }

    @Test
    void invalidNameIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new EchoCommand(""));
        assertThrows(IllegalArgumentException.class, () -> new EchoCommand(" echo"));
        assertThrows(IllegalArgumentException.class, () -> new EchoCommand("a\"b"));
    }

    @Test
    void sceneCommandsAreRemovedButNotGeneralOnes() {
        Commands commands = newCommands();
        int generalCount = commands.getCommands().size();
        BaseCommand help = commands.findCommand("help");
        EchoCommand echo = new EchoCommand("echo");
        commands.addCommand(echo);

        assertFalse(commands.isSceneCommand(help));
        assertTrue(commands.isSceneCommand(echo));

        commands.removeSceneCommands();

        assertEquals(generalCount, commands.getCommands().size());
        assertNull(commands.findCommand("echo"));
        assertNotNull(commands.findCommand("help"));

        // The name can be used again by the next scene
        commands.addCommand(new EchoCommand("echo"));
    }

    @Test
    void submittedCommandsRunOnProcess() throws Exception {
        Commands commands = newCommands();
        commands.addCommand(new EchoCommand("echo"));

        CompletableFuture<CommandResult> future = commands.submit("echo x");
        assertFalse(future.isDone());

        commands.processPendingCommands();

        assertEquals("echo|x", future.get(1, TimeUnit.SECONDS).getMessage());
    }

    @Test
    void deferredResultCompletesTheSubmittedFuture() throws Exception {
        Commands commands = newCommands();
        CompletableFuture<CommandResult> later = new CompletableFuture<>();
        commands.addCommand(new BaseCommand("later") {
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
            super(name);
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
        commands.addCommand(new ChoiceCommand("zzfirst"));
        commands.addCommand(new ChoiceCommand("zzfoo"));

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
        commands.addCommand(new ChoiceCommand("zzchoice"));

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
        commands.addCommand(new BaseCommand("source") {
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
