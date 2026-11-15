package io.github.mightycode.mightylib.graphics.text;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawnTextsTest {
    @AfterEach
    void reset() {
        DrawnTexts.clear();
    }

    @Test
    void theTextsOfTheFrameInProgressAreNotGivenYet() {
        DrawnTexts.beginFrame();
        DrawnTexts.record("a", 0, 0, 1, 1, 10);

        assertTrue(DrawnTexts.last().isEmpty());
    }

    @Test
    void theLastCompleteFrameIsSortedFromTopToBottomThenLeftToRight() {
        DrawnTexts.beginFrame();
        DrawnTexts.record("second row", 5, 20, 1, 1, 10);
        DrawnTexts.record("first row right", 50, 10, 1, 1, 10);
        DrawnTexts.record("first row left", 5, 10, 1, 1, 10);
        DrawnTexts.beginFrame();

        List<DrawnTexts.Entry> texts = DrawnTexts.last();
        assertEquals(3, texts.size());
        assertEquals("first row left", texts.get(0).text());
        assertEquals("first row right", texts.get(1).text());
        assertEquals("second row", texts.get(2).text());
    }

    @Test
    void aNewFrameReplacesTheOldOne() {
        DrawnTexts.beginFrame();
        DrawnTexts.record("old", 0, 0, 1, 1, 10);
        DrawnTexts.beginFrame();
        DrawnTexts.record("new", 0, 0, 1, 1, 10);
        DrawnTexts.beginFrame();

        List<DrawnTexts.Entry> texts = DrawnTexts.last();
        assertEquals(1, texts.size());
        assertEquals("new", texts.get(0).text());
    }
}
