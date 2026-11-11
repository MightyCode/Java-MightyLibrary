package io.github.mightycode.mightylib.inputs.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClickCounterTest {
    private static final long MS = 1_000_000L;

    @Test
    void doubleAndTripleClicksThenANewSeries() {
        ClickCounter counter = new ClickCounter();
        assertEquals(1, counter.press(10, 10, 0));
        assertEquals(2, counter.press(12, 13, 200 * MS));
        assertEquals(3, counter.press(14, 10, 400 * MS));
        // After a triple click: a new series
        assertEquals(1, counter.press(14, 10, 500 * MS));
    }

    @Test
    void tooLateOrTooFarStartsAgain() {
        ClickCounter counter = new ClickCounter();
        counter.press(10, 10, 0);
        assertEquals(1, counter.press(10, 10, 500 * MS));
        assertEquals(1, counter.press(15, 10, 600 * MS));
        assertEquals(2, counter.press(15, 6, 700 * MS));
    }

    @Test
    void resetEndsTheSeries() {
        ClickCounter counter = new ClickCounter();
        counter.press(10, 10, 0);
        counter.reset();
        assertEquals(1, counter.press(10, 10, 100 * MS));
    }
}
