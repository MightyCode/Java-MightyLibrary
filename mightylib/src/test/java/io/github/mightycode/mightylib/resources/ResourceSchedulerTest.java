package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceSchedulerTest {
    @Test
    void processStopsAtTheDeadlineAfterAtLeastOneTask() throws InterruptedException {
        MainThreadTestAccess.setCurrentAsMain();
        ResourceScheduler scheduler = new ResourceScheduler(null, 1);
        AtomicInteger done = new AtomicInteger();

        // Posted from another thread: they go to the inbox
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 5; ++i)
                scheduler.runOnMainThread(done::incrementAndGet);
        });
        thread.start();
        thread.join();

        // Deadline already passed: one task is still done, so the loading progresses
        scheduler.processUntil(0);
        assertEquals(1, done.get());
        assertFalse(scheduler.isIdle());

        scheduler.processUntil(Long.MAX_VALUE);
        assertEquals(5, done.get());
        assertTrue(scheduler.isIdle());
    }
}
