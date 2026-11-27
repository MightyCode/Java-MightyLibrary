package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.utils.KitGraphicsAccess;
import io.github.mightycode.mightylib.main.Window;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// The image of the window read in memory at the end of its display, and the PNG of the screenshot command
@MightySceneTest(config = SceneRunnerTest.Config.class)
class ScreenshotGLTest {
    // Two colors, none of the check of Screenshot.readsBackBuffer: the image of a display is not the one before
    static final int[][] COLORS = {{20, 200, 90}, {180, 40, 120}};

    // Clears the window in a known color, another one at each display
    public static final class FilledScene extends Scene {
        private int displays;
        private int[] shown;

        @Override
        public void display() {
            shown = COLORS[displays++ % COLORS.length];
            setClearColor(shown[0], shown[1], shown[2], 1f);
            clear();
        }

        int[] shown() {
            return shown;
        }
    }

    private static Window window(SceneRunner runner) {
        return runner.scene().context().getWindow();
    }

    private static void assertFilled(byte[] rgba, int width, int height, int[] color) {
        for (int i = 0; i < width * height; ++i) {
            for (int channel = 0; channel < 3; ++channel) {
                int value = rgba[i * 4 + channel] & 0xFF;
                if (Math.abs(value - color[channel]) > 1)
                    fail("Pixel " + (i % width) + "," + (i / width) + " channel " + channel + ": " + value);
            }
        }
    }

    private static void assertColor(int expected, int rgb, String where) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs(((expected >> shift) & 0xFF) - ((rgb >> shift) & 0xFF)) > 1)
                fail(where + ": " + Integer.toHexString(rgb & 0xFFFFFF) + " instead of " + Integer.toHexString(expected));
        }
    }

    @Test
    void thePixelsOfTheWindowAreReadAtTheEndOfItsDisplay(SceneRunner runner) {
        runner.start(FilledScene::new).untilLoaded().frames(1);
        FilledScene scene = (FilledScene) runner.scene();
        Window window = window(runner);
        assertTrue(KitGraphicsAccess.readsBackBuffer(window),
                "This driver does not read the back buffer of a hidden window");

        int[] before = scene.shown();
        CompletableFuture<KitGraphicsAccess.WindowImage> first = KitGraphicsAccess.requestImage(window);
        CompletableFuture<KitGraphicsAccess.WindowImage> second = KitGraphicsAccess.requestImage(window);
        assertFalse(first.isDone());
        runner.engine().frames(1);

        // Completed by the display itself, the same image for both: the color of that display, not of the one before
        assertTrue(first.isDone() && second.isDone());
        assertNotSame(before, scene.shown(), "the scene was not displayed by the frame");
        KitGraphicsAccess.WindowImage image = first.join();
        assertSame(image.rgba(), second.join().rgba());

        Vector2i size = window.getInfo().getSizeCopy();
        assertEquals(size.x, image.width());
        assertEquals(size.y, image.height());
        assertEquals(size.x * size.y * 4, image.rgba().length);
        assertFilled(image.rgba(), image.width(), image.height(), scene.shown());
    }

    // The region is in pixels of the window from its top left corner, the PNG encoded by a worker
    @Test
    void theScreenshotCommandWritesTheRegionOnTheWorkers(SceneRunner runner) throws Exception {
        runner.start(SceneCaptureGLTest.RectangleScene::new).untilLoaded();
        File file = new File("target/kit-screenshot/region.png");
        if (file.exists())
            assertTrue(file.delete());

        // Over the top left corner of the rectangle: 2 columns and 1 row of background before it
        int x = SceneCaptureGLTest.X - 2, y = SceneCaptureGLTest.Y - 1;
        CommandResult result = runner.execute("screenshot " + file.getPath() + " --rect " + x + " " + y + " 10 5");
        assertTrue(result.isDeferred(), result.toString());
        // Attached before the capture: run by the thread that completes it
        CompletableFuture<String> thread = result.getDeferred().thenApply(written -> Thread.currentThread().getName());
        runner.engine().frames(1);
        CommandResult written = result.getDeferred().get(10, TimeUnit.SECONDS);
        assertTrue(written.isSuccess(), written.toString());
        assertTrue(thread.get(10, TimeUnit.SECONDS).startsWith("MightyLibrary-Worker-"), thread.get());

        BufferedImage image = ImageIO.read(file);
        assertEquals(10, image.getWidth());
        assertEquals(5, image.getHeight());
        assertColor(SceneCaptureGLTest.BACKGROUND, image.getRGB(0, 0), "top left");
        assertColor(SceneCaptureGLTest.BACKGROUND, image.getRGB(9, 0), "first row");
        assertColor(SceneCaptureGLTest.BACKGROUND, image.getRGB(1, 4), "first columns");
        assertColor(SceneCaptureGLTest.RECTANGLE, image.getRGB(2, 1), "corner of the rectangle");
        assertColor(SceneCaptureGLTest.RECTANGLE, image.getRGB(9, 4), "bottom right");
    }
}
