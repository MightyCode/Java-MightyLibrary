package io.github.mightycode.mightylib.graphics.utils;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.main.Window;
import org.apiguardian.api.API;

import java.util.concurrent.CompletableFuture;

import static org.apiguardian.api.API.Status.INTERNAL;

/** Bridge of the test kit to the package-private capture of the windows (Screenshot). Not for the games. */
@API(status = INTERNAL)
public final class KitGraphicsAccess {
    private KitGraphicsAccess() {}

    /** The final image of a window: RGBA, rows from the top. */
    public static final class WindowImage {
        private final int width;
        private final int height;
        private final byte[] rgba;

        private WindowImage(int width, int height, byte[] rgba) {
            this.width = width;
            this.height = height;
            this.rgba = rgba;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        // Shared with the other requests of the same display: read only
        public byte[] rgba() {
            return rgba;
        }
    }

    // Main thread: completed by the next display of the window, before its buffers are swapped
    public static CompletableFuture<WindowImage> requestImage(Window window) {
        return Screenshot.requestPixels(window.getInfo().getWindowId())
                .thenApply(pixels -> new WindowImage(pixels.width, pixels.height, pixels.rgba));
    }

    // Main thread: false when the driver does not read the back buffer of this window back (hidden window)
    public static boolean readsBackBuffer(Window window) {
        GLContext previous = GLContext.currentOrNull();
        window.bindWindow();
        try {
            return Screenshot.readsBackBuffer();
        } finally {
            if (previous != null && !previous.isDestroyed())
                previous.makeCurrent();
        }
    }
}
