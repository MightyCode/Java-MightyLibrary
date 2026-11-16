package io.github.mightycode.mightylib.main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadingDeadlineTest {
    private static final double FRAME = 10_000_000;

    @Test
    void loadingScreenUsesTheWholeFrame() {
        assertEquals(9_000_000L, MainLoop.loadingDeadline(1_000_000L, 9_000_000L, FRAME, 0.3f, true));
    }

    @Test
    void sceneDisplayedUsesAShareOfTheFrame() {
        assertEquals(4_000_000L, MainLoop.loadingDeadline(1_000_000L, 9_000_000L, FRAME, 0.3f, false));
    }

    @Test
    void theShareNeverGoesPastTheFrameDeadline() {
        assertEquals(2_000_000L, MainLoop.loadingDeadline(1_000_000L, 2_000_000L, FRAME, 0.3f, false));
    }
}
