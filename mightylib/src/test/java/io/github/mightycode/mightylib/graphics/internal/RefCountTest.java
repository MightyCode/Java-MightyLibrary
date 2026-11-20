package io.github.mightycode.mightylib.graphics.internal;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RefCountTest {
    @Test
    void theLastReleaseFrees() {
        AtomicInteger frees = new AtomicInteger();
        RefCount count = new RefCount("thing", frees::incrementAndGet);

        count.retain();
        count.retain();
        assertEquals(2, count.references());

        assertFalse(count.release());
        assertEquals(0, frees.get());
        assertTrue(count.release());
        assertEquals(1, frees.get());
        assertEquals(0, count.references());
        assertTrue(count.isFreed());
    }

    @Test
    void releasedMoreThanRetainedIsAnError() {
        RefCount count = new RefCount("thing", () -> {});
        assertThrows(IllegalStateException.class, count::release);

        count.retain();
        count.release();
        assertThrows(IllegalStateException.class, count::release);
    }

    @Test
    void aFreedObjectCannotBeRetainedAgain() {
        RefCount count = new RefCount("thing", () -> {});
        count.retain();
        count.release();

        IllegalStateException e = assertThrows(IllegalStateException.class, count::retain);
        assertTrue(e.getMessage().startsWith("thing"), e.getMessage());
    }

    @Test
    void neverRetainedIsNotFreed() {
        AtomicInteger frees = new AtomicInteger();
        RefCount count = new RefCount("thing", frees::incrementAndGet);
        assertEquals(0, count.references());
        assertFalse(count.isFreed());
        assertEquals(0, frees.get());
    }
}
