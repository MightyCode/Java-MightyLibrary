package io.github.mightycode.mightylib.graphics.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

// The requests of pixels of a window without GL: cancelled for their window only, forgotten once cancelled
class ScreenshotRequestsTest {
    // Ids of windows that no GLFW window has in a test
    private static final long WINDOW = 0x7E57_0001L;
    private static final long OTHER = 0x7E57_0002L;

    @Test
    void aCancelledRequestFailsAndIsForgotten() {
        CompletableFuture<Screenshot.Pixels> cancelled = Screenshot.requestPixels(WINDOW);
        CompletableFuture<Screenshot.Pixels> other = Screenshot.requestPixels(OTHER);
        try {
            Screenshot.cancelPixelsRequests(WINDOW);
            CompletionException e = assertThrows(CompletionException.class, cancelled::join);
            assertInstanceOf(IOException.class, e.getCause());
            assertFalse(other.isDone(), "the request of another window is kept");

            // Nothing waits for this window any more: its display reads nothing (no GL call here)
            Screenshot.captureIfRequested(WINDOW);
        } finally {
            Screenshot.cancelRequests(OTHER);
        }
        assertTrue(other.isCompletedExceptionally());
    }
}
