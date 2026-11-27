package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.utils.ELogLevel;
import io.github.mightycode.mightylib.utils.LogEntry;
import io.github.mightycode.mightylib.utils.enginecommand.CommandOwner;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.Commands;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.internal.ProblemLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

// The log is fed directly: the problems of other tests of the process do not reach it
class ProblemsGenComTest {
    private ProblemLog log = new ProblemLog();
    private final Commands commands = newCommands();

    private Commands newCommands() {
        Commands created = new Commands(ECommandLevel.DEV, ECommandLevel.DEV);
        created.register(new ProblemsGenCom(() -> log), CommandOwner.Plugin(Identifier.ENGINE_NAMESPACE));
        return created;
    }

    private void send(ELogLevel level, String category, String message, Throwable error) {
        log.onLog(new LogEntry(level, category, message, error, "main", Instant.EPOCH));
    }

    @Test
    void countsByLevelAndCategory() {
        send(ELogLevel.ERROR, "gl", "Uniform 'x' not found", null);
        send(ELogLevel.WARNING, "config", "Unknown property", null);
        send(ELogLevel.WARNING, "gl", "Performance warning", null);

        CommandResult result = commands.execute("problems");
        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals("1 error(s), 2 warning(s)\n  config: 1 warning\n  gl: 1 error 1 warning\nLast 3:\n"
                + "  ERROR [gl] Uniform 'x' not found\n  WARNING [config] Unknown property\n"
                + "  WARNING [gl] Performance warning", result.getMessage());

        JSONObject data = result.getData();
        assertEquals(1, data.getLong("errors"));
        assertEquals(2, data.getLong("warnings"));
        assertEquals(1, data.getJSONObject("categories").getJSONObject("gl").getLong("error"));
        assertFalse(data.getJSONObject("categories").getJSONObject("config").has("error"));
        assertEquals(3, data.getJSONArray("last").length());
    }

    @Test
    void levelAndLastFilterTheLastProblems() {
        for (int i = 0; i < 5; ++i) {
            send(ELogLevel.ERROR, "engine", "error " + i, null);
            send(ELogLevel.WARNING, "engine", "warning " + i, null);
        }

        JSONArray last = commands.execute("problems --level error --last 2").getData().getJSONArray("last");
        assertEquals(2, last.length());
        assertEquals("error 3", last.getJSONObject(0).getString("message"));
        assertEquals("error 4", last.getJSONObject(1).getString("message"));

        // The counts are not filtered
        JSONObject data = commands.execute("problems --level warning").getData();
        assertEquals(5, data.getLong("errors"));
        assertEquals(5, data.getJSONArray("last").length());
        assertEquals("warning", data.getJSONArray("last").getJSONObject(4).getString("level"));
    }

    @Test
    void exceptionsAndMultiLineMessages() {
        send(ELogLevel.ERROR, "engine", "Hook failed", new IllegalStateException("boom"));
        send(ELogLevel.WARNING, "gl", "OpenGL debug message\n\tID: 0x1", null);

        CommandResult result = commands.execute("problems");
        assertTrue(result.getMessage().contains("ERROR [engine] Hook failed: java.lang.IllegalStateException: boom"));
        assertTrue(result.getMessage().contains("WARNING [gl] OpenGL debug message (...)"), result.getMessage());

        JSONObject first = result.getData().getJSONArray("last").getJSONObject(0);
        assertEquals("java.lang.IllegalStateException: boom", first.getString("exception"));
        assertTrue(first.getString("stackTrace").contains("ProblemsGenComTest"));
        assertEquals("main", first.getString("thread"));
        assertFalse(result.getData().getJSONArray("last").getJSONObject(1).has("exception"));
    }

    @Test
    void clearForgetsCountsAndLastOnes() {
        send(ELogLevel.ERROR, "engine", "lost", null);
        assertTrue(commands.execute("problems clear").isSuccess());

        CommandResult result = commands.execute("problems");
        assertEquals("0 error(s), 0 warning(s)", result.getMessage());
        assertEquals(0, result.getData().getJSONArray("last").length());
    }

    @Test
    void fullRingKeepsTheNewestButCountsAll() {
        log = new ProblemLog(3);
        for (int i = 0; i < 5; ++i)
            send(ELogLevel.WARNING, "resources", "w" + i, null);

        JSONObject data = commands.execute("problems").getData();
        assertEquals(5, data.getLong("warnings"));
        assertEquals(3, data.getInt("kept"));
        assertEquals(3, data.getInt("capacity"));
        assertEquals("w2", data.getJSONArray("last").getJSONObject(0).getString("message"));
        assertEquals("w4", data.getJSONArray("last").getJSONObject(2).getString("message"));
    }

    @Test
    void defaultShowsTheTwentyLast() {
        for (int i = 0; i < 30; ++i)
            send(ELogLevel.WARNING, "resources", "w" + i, null);

        JSONArray last = commands.execute("problems").getData().getJSONArray("last");
        assertEquals(ProblemsGenCom.DEFAULT_LAST, last.length());
        assertEquals("w10", last.getJSONObject(0).getString("message"));
    }

    @Test
    void refusedValues() {
        assertFalse(commands.execute("problems --level info").isSuccess());
        assertFalse(commands.execute("problems --last 0").isSuccess());
    }
}
