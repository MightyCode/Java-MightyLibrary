package io.github.mightycode.mightylib.graphics.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiveObjectsTest {
    @Test
    void registeredObjectsAreListedUntilUnregistered() {
        LiveObjects<String> live = new LiveObjects<>();
        String first = new String("first");
        String second = new String("second");
        live.register(first);
        live.register(second);
        assertEquals(2, live.count());

        live.unregister(first);
        assertEquals(1, live.count());
        assertSame(second, live.snapshot().get(0));
    }

    @Test
    void registeringDoesNotKeepAnObjectAlive() throws InterruptedException {
        LiveObjects<Object> live = new LiveObjects<>();
        live.register(new Object());

        for (int i = 0; i < 20 && live.count() > 0; ++i) {
            System.gc();
            Thread.sleep(20);
        }
        assertEquals(0, live.count());
    }
}
