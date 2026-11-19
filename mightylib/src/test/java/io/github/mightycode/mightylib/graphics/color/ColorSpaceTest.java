package io.github.mightycode.mightylib.graphics.color;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static io.github.mightycode.mightylib.graphics.color.ERenderColorSpace.*;
import static org.junit.jupiter.api.Assertions.*;

// The mode of a draw: renderer, then view, then engine (INHERIT or null takes the level above)
class ColorSpaceTest {
    private static final ERenderColorSpace[] LEVELS = { null, INHERIT, LINEAR, GAMMA };

    @Test
    void everyCombination() {
        for (ERenderColorSpace renderer : LEVELS) {
            for (ERenderColorSpace view : LEVELS) {
                for (ERenderColorSpace engine : new ERenderColorSpace[]{ LINEAR, GAMMA }) {
                    ERenderColorSpace expected = renderer != null && renderer != INHERIT ? renderer
                            : view != null && view != INHERIT ? view : engine;
                    assertEquals(expected, Resolve(renderer, view, engine), renderer + " " + view + " " + engine);
                }
            }
        }
    }

    @Test
    void namedCases() {
        assertEquals(GAMMA, Resolve(INHERIT, INHERIT, GAMMA));
        assertEquals(LINEAR, Resolve(INHERIT, LINEAR, GAMMA));
        assertEquals(GAMMA, Resolve(GAMMA, LINEAR, LINEAR));
        assertEquals(LINEAR, Resolve(LINEAR, GAMMA, GAMMA));
    }

    @Test
    void theEngineIsNeverInherit() {
        assertThrows(IllegalArgumentException.class, () -> Resolve(INHERIT, INHERIT, INHERIT));
        assertThrows(IllegalArgumentException.class, () -> Resolve(LINEAR, LINEAR, null));
        assertThrows(IllegalArgumentException.class, () -> new ColorService(INHERIT));
    }

    @Test
    void parse() {
        assertEquals(LINEAR, ERenderColorSpace.parse(" Linear "));
        assertEquals(INHERIT, ERenderColorSpace.parse("inherit"));
        assertNull(ERenderColorSpace.parse("srgb"));
        assertNull(ERenderColorSpace.parse(null));
        assertTrue(LINEAR.isLinear());
        assertFalse(GAMMA.isLinear());
        assertFalse(INHERIT.isLinear());
    }

    @Test
    void serviceChangesItsModeOnTheMainThread() throws InterruptedException {
        MainThreadTestAccess.setCurrentAsMain();
        ColorService service = new ColorService(GAMMA);
        assertEquals(GAMMA, service.resolve(INHERIT, null));

        service.setColorSpace(LINEAR);
        assertEquals(LINEAR, service.colorSpace());
        assertEquals(LINEAR, service.resolve(null, INHERIT));
        assertEquals(GAMMA, service.resolve(null, GAMMA));
        assertThrows(IllegalArgumentException.class, () -> service.setColorSpace(INHERIT));
        assertEquals(LINEAR, service.colorSpace());

        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread other = new Thread(() -> {
            try {
                service.setColorSpace(GAMMA);
            } catch (Throwable e) {
                error.set(e);
            }
        });
        other.start();
        other.join();
        assertInstanceOf(IllegalStateException.class, error.get());
        assertEquals(LINEAR, service.colorSpace());
    }
}
