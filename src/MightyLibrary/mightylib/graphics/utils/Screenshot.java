package MightyLibrary.mightylib.graphics.utils;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
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
 */
public final class Screenshot {
    private Screenshot() {}

    private static class Request {
        private final File file;
        private final CompletableFuture<File> result;

        private Request(File file) {
            this.file = file;
            this.result = new CompletableFuture<>();
        }
    }

    // Main thread only
    private static final List<Request> requests = new ArrayList<>();

    // Main thread only
    public static CompletableFuture<File> request(File file) {
        Request request = new Request(file);
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

                    ImageIO.write(image, "png", request.file);
                    request.result.complete(request.file);
                } catch (IOException e) {
                    request.result.completeExceptionally(e);
                }
            }
        }, "MightyLibrary-Screenshot");

        encoder.setDaemon(true);
        encoder.start();
    }
}
