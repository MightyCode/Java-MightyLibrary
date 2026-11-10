package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.word;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;

import static org.junit.jupiter.api.Assertions.*;

class CommandScriptTest {
    // Records the lines it runs, in order
    private static class RecordCommand extends BaseCommand {
        final List<String> calls = new ArrayList<>();

        RecordCommand() {
            super(TestCommands.Id("record"), ECommandLevel.USER, "Records the words it receives");
        }

        @Override
        protected void syntax(CommandNode root) {
            root.then(argument("words", word(), "Any words").variadic().optional().executes(context -> {
                List<String> words = context.getList("words");
                calls.add(String.join(" ", words));
                return !words.isEmpty() && words.get(0).equals("fail") ? CommandResult.error("failed") : CommandResult.ok("recorded");
            }));
        }
    }

    private static Commands commandsWith(RecordCommand record) {
        Commands commands = new Commands();
        TestCommands.add(commands, record);
        return commands;
    }

    private static CommandResult run(Commands commands, String line, int maxTicks) throws Exception {
        CompletableFuture<CommandResult> future = commands.submit(line);
        for (int tick = 0; tick < maxTicks && !future.isDone(); ++tick)
            commands.processPendingCommands();

        assertTrue(future.isDone(), "not finished after " + maxTicks + " ticks");
        return future.get(1, TimeUnit.SECONDS);
    }

    @Test
    void waitAnswersAfterTheGivenTicks() throws Exception {
        Commands commands = new Commands();

        CompletableFuture<CommandResult> future = commands.submit("wait 3");
        commands.processPendingCommands();
        assertFalse(future.isDone());
        commands.processPendingCommands();
        commands.processPendingCommands();
        assertFalse(future.isDone());
        commands.processPendingCommands();

        assertTrue(future.get(1, TimeUnit.SECONDS).isSuccess());
    }

    @Test
    void scriptResumesInTheTickWhereItsWaitEnds() {
        RecordCommand record = new RecordCommand();
        Commands commands = commandsWith(record);

        CommandScript script = new CommandScript(commands, "test", Arrays.asList("wait 1", "record b"));
        assertFalse(script.step());
        commands.addTickTask(script::step);

        commands.processPendingCommands();

        assertEquals(Arrays.asList("b"), record.calls, "the line after -> wait 1 <- runs at the next tick");
        assertTrue(script.getResult().isDone());
    }

    @Test
    void waitRejectsBadArguments() {
        Commands commands = new Commands();

        assertFalse(commands.execute("wait 0").isSuccess());
        assertFalse(commands.execute("wait abc").isSuccess());
        assertFalse(commands.execute("wait loaded").isSuccess(), "no scene manager in tests");
    }

    @Test
    void scriptRunsLinesInOrderAndWaitsForDeferredOnes() throws Exception {
        RecordCommand record = new RecordCommand();
        Commands commands = commandsWith(record);

        CommandScript script = new CommandScript(commands, "test",
                Arrays.asList("# comment", "record a", "", "wait 2", "record b"));

        assertFalse(script.step());
        assertEquals(Arrays.asList("a"), record.calls);

        commands.addTickTask(script::step);
        for (int tick = 0; tick < 5 && !script.getResult().isDone(); ++tick)
            commands.processPendingCommands();

        CommandResult result = script.getResult().get(1, TimeUnit.SECONDS);
        assertTrue(result.isSuccess(), result.toString());
        assertEquals(Arrays.asList("a", "b"), record.calls);

        JSONArray lines = result.getData().getJSONArray("lines");
        assertEquals(3, lines.length());
        assertEquals(4, lines.getJSONObject(1).getInt("line"));
    }

    @Test
    void scriptStopsAtTheFirstFailedLine() throws Exception {
        RecordCommand record = new RecordCommand();
        Commands commands = commandsWith(record);

        CommandScript script = new CommandScript(commands, "test",
                Arrays.asList("record a", "record fail", "record never"));

        assertTrue(script.step());

        CommandResult result = script.getResult().get(1, TimeUnit.SECONDS);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("line 2"));
        assertEquals(Arrays.asList("a", "fail"), record.calls);
    }

    @Test
    void scriptStopsOnAnUnknownCommand() throws Exception {
        Commands commands = new Commands();
        CommandScript script = new CommandScript(commands, "test", Arrays.asList("unknownCommand"));

        assertTrue(script.step());
        assertFalse(script.getResult().get(1, TimeUnit.SECONDS).isSuccess());
    }

    @Test
    void execRunsAFileWithQuotedWords(@TempDir Path folder) throws Exception {
        RecordCommand record = new RecordCommand();
        Commands commands = commandsWith(record);
        Path file = folder.resolve("my script.txt");
        Files.write(file, Arrays.asList("record \"a b\"", "wait 1", "record c"), StandardCharsets.UTF_8);

        CommandResult result = run(commands, "exec \"" + file + "\"", 10);

        assertTrue(result.isSuccess(), result.toString());
        assertEquals(Arrays.asList("a b", "c"), record.calls);
    }

    @Test
    void execRefusesAScriptRunningItself(@TempDir Path folder) throws Exception {
        Commands commands = new Commands();
        Path file = folder.resolve("loop.txt");
        Files.write(file, Arrays.asList("exec \"" + file + "\""), StandardCharsets.UTF_8);

        CommandResult result = run(commands, "exec \"" + file + "\"", 10);

        assertFalse(result.isSuccess());
        assertTrue(result.getData().toString().contains("already running"), result.toString());

        // Finished scripts can run again
        assertFalse(run(commands, "exec \"" + file + "\"", 10).isSuccess());
    }

    @Test
    void execReportsAMissingFile(@TempDir Path folder) {
        Commands commands = new Commands();

        CommandResult result = commands.execute("exec \"" + folder.resolve("missing.txt") + "\"");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Can't read"));
    }

    @Test
    void deferredResultBecomesAnErrorAfterItsTimeout() throws Exception {
        Commands commands = new Commands();
        TestCommands.add(commands, TestCommands.Syntax("never", ECommandLevel.USER, "Never answers",
                root -> root.executes(context -> CommandResult.later(new CompletableFuture<>(), 1))));

        CompletableFuture<CommandResult> future = commands.submit("never");
        commands.processPendingCommands();

        CommandResult result = future.get(5, TimeUnit.SECONDS);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("No answer after 1 seconds"), result.getMessage());
    }
}
