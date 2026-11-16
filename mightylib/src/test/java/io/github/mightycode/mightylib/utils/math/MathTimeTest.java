package io.github.mightycode.mightylib.utils.math;

import org.junit.jupiter.api.Test;

import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

// Ticks are microseconds since 0001-01-01 00:00:00
class MathTimeTest {
    @Test
    void theOriginIsTickZero() {
        assertEquals(0L, MathTime.datetimeStrToTick("0001-01-01 00:00:00"));
        assertEquals("0001-01-01 00:00:00", MathTime.tickToStr(0));
    }

    @Test
    void oneSecondIsAMillionTicks() {
        assertEquals(1_000_000L, MathTime.datetimeStrToTick("0001-01-01 00:00:01"));
        assertEquals(60_000_000L, MathTime.datetimeStrToTick("0001-01-01 00:01:00"));
        assertEquals("0001-01-01 00:00:01", MathTime.tickToStr(1_000_000L));
    }

    @Test
    void aDayIsEightySixThousandFourHundredSeconds() {
        assertEquals(86_400L * 1_000_000L, MathTime.datetimeStrToTick("0001-01-02 00:00:00"));
    }

    @Test
    void conversionRoundTrip() {
        String[] dates = {"2024-02-29 13:45:10", "1999-12-31 23:59:59", "0001-12-31 00:00:01", "2100-06-15 08:00:00"};
        for (String date : dates)
            assertEquals(date, MathTime.tickToStr(MathTime.datetimeStrToTick(date)));
    }

    @Test
    void ticksBelowASecondAreIgnoredWhenWritten() {
        assertEquals("0001-01-01 00:00:00", MathTime.tickToStr(999_999L));
    }

    @Test
    void customPattern() {
        long tick = MathTime.datetimeStrToTick("2024-03-09 07:05:03");
        assertEquals("09/03/2024", MathTime.tickToCustomizedStr(tick, "dd/MM/yyyy"));
        assertEquals("07h05", MathTime.tickToCustomizedStr(tick, "HH'h'mm"));
    }

    @Test
    void defaultPatternWritesTheTimeOfTheDay() {
        long tick = MathTime.datetimeStrToTick("2024-03-09 07:05:03");
        String text = MathTime.tickToCustomizedStr(tick);
        assertTrue(text.startsWith("09 "), text);
        assertTrue(text.endsWith("2024, 7 h 5 m 3 s"), text);
    }

    @Test
    void invalidDateIsRefused() {
        assertThrows(DateTimeParseException.class, () -> MathTime.datetimeStrToTick("yesterday"));
        assertThrows(DateTimeParseException.class, () -> MathTime.datetimeStrToTick("2024-13-01 00:00:00"));
    }
}
