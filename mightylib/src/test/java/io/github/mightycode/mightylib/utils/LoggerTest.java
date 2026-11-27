package io.github.mightycode.mightylib.utils;

import io.github.mightycode.mightylib.algorithms.OutputCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LoggerTest {
    // Unique category: problems sent by other code of the process are not counted
    private static final String CATEGORY = "logger-test";

    private final List<ILogListener> added = new ArrayList<>();

    @AfterEach
    void removeListeners() {
        for (ILogListener listener : added)
            Logger.RemoveListener(listener);
    }

    private Recorder record() {
        Recorder recorder = new Recorder();
        add(recorder);
        return recorder;
    }

    private void add(ILogListener listener) {
        added.add(listener);
        Logger.AddListener(listener);
    }

    @Test
    void levelsCategoryAndMessage() {
        Recorder recorder = record();
        OutputCapture.errorsOf(() -> {
            Logger.Error(CATEGORY, "first");
            Logger.Warn(CATEGORY, "second");
        });

        List<LogEntry> entries = recorder.entries();
        assertEquals(2, entries.size());
        assertEquals(ELogLevel.ERROR, entries.get(0).level());
        assertEquals("first", entries.get(0).message());
        assertNull(entries.get(0).error());
        assertEquals(ELogLevel.WARNING, entries.get(1).level());
        assertEquals(CATEGORY, entries.get(1).category());
        assertEquals(Thread.currentThread().getName(), entries.get(1).thread());
        assertNotNull(entries.get(1).time());
    }

    @Test
    void removedListenerReceivesNothing() {
        Recorder recorder = record();
        assertTrue(Logger.RemoveListener(recorder));
        assertFalse(Logger.RemoveListener(recorder));

        OutputCapture.errorsOf(() -> Logger.Error(CATEGORY, "nobody"));
        assertTrue(recorder.entries().isEmpty());
    }

    @Test
    void logExceptionIsAnEngineErrorWithItsException() {
        Recorder recorder = new Recorder();
        add(recorder);
        IllegalStateException boom = new IllegalStateException("boom");
        OutputCapture.errorsOf(() -> Logger.LogException("Hook failed", boom));

        List<LogEntry> entries = recorder.all().stream()
                .filter(entry -> entry.error() == boom).collect(Collectors.toList());
        assertEquals(1, entries.size());
        assertEquals(ELogLevel.ERROR, entries.get(0).level());
        assertEquals(Logger.CATEGORY_ENGINE, entries.get(0).category());
        assertEquals("Hook failed", entries.get(0).message());
    }

    @Test
    void printedLinesAreUnchanged() {
        String plain = OutputCapture.errorsOf(() -> Logger.Warn(CATEGORY, "Unknown property 'x'"));
        assertEquals("--Unknown property 'x'" + System.lineSeparator(), plain);

        String withException = OutputCapture.errorsOf(
                () -> Logger.LogException("Hook failed", new IllegalStateException("boom")));
        String[] lines = withException.split("\\R");
        assertEquals("--Hook failed: java.lang.IllegalStateException: boom", lines[0]);
        assertEquals("java.lang.IllegalStateException: boom", lines[1]);
        assertTrue(lines[2].trim().startsWith("at "), lines[2]);
    }

    @Test
    void entriesFromSeveralThreads() throws InterruptedException {
        Recorder recorder = record();
        int threads = 4;
        int perThread = 200;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        String output = OutputCapture.errorsOf(() -> {
            for (int t = 0; t < threads; ++t) {
                Thread thread = new Thread(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; ++i)
                            Logger.Warn(CATEGORY, "from worker");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }, "logger-test-" + t);
                thread.start();
            }
            start.countDown();
            try {
                assertTrue(done.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<LogEntry> entries = recorder.entries();
        assertEquals(threads * perThread, entries.size());
        assertEquals(threads, entries.stream().map(LogEntry::thread).distinct().count());
        assertEquals(threads * perThread, OutputCapture.lines(output.trim()));
    }

    @Test
    void throwingListenerIsRemovedWithoutLoop() {
        Recorder recorder = record();
        int[] calls = new int[1];
        ILogListener throwing = entry -> {
            calls[0]++;
            throw new IllegalStateException("listener bug");
        };
        add(throwing);

        String output = OutputCapture.errorsOf(() -> {
            Logger.Error(CATEGORY, "one");
            Logger.Error(CATEGORY, "two");
        });

        assertEquals(1, calls[0]);
        assertFalse(Logger.RemoveListener(throwing));
        // The other listener still receives both, and the listener's failure is not a problem of the channel
        assertEquals(2, recorder.entries().size());
        assertEquals(0, recorder.all().stream().filter(entry -> entry.message().contains("listener")).count());
        assertTrue(output.contains("--Log listener removed, it threw: java.lang.IllegalStateException: listener bug"));
    }

    @Test
    void listenerThatLogsDoesNotLoop() {
        Recorder recorder = record();
        ILogListener relogging = entry -> {
            if (CATEGORY.equals(entry.category()))
                Logger.Error(CATEGORY, "again");
        };
        add(relogging);

        String output = OutputCapture.errorsOf(() -> Logger.Error(CATEGORY, "once"));

        assertEquals(1, recorder.entries().size());
        // Printed, never sent
        assertTrue(output.contains("--again"));
    }

    private static final class Recorder implements ILogListener {
        private final ConcurrentLinkedQueue<LogEntry> entries = new ConcurrentLinkedQueue<>();

        @Override
        public void onLog(LogEntry entry) {
            entries.add(entry);
        }

        List<LogEntry> all() {
            return new ArrayList<>(entries);
        }

        List<LogEntry> entries() {
            return entries.stream().filter(entry -> CATEGORY.equals(entry.category())).collect(Collectors.toList());
        }
    }
}
