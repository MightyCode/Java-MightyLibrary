package io.github.mightycode.mightylib.utils;

import io.github.mightycode.mightylib.main.utils.GameTimeControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimerTest {
    private final Timer timer = new Timer();

    @BeforeEach
    void halfASecondPerFrame() {
        GameTimeControl.setDeltaTime(0.5f);
    }

    @AfterEach
    void noTimeAnymore() {
        GameTimeControl.reset();
    }

    @Test
    void aNewTimerIsNotStartedAndDoesNotAdvance() {
        assertFalse(timer.isStarted());
        timer.update();
        assertEquals(0f, timer.getElapsedTime());
        assertFalse(timer.isFinished());
    }

    @Test
    void startedTimerAdvancesByTheFrameDurationUntilItsAimedTime() {
        timer.start(2f);
        assertTrue(timer.isStarted());
        assertEquals(2f, timer.getAimedTime());

        timer.update();
        timer.update();
        timer.update();
        assertEquals(1.5f, timer.getElapsedTime(), 1e-6);
        assertFalse(timer.isFinished());

        timer.update();
        assertEquals(2f, timer.getElapsedTime(), 1e-6);
        assertTrue(timer.isFinished());
    }

    @Test
    void elapsedTimeIsCappedByDefaultAndStopsAdvancingOnceFinished() {
        timer.start(0.75f);

        timer.update();
        timer.update();
        assertEquals(0.75f, timer.getElapsedTime(), 1e-6, "1 s elapsed, capped to the aimed time");
        timer.update();
        assertEquals(0.75f, timer.getElapsedTime(), 1e-6);
    }

    @Test
    void withoutTheCapTheOvershootIsKept() {
        timer.setElapsedTimeCapped(false);
        timer.start(0.75f);

        timer.update();
        timer.update();

        assertEquals(1f, timer.getElapsedTime(), 1e-6);
        assertTrue(timer.isFinished());
    }

    @Test
    void stoppedTimerKeepsItsTimeUntilRestarted() {
        timer.start(10f);
        timer.update();

        timer.stop();
        assertTrue(timer.isStopped());
        timer.update();
        assertEquals(0.5f, timer.getElapsedTime(), 1e-6);

        timer.restart();
        timer.update();
        assertEquals(1f, timer.getElapsedTime(), 1e-6);
    }

    @Test
    void resetStartBeginsAgain() {
        timer.start(1f);
        timer.update();
        timer.update();
        assertTrue(timer.isFinished());

        timer.resetStart();

        assertFalse(timer.isFinished());
        assertEquals(0f, timer.getElapsedTime());
        assertTrue(timer.isStarted());
    }

    @Test
    void resetStopLeavesTheTimerStoppedAndNotStarted() {
        timer.start(1f);
        timer.update();

        timer.resetStop();

        assertFalse(timer.isStarted());
        assertTrue(timer.isStopped());
        assertEquals(0f, timer.getElapsedTime());
        timer.update();
        assertEquals(0f, timer.getElapsedTime());
    }

    @Test
    void timeCanBeForced() {
        timer.start(10f);
        timer.forceTime(3f);
        assertEquals(3f, timer.getElapsedTime());
    }

    @Test
    void randomTimeStaysInsideTheDuration() {
        timer.start(4f);
        for (int i = 0; i < 100; ++i) {
            timer.forceRandomTime();
            assertTrue(timer.getElapsedTime() >= 0f && timer.getElapsedTime() < 4f);
        }
    }

    @Test
    void aTimerAimingAtZeroFinishesAtTheFirstUpdate() {
        timer.start(0f);
        timer.update();
        assertTrue(timer.isFinished());
    }
}
