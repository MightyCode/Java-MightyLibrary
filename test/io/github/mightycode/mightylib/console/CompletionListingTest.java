package io.github.mightycode.mightylib.console;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompletionListingTest {
    private int bells;

    @Test
    void sameChoicesRingInsteadOfBeingWrittenAgain() {
        ConsoleLog log = new ConsoleLog(10);
        CompletionListing listing = new CompletionListing(log, () -> ++bells);

        listing.accept("alpha  alpine");
        listing.accept("alpha  alpine");
        assertEquals(1, log.size());
        assertEquals(1, bells);

        // Other choices are written
        listing.accept("scene  screenshot");
        assertEquals(2, log.size());
        assertEquals(1, bells);
    }

    @Test
    void sameChoicesAreWrittenAgainOnceOtherLinesFollowThem() {
        ConsoleLog log = new ConsoleLog(10);
        CompletionListing listing = new CompletionListing(log, () -> ++bells);

        listing.accept("alpha  alpine");
        log.add(ConsoleLog.EType.OUT, null, "something else");
        listing.accept("alpha  alpine");

        assertEquals(3, log.size());
        assertEquals(0, bells);

        // Cleared log: the choices are no longer shown
        log.clear();
        listing.accept("alpha  alpine");
        assertEquals(1, log.size());
        assertEquals(0, bells);
    }
}
