package io.github.mightycode.mightylib.graphics.utils;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.internal.GLStateCache;
import io.github.mightycode.mightylib.main.Workers;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

/**
 * Capture of the rendered image of a window, at the end of its next display, before the buffers are swapped.
 * Two steps: the pixels are read in memory on the main thread, then each request gets them: a PNG file (encoded by
 * the workers, a part of the image can be kept: region in pixels of the window, origin at the top left as in the
 * PNG), or the pixels themselves (test kit).
 */
public final class Screenshot {
    private Screenshot() {}

    // The final image of a window: RGBA, rows from the top of the image. Read only
    static final class Pixels {
        final int width;
        final int height;
        final byte[] rgba;

        Pixels(int width, int height, byte[] rgba) {
            this.width = width;
            this.height = height;
            this.rgba = rgba;
        }
    }

    private static final class FileRequest {
        private final File file;
        // x, y, width, height, null for the whole image
        private final int[] region;
        // Size factor applied after the crop, 1 to keep the size
        private final float scale;
        private final CompletableFuture<File> result = new CompletableFuture<>();
        private final long windowId;

        private FileRequest(File file, int[] region, float scale, long windowId) {
            this.windowId = windowId;
            this.file = file;
            this.region = region;
            this.scale = scale;
        }
    }

    private static final class PixelsRequest {
        private final CompletableFuture<Pixels> result = new CompletableFuture<>();
        private final long windowId;

        private PixelsRequest(long windowId) {
            this.windowId = windowId;
        }
    }

    // Main thread only
    private static final List<FileRequest> fileRequests = new ArrayList<>();
    private static final List<PixelsRequest> pixelsRequests = new ArrayList<>();

    // Main thread only
    public static CompletableFuture<File> request(File file, long windowId) {
        return request(file, null, 1f, windowId);
    }

    /**
     * Main thread only. region = {x, y, width, height} in pixels of the window (top left origin), null for the
     * whole image; it is clipped to the image, the future fails if nothing is left.
     * scale in ]0, 1] reduces the saved image (smooth), applied after the crop.
     */
    public static CompletableFuture<File> request(File file, int[] region, float scale, long windowId) {
        if (scale <= 0f || scale > 1f)
            throw new IllegalArgumentException("Screenshot scale must be in ]0, 1] : " + scale);

        FileRequest request = new FileRequest(file, region == null ? null : region.clone(), scale, windowId);
        fileRequests.add(request);

        return request.result;
    }

    /**
     * Main thread only: the final image of the window at the end of its next display, completed on the main thread
     * by that display (before the buffers are swapped). Test kit.
     */
    static CompletableFuture<Pixels> requestPixels(long windowId) {
        PixelsRequest request = new PixelsRequest(windowId);
        pixelsRequests.add(request);
        return request.result;
    }

    // Main thread: the requests of pixels of the window not completed yet fail (their caller gave up waiting)
    static void cancelPixelsRequests(long windowId) {
        IOException cancelled = new IOException("The request of the image of the window was cancelled");
        for (PixelsRequest request : takePixelsRequests(windowId))
            request.result.completeExceptionally(cancelled);
    }

    // The window is destroyed: what waited for its image fails instead of waiting forever
    public static void cancelRequests(long windowId) {
        IOException closed = new IOException("The window was closed before the screenshot");
        for (FileRequest request : takeFileRequests(windowId))
            request.result.completeExceptionally(closed);
        for (PixelsRequest request : takePixelsRequests(windowId))
            request.result.completeExceptionally(closed);
    }

    private static List<FileRequest> takeFileRequests(long windowId) {
        List<FileRequest> taken = new ArrayList<>();
        for (FileRequest request : fileRequests) {
            if (request.windowId == windowId)
                taken.add(request);
        }
        fileRequests.removeAll(taken);
        return taken;
    }

    private static List<PixelsRequest> takePixelsRequests(long windowId) {
        List<PixelsRequest> taken = new ArrayList<>();
        for (PixelsRequest request : pixelsRequests) {
            if (request.windowId == windowId)
                taken.add(request);
        }
        pixelsRequests.removeAll(taken);
        return taken;
    }

    // Main thread, once the frame is drawn in the default framebuffer and before the buffers are swapped
    public static void captureIfRequested(long windowId) {
        if (!isRequested(windowId))
            return;

        List<FileRequest> files = takeFileRequests(windowId);
        List<PixelsRequest> pixelsWaiting = takePixelsRequests(windowId);

        Pixels pixels;
        try {
            pixels = readBackBuffer(windowId);
        } catch (IOException e) {
            for (FileRequest request : files)
                request.result.completeExceptionally(e);
            for (PixelsRequest request : pixelsWaiting)
                request.result.completeExceptionally(e);
            return;
        }

        for (PixelsRequest request : pixelsWaiting)
            request.result.complete(pixels);

        // Encoding a PNG is slow, it must not slow down the game
        if (!files.isEmpty())
            Workers.execute(() -> writeFiles(pixels, files));
    }

    private static boolean isRequested(long windowId) {
        for (FileRequest request : fileRequests) {
            if (request.windowId == windowId)
                return true;
        }
        for (PixelsRequest request : pixelsRequests) {
            if (request.windowId == windowId)
                return true;
        }
        return false;
    }

    // The back buffer of the window (its context current), rows from the top
    private static Pixels readBackBuffer(long windowId) throws IOException {
        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        glfwGetFramebufferSize(windowId, widthBuffer, heightBuffer);
        int width = widthBuffer.get(0);
        int height = heightBuffer.get(0);

        // A minimized window has no image
        if (width <= 0 || height <= 0)
            throw new IOException("The window is minimized, it has no image");

        ByteBuffer read = BufferUtils.createByteBuffer(width * height * 4);
        glReadBuffer(GL_BACK);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, read);

        // OpenGL rows start from the bottom of the image
        byte[] rgba = new byte[width * height * 4];
        int row = width * 4;
        for (int y = 0; y < height; ++y) {
            read.position((height - 1 - y) * row);
            read.get(rgba, y * row, row);
        }

        return new Pixels(width, height, rgba);
    }

    /**
     * Main thread, the context of the window current: clears two pixels of its back buffer in known colors and reads
     * them back. False when the driver does not read the back buffer of this window (the pixels of a hidden window
     * may not belong to it). The back buffer is changed: the next display draws it again.
     */
    static boolean readsBackBuffer() {
        GLContext.FlushCurrentDraws(GLContext.CAUSE_FRAMEBUFFER);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        GLStateCache state = GLContext.currentState();
        state.capability(GLStateCache.FRAMEBUFFER_SRGB, false);

        int[][] colors = {{51, 102, 153}, {204, 153, 51}};
        for (int i = 0; i < colors.length; ++i) {
            state.scissor(i, 0, 1, 1);
            glClearColor(colors[i][0] / 255f, colors[i][1] / 255f, colors[i][2] / 255f, 1f);
            glClear(GL_COLOR_BUFFER_BIT);
        }
        state.disableScissor();

        ByteBuffer read = BufferUtils.createByteBuffer(2 * 4);
        glReadBuffer(GL_BACK);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, 2, 1, GL_RGBA, GL_UNSIGNED_BYTE, read);

        for (int i = 0; i < colors.length; ++i) {
            for (int channel = 0; channel < 3; ++channel) {
                if (Math.abs((read.get(i * 4 + channel) & 0xFF) - colors[i][channel]) > 1)
                    return false;
            }
        }
        return true;
    }

    // A worker: each file of the same image
    private static void writeFiles(Pixels pixels, List<FileRequest> files) {
        BufferedImage image = new BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < pixels.height; ++y) {
            for (int x = 0; x < pixels.width; ++x) {
                int index = (x + y * pixels.width) * 4;
                int r = pixels.rgba[index] & 0xFF;
                int g = pixels.rgba[index + 1] & 0xFF;
                int b = pixels.rgba[index + 2] & 0xFF;

                image.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        for (FileRequest request : files) {
            try {
                File parent = request.file.getAbsoluteFile().getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs())
                    throw new IOException("Can't create folder " + parent);

                ImageIO.write(scale(crop(image, request.region), request.scale), "png", request.file);
                request.result.complete(request.file);
            } catch (IOException | IllegalArgumentException e) {
                request.result.completeExceptionally(e);
            }
        }
    }

    private static BufferedImage scale(BufferedImage image, float scale) {
        if (scale >= 1f)
            return image;

        int width = Math.max(1, Math.round(image.getWidth() * scale));
        int height = Math.max(1, Math.round(image.getHeight() * scale));

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.drawImage(image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        graphics.dispose();

        return scaled;
    }

    private static BufferedImage crop(BufferedImage image, int[] region) {
        if (region == null)
            return image;

        int x = Math.max(0, region[0]);
        int y = Math.max(0, region[1]);
        int right = Math.min(image.getWidth(), region[0] + region[2]);
        int bottom = Math.min(image.getHeight(), region[1] + region[3]);

        if (right <= x || bottom <= y)
            throw new IllegalArgumentException("Screenshot region outside the image (" + image.getWidth() + "x"
                    + image.getHeight() + ")");

        return image.getSubimage(x, y, right - x, bottom - y);
    }
}
