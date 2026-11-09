package io.github.mightycode.mightylib.console;

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
