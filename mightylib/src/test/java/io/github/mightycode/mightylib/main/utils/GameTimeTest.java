package io.github.mightycode.mightylib.main.utils;

import io.github.mightycode.mightylib.utils.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTimeTest {
    private static final long SECOND = 1_000_000_000L;

    private final AtomicLong now = new AtomicLong(10 * SECOND);

    @BeforeEach
    void manualClock() {
        GameTime.setClock(now::get);
    }

    @AfterEach
    void realClock() {
        GameTime.reset();
    }

    @Test
    void theTimeOnlyMovesWithTheClock() {
        assertEquals(10f, GameTime.currentTime(), 1e-6);
        GameTime.update();
        assertEquals(0f, GameTime.DeltaTime());

        now.addAndGet(SECOND / 4);
        assertEquals(0.25f, GameTime.getDeltaTSinceLastUpdate(), 1e-6);
        GameTime.update();
        assertEquals(0.25f, GameTime.DeltaTime(), 1e-6);
        assertEquals(10.25f, GameTime.currentTime(), 1e-6);
        assertEquals(0f, GameTime.getDeltaTSinceLastUpdate());
    }

    @Test
    void aTimerAdvancesWithTheManualClock() {
        Timer timer = new Timer();
        timer.start(1f);
        GameTime.update();
        for (int i = 0; i < 4; i++) {
            now.addAndGet(SECOND / 4);
            GameTime.update();
            timer.update();
        }
        assertTrue(timer.isFinished());
    }

    @Test
    void theFrameDurationCanBeSetWithoutWaiting() {
        GameTime.setDeltaTime(0.5f);
        assertEquals(0.5f, GameTime.DeltaTime());
    }

    @Test
    void resetGoesBackToTheRealTimeAndDefaultTick() {
        GameTime.setFixedDeltaTime(0.1f);
        GameTime.reset();
        assertEquals(1f / 60f, GameTime.FixedDeltaTime());
        assertEquals(0f, GameTime.DeltaTime());
    }
}
