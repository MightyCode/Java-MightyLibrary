package io.github.mightycode.mightylib.utils.math;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class IdentifiersTest {
    @Test
    void idCountsUpAndDownAndChains() {
        ID id = new ID(5);

        assertSame(id, id.increment());
        assertEquals(6, id.id);
        assertEquals(4, id.decrement().decrement().id);
        assertEquals(7, id.increment().increment().increment().id);
    }

    @Test
    void uuidsAreUniqueAndIncreasing() {
        UUID first = new UUID(), second = new UUID(), third = new UUID();

        assertTrue(second.id() > first.id());
        assertTrue(third.id() > second.id());
        int id = first.id();
        assertEquals(id, first.id(), "stable");
    }

    // The workers create data types too: ids made at the same time on several threads stay unique (the counter was
    // a plain int incremented without synchronization)
    @Test
    void uuidsMadeOnSeveralThreadsAreUnique() throws InterruptedException {
        int threads = 8, perThread = 20_000;
        Set<Integer> ids = ConcurrentHashMap.newKeySet();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; ++t) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < perThread; ++i)
                    ids.add(new UUID().id());
            });
            worker.start();
            workers.add(worker);
        }

        start.countDown();
        for (Thread worker : workers)
            worker.join();

        assertEquals(threads * perThread, ids.size());
    }
}
