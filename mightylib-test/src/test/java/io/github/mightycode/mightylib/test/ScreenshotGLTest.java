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
    static final int[] COLOR = {51, 102, 153};

    // Clears the window in a known color, nothing else
    public static final class FilledScene extends Scene {
        @Override
        public void launch(String[] args) {
            super.launch(args);
            setClearColor(COLOR[0], COLOR[1], COLOR[2], 1f);
        }

        @Override
        public void display() {
            clear();
        }
    }

    private static Window window(SceneRunner runner) {
        return runner.scene().context().getWindow();
    }

    private static void assertFilled(byte[] rgba, int width, int height) {
        for (int i = 0; i < width * height; ++i) {
            for (int channel = 0; channel < 3; ++channel) {
                int value = rgba[i * 4 + channel] & 0xFF;
                if (Math.abs(value - COLOR[channel]) > 1)
                    fail("Pixel " + (i % width) + "," + (i / width) + " channel " + channel + ": " + value);
            }
        }
    }

    @Test
    void thePixelsOfTheWindowAreReadAtTheEndOfItsDisplay(SceneRunner runner) {
        runner.start(FilledScene::new).untilLoaded().frames(1);
        Window window = window(runner);
        assertTrue(KitGraphicsAccess.readsBackBuffer(window),
                "This driver does not read the back buffer of a hidden window");

        CompletableFuture<KitGraphicsAccess.WindowImage> first = KitGraphicsAccess.requestImage(window);
        CompletableFuture<KitGraphicsAccess.WindowImage> second = KitGraphicsAccess.requestImage(window);
        assertFalse(first.isDone());
        runner.engine().frames(1);

        // Completed by the display itself, the same image for both
        assertTrue(first.isDone() && second.isDone());
        KitGraphicsAccess.WindowImage image = first.join();
        assertSame(image.rgba(), second.join().rgba());

        Vector2i size = window.getInfo().getSizeCopy();
        assertEquals(size.x, image.width());
        assertEquals(size.y, image.height());
        assertEquals(size.x * size.y * 4, image.rgba().length);
        assertFilled(image.rgba(), image.width(), image.height());
    }

    @Test
    void theScreenshotCommandWritesTheRegionOnTheWorkers(SceneRunner runner) throws Exception {
        runner.start(FilledScene::new).untilLoaded();
        File file = new File("target/kit-screenshot/region.png");
        if (file.exists())
            assertTrue(file.delete());

        CommandResult result = runner.execute("screenshot " + file.getPath() + " --rect 2 3 10 5");
        assertTrue(result.isDeferred(), result.toString());
        runner.engine().frames(1);
        CommandResult written = result.getDeferred().get(10, TimeUnit.SECONDS);
        assertTrue(written.isSuccess(), written.toString());

        BufferedImage image = ImageIO.read(file);
        assertEquals(10, image.getWidth());
        assertEquals(5, image.getHeight());
        int rgb = image.getRGB(4, 2);
        assertEquals(COLOR[0], (rgb >> 16) & 0xFF, 1);
        assertEquals(COLOR[1], (rgb >> 8) & 0xFF, 1);
        assertEquals(COLOR[2], rgb & 0xFF, 1);
    }
}
