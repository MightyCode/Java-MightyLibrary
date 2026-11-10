package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleLogTest {
    private static List<String> texts(List<ConsoleLog.Line> lines) {
        return lines.stream().map(ConsoleLog.Line::getText).collect(Collectors.toList());
    }

    @Test
    void addSplitsOnLineBreaksWithoutTrailingEmptyLine() {
        ConsoleLog log = new ConsoleLog(10);
        log.add(ConsoleLog.EType.OUT, null, "a\nb\r\nc\n");
        log.add(ConsoleLog.EType.OUT, null, "");

        assertEquals(List.of("a", "b", "c", ""), texts(log.last(10)));
    }

    @Test
    void ringBufferForgetsTheOldestLinesAndKeepsIncreasingIds() {
        ConsoleLog log = new ConsoleLog(3);
        for (int i = 1; i <= 5; ++i)
            log.add(ConsoleLog.EType.OUT, null, "l" + i);

        assertEquals(3, log.size());
        assertEquals(List.of("l3", "l4", "l5"), texts(log.last(10)));
        assertEquals(3, log.get(0).getId());
        assertEquals(List.of("l4", "l5"), texts(log.last(2)));
        assertEquals(List.of("l5"), texts(log.since(4)));
        assertEquals(List.of("l3", "l4", "l5"), texts(log.since(0)));
    }

    @Test
    void lowerShowsWrittenLinesToLowerLevelsOnly() {
        ConsoleLog log = new ConsoleLog(3);
        List<ConsoleLog.Line> written = log.add(ConsoleLog.EType.COMMAND, "game", "a\nb", ECommandLevel.CHEAT);
        log.add(ConsoleLog.EType.RESULT, "game", "c", ECommandLevel.CHEAT);
        long user = log.version(ECommandLevel.USER);
        long developer = log.version(ECommandLevel.DEV);

        log.lower(written, ECommandLevel.USER);
        assertEquals(List.of("a", "b"), texts(log.last(10, ECommandLevel.USER)));
        assertNotEquals(user, log.version(ECommandLevel.USER));
        // DEV already saw them: nothing to rebuild
        assertEquals(developer, log.version(ECommandLevel.DEV));

        // Never raised
        log.lower(written, ECommandLevel.DEV);
        assertEquals(ECommandLevel.USER, log.get(0).getLevel());

        // A forgotten line is ignored
        log.add(ConsoleLog.EType.OUT, null, "d");
        log.lower(written, ECommandLevel.USER);
        assertEquals(List.of("b"), texts(log.last(10, ECommandLevel.USER)));
    }

    @Test
    void versionChangesOnAddAndClear() {
        ConsoleLog log = new ConsoleLog(3);
        long first = log.version();
        log.add(ConsoleLog.EType.OUT, null, "x");
        long second = log.version();
        log.clear();

        assertNotEquals(first, second);
        assertNotEquals(second, log.version());
        assertEquals(0, log.size());
        // Ids are never reused after a clear
        log.add(ConsoleLog.EType.OUT, null, "y");
        assertEquals(2, log.get(0).getId());
    }

    @Test
    void listenersReceiveEachLine() {
        ConsoleLog log = new ConsoleLog(10);
        List<String> received = new ArrayList<>();
        log.addListener(line -> received.add(line.getText()));

        log.add(ConsoleLog.EType.COMMAND, "server", "stats\nfps");

        assertEquals(List.of("stats", "fps"), received);
        assertEquals("[server] > stats", log.get(0).toString());
    }

    @Test
    void concurrentWritersLoseNoLine() throws InterruptedException {
        ConsoleLog log = new ConsoleLog(10000);
        Thread[] threads = new Thread[4];
        for (int t = 0; t < threads.length; ++t) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 1000; ++i)
                    log.add(ConsoleLog.EType.OUT, null, "x");
            });
            threads[t].start();
        }
        for (Thread thread : threads)
            thread.join();

        assertEquals(4000, log.size());
        assertEquals(4000, log.get(3999).getId());
    }

    @Test
    void readsAroundALineForTheViewerAndTheFilter() {
        ConsoleLog log = new ConsoleLog(6);
        // Ids 1 to 8, the first two forgotten
        for (int i = 1; i <= 8; ++i) {
            ConsoleLog.EType type = i % 2 == 0 ? ConsoleLog.EType.OUT : ConsoleLog.EType.RESULT;
            log.add(type, null, "l" + i);
        }

        assertEquals(3, log.oldestId());
        assertEquals(List.of("l3", "l4", "l5"), texts(log.upTo(5, 10, ECommandLevel.DEV, null)));
        assertEquals(List.of("l4", "l5"), texts(log.upTo(5, 2, ECommandLevel.DEV, null)));
        assertEquals(List.of("l6", "l7"), texts(log.after(5, 2, ECommandLevel.DEV, null)));
        // An id between two kept lines, before the oldest or after the newest
        assertEquals(List.of("l3", "l4", "l5", "l6", "l7", "l8"), texts(log.after(0, 10, ECommandLevel.DEV, null)));
        assertEquals(List.of(), texts(log.after(8, 10, ECommandLevel.DEV, null)));
        assertEquals(List.of(), texts(log.upTo(2, 10, ECommandLevel.DEV, null)));
        assertEquals(List.of("l8"), texts(log.upTo(Long.MAX_VALUE, 1, ECommandLevel.DEV, null)));

        // The player doesn't see the program output (OUT, even ids); the filter hides lines too
        assertEquals(List.of("l3", "l5"), texts(log.upTo(6, 10, ECommandLevel.USER, null)));
        assertEquals(List.of("l7"), texts(log.after(5, 10, ECommandLevel.USER, null)));
        assertEquals(List.of("l4", "l6"),
                texts(log.upTo(7, 10, ECommandLevel.DEV, line -> line.getType() == ConsoleLog.EType.OUT)));

        // Cleared for the player: nothing up to the newest line, the developer still reads them
        log.clear(ECommandLevel.USER);
        assertEquals(8, log.clearedUpTo(ECommandLevel.USER));
        assertEquals(0, log.clearedUpTo(ECommandLevel.DEV));
        assertEquals(List.of(), texts(log.upTo(8, 10, ECommandLevel.USER, null)));
        assertEquals(List.of(), texts(log.after(0, 10, ECommandLevel.USER, null)));
        assertEquals(6, log.upTo(8, 10, ECommandLevel.DEV, null).size());

        // Everything forgotten: cleared for every level
        log.clear();
        assertEquals(8, log.clearedUpTo(ECommandLevel.DEV));
        assertEquals(9, log.oldestId());
    }

    @Test
    void teeWritesThroughAndLogsCompleteLines() {
        ConsoleLog log = new ConsoleLog(10);
        ByteArrayOutputStream real = new ByteArrayOutputStream();
        PrintStream target = new PrintStream(real, true, StandardCharsets.UTF_8);
        PrintStream tee = LogCapture.tee(target, log, ConsoleLog.EType.ERR, StandardCharsets.UTF_8);

        tee.print("début ");
        assertEquals(0, log.size());
        tee.println("é");
        tee.print("a\r\nb\n");

        assertEquals("début é" + System.lineSeparator() + "a\r\nb\n", real.toString(StandardCharsets.UTF_8));
        assertEquals(List.of("début é", "a", "b"), texts(log.last(10)));
        assertEquals(ConsoleLog.EType.ERR, log.get(0).getType());
    }
}
