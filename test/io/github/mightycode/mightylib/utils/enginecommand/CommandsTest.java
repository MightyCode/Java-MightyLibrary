package io.github.mightycode.mightylib.utils.enginecommand;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

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

    @Test
    void jsonSelectKeepsOnlyGivenPaths() {
        JSONObject source = new JSONObject("{\"fps\":60,\"memory\":1,\"glObjects\":{\"Texture\":3,\"Shader\":2}}");

        JSONObject selected = JsonSelect.Select(source, "fps", "glObjects.Texture", "unknown", "glObjects.unknown.deep");

        assertEquals("{\"fps\":60,\"glObjects\":{\"Texture\":3}}", selected.toString());
    }
}
