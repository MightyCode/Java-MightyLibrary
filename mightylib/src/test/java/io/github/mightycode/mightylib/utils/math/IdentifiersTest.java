package io.github.mightycode.mightylib.utils.math;

import org.junit.jupiter.api.Test;

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
        assertEquals(first.id(), first.id(), "stable");
    }
}
