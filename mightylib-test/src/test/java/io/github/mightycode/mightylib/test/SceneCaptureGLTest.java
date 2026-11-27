package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.scenes.Scene;
import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// SceneRunner.capture: the final image of the window, its top at the top
@MightySceneTest(config = SceneRunnerTest.Config.class)
class SceneCaptureGLTest {
    static final int BACKGROUND = 0x336699;
    static final int RECTANGLE = 0xCC9933;
    // The rectangle, from the top left of the window
    static final int X = 10, Y = 4, WIDTH = 20, HEIGHT = 6;

    // The background, and a rectangle near the top left (cleared in a scissor: origin at the bottom of the window)
    public static final class RectangleScene extends Scene {
        @Override
        public void display() {
            setClearColor(0x33, 0x66, 0x99, 1f);
            clear();
            int windowHeight = context().getWindow().getInfo().getSizeRef().y;
            setScissor(X, windowHeight - Y - HEIGHT, WIDTH, HEIGHT);
            setClearColor(0xCC, 0x99, 0x33, 1f);
            clear();
            clearScissor();
        }
    }

    private static void assertColor(int expected, int actual, String where) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs((expected >> shift & 0xFF) - (actual >> shift & 0xFF)) > 1)
                fail(where + ": " + Integer.toHexString(actual) + " instead of " + Integer.toHexString(expected));
        }
    }

    @Test
    void theRectangleIsWhereItWasDrawnTopAtTheTop(SceneRunner runner) {
        runner.start(RectangleScene::new).untilLoaded();
        CapturedImage image = runner.capture();

        Vector2i size = runner.scene().context().getWindow().getInfo().getSizeCopy();
        assertEquals(size.x, image.width());
        assertEquals(size.y, image.height());

        int inside = 0;
        for (int y = 0; y < Y + HEIGHT + 2; ++y) {
            for (int x = 0; x < X + WIDTH + 2; ++x) {
                boolean in = x >= X && x < X + WIDTH && y >= Y && y < Y + HEIGHT;
                assertColor(in ? RECTANGLE : BACKGROUND, image.rgb(x, y), x + "," + y);
                if (in)
                    ++inside;
            }
        }
        assertEquals(WIDTH * HEIGHT, inside);
        assertColor(BACKGROUND, image.rgb(image.width() - 1, image.height() - 1), "bottom right");

        // A part of the capture: the rectangle alone
        CapturedImage part = image.crop(X, Y, WIDTH, HEIGHT);
        assertColor(RECTANGLE, part.rgb(0, 0), "part top left");
        assertColor(RECTANGLE, part.rgb(WIDTH - 1, HEIGHT - 1), "part bottom right");
    }

    @Test
    void twoCapturesOfAStillSceneAreTheSame(SceneRunner runner) {
        runner.start(RectangleScene::new).untilLoaded();
        CapturedImage first = runner.capture();
        CapturedImage second = runner.capture();
        for (int y = 0; y < first.height(); ++y) {
            for (int x = 0; x < first.width(); ++x)
                assertEquals(first.rgb(x, y), second.rgb(x, y), x + "," + y);
        }
    }
}
