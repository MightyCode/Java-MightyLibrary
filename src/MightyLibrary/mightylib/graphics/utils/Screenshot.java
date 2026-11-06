package MightyLibrary.mightylib.graphics.utils;

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

/**
 * Capture of the rendered image to a PNG file.
 * The capture is done at the end of the next frame, before the buffers are swapped.
 * A part of the image can be kept: region in pixels of the window, origin at the top left (as in the PNG).
 */
public final class Screenshot {
    private Screenshot() {}

    private static class Request {
        private final File file;
        // x, y, width, height, null for the whole image
        private final int[] region;
        // Size factor applied after the crop, 1 to keep the size
        private final float scale;
        private final CompletableFuture<File> result;

        private Request(File file, int[] region, float scale) {
            this.file = file;
            this.region = region;
            this.scale = scale;
            this.result = new CompletableFuture<>();
        }
    }

    // Main thread only
    private static final List<Request> requests = new ArrayList<>();

    // Main thread only
    public static CompletableFuture<File> request(File file) {
        return request(file, null, 1f);
    }

    /**
     * Main thread only. region = {x, y, width, height} in pixels of the window (top left origin), null for the
     * whole image; it is clipped to the image, the future fails if nothing is left.
     * scale in ]0, 1] reduces the saved image (smooth), applied after the crop.
     */
    public static CompletableFuture<File> request(File file, int[] region, float scale) {
        if (scale <= 0f || scale > 1f)
            throw new IllegalArgumentException("Screenshot scale must be in ]0, 1] : " + scale);

        Request request = new Request(file, region == null ? null : region.clone(), scale);
        requests.add(request);

        return request.result;
    }

    // Main thread, once the frame is drawn in the default framebuffer and before the buffers are swapped
    public static void captureIfRequested(long windowId) {
        if (requests.isEmpty())
            return;

        int width, height;
        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        glfwGetFramebufferSize(windowId, widthBuffer, heightBuffer);
        width = widthBuffer.get(0);
        height = heightBuffer.get(0);

        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        glReadBuffer(GL_BACK);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        List<Request> capturedRequests = new ArrayList<>(requests);
        requests.clear();

        // Encoding a PNG is slow, it must not slow down the game
        Thread encoder = new Thread(() -> {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // OpenGL rows start from the bottom of the image
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    int index = (x + (height - 1 - y) * width) * 4;
                    int r = pixels.get(index) & 0xFF;
                    int g = pixels.get(index + 1) & 0xFF;
                    int b = pixels.get(index + 2) & 0xFF;

                    image.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }

            for (Request request : capturedRequests) {
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
        }, "MightyLibrary-Screenshot");

        encoder.setDaemon(true);
        encoder.start();
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
