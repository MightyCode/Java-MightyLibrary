package io.github.mightycode.mightylib.graphics.GUI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SliderPrecisionTest {
    @Test
    void negativePrecisionDisablesTheRounding() {
        assertEquals(-1, Slider.CapPrecision(-5));
        assertEquals(0.123456789123, Slider.RoundToPrecision(0.123456789123, -1));
    }

    @Test
    void tooLargePrecisionIsCapped() {
        assertEquals(9, Slider.CapPrecision(42));
        assertEquals(3, Slider.CapPrecision(3));
        assertEquals(0, Slider.CapPrecision(0));
    }

    @Test
    void roundsToTheNearestValue() {
        assertEquals(0.29, Slider.RoundToPrecision(0.29, 2));
        assertEquals(0.67, Slider.RoundToPrecision(0.666, 2));
        assertEquals(3.0, Slider.RoundToPrecision(2.7, 0));
        assertEquals(-1.25, Slider.RoundToPrecision(-1.249, 2));
    }

    @Test
    void largeValuesDoNotOverflow() {
        // The former rounding went through an int: 5 * 10^9 overflowed
        assertEquals(5.5, Slider.RoundToPrecision(5.5, 9));
        assertEquals(123456.5, Slider.RoundToPrecision(123456.5, 9));
    }

    @Test
    void keepsTheDoublePrecision() {
        // The former rounding went through a float: 1/3 lost its digits after the seventh
        assertEquals(0.333333333, Slider.RoundToPrecision(1.0 / 3.0, 9));
    }
}
