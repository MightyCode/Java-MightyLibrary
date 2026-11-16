package io.github.mightycode.mightylib.inputs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyRepeatTest {
    private static final float TICK = 0.125f;

    @Test
    void oneStepAtThePressThenNothingUntilTheDelay() {
        KeyRepeat repeat = new KeyRepeat(0.5f, 0.25f);

        assertEquals(1, repeat.update(true, true, TICK));
        // 0.125 .. 0.375 s held: nothing yet
        for (int i = 0; i < 3; ++i)
            assertEquals(0, repeat.update(false, true, TICK), "tick " + i);
    }

    @Test
    void repeatsEveryIntervalAfterTheDelay() {
        KeyRepeat repeat = new KeyRepeat(0.5f, 0.25f);
        repeat.update(true, true, TICK);

        int total = 0;
        // From 0.125 s to 1.75 s: steps at 0.5, 0.75, 1, 1.25, 1.5, 1.75
        for (int i = 0; i < 14; ++i)
            total += repeat.update(false, true, TICK);

        assertEquals(6, total);
    }

    @Test
    void aSlowTickOwesSeveralSteps() {
        KeyRepeat repeat = new KeyRepeat(0.5f, 0.125f);
        repeat.update(true, true, TICK);

        // 1 s without a tick: 0.5 s of delay, then 5 steps of 0.125 s
        assertEquals(5, repeat.update(false, true, 1f));
    }

    @Test
    void releasingStopsAndTheNextPressStartsAgain() {
        KeyRepeat repeat = new KeyRepeat(0.25f, 0.125f);
        repeat.update(true, true, TICK);
        repeat.update(false, true, TICK);
        assertTrue(repeat.update(false, true, TICK) > 0);

        assertEquals(0, repeat.update(false, false, TICK));
        assertEquals(0, repeat.update(false, true, TICK), "held without a press: no repeat");
        assertEquals(1, repeat.update(true, true, TICK));
        assertEquals(0, repeat.update(false, true, TICK));
    }

    @Test
    void aPressReleasedInTheSameTickIsOneStep() {
        KeyRepeat repeat = new KeyRepeat();

        assertEquals(1, repeat.update(true, false, TICK));
        assertEquals(0, repeat.update(false, false, TICK));
    }

    @Test
    void aKeyHeldWithoutPressDoesNotRepeat() {
        KeyRepeat repeat = new KeyRepeat(0f, 0.1f);

        assertEquals(0, repeat.update(false, true, 5f));
    }

    @Test
    void resetForgetsTheKey() {
        KeyRepeat repeat = new KeyRepeat(0f, 0.1f);
        repeat.update(true, true, TICK);

        repeat.reset();
        assertEquals(0, repeat.update(false, true, 5f));
    }

    @Test
    void defaultsAreTheOnesOfTheSystemKeyboards() {
        KeyRepeat repeat = new KeyRepeat();

        assertEquals(0.5f, repeat.getDelay());
        assertEquals(1f / 30f, repeat.getInterval(), 1e-6);
    }

    @Test
    void refusesAnIntervalOfZero() {
        assertThrows(IllegalArgumentException.class, () -> new KeyRepeat(0.5f, 0f));
        assertThrows(IllegalArgumentException.class, () -> new KeyRepeat(-1f, 0.1f));
    }
}
