package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.debug.DebugDraw2D;
import io.github.mightycode.mightylib.graphics.debug.DebugLifetimes;
import io.github.mightycode.mightylib.graphics.debug.EDebugSpace;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.test.BatchPictures.Capture;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static io.github.mightycode.mightylib.test.BatchPictures.drawnPixels;
import static io.github.mightycode.mightylib.test.BatchPictures.inScene;
import static org.junit.jupiter.api.Assertions.*;

// The debug drawings of a scene, in its 2D batch: drawn in every mode, their materials released by delete
@MightySceneTest(config = SceneRunnerTest.Config.class)
class DebugDraw2DGLTest {
    private static final Color4f RED = new Color4f(1f, 0f, 0f, 1f);
    private static final Color4f GREEN = new Color4f(0f, 1f, 0f, 1f);

    // One of each drawing, in the world and on the screen, kept until removed
    private static void record(DebugDraw2D draw) {
        draw.line(EDebugSpace.WORLD, 20, 20, 300, 120, RED, 3, DebugLifetimes.untilRemoved());
        draw.polyline(EDebugSpace.WORLD, List.of(new Vector2f(50, 300), new Vector2f(150, 250),
                new Vector2f(250, 330)), true, GREEN, 2, DebugLifetimes.untilRemoved());
        draw.rect(EDebugSpace.SCREEN, 400, 50, 200, 100, GREEN, 4, DebugLifetimes.untilRemoved());
        draw.circle(EDebugSpace.WORLD, 600, 350, 60, RED, 3, DebugLifetimes.untilRemoved());
        draw.cross(EDebugSpace.SCREEN, 100, 450, 40, GREEN, 2, DebugLifetimes.untilRemoved());
        draw.arrow(EDebugSpace.WORLD, 300, 450, 500, 500, RED, 2, DebugLifetimes.untilRemoved());
        draw.text(EDebugSpace.SCREEN, 600, 50, "debug\ntext", GREEN, 24, DebugLifetimes.untilRemoved());
    }

    private static ByteBuffer drawn(Capture capture, Scene scene) {
        return capture.draw(scene, () -> scene.debugDraw().drawFrame(scene.batch2D()));
    }

    @Test
    void theDrawingsAreDrawnInEveryMode(SceneRunner runner) {
        inScene(runner, scene -> {
            try (Views.Scope ignored = Views.enter(scene)) {
                record(scene.debugDraw());
            }
            // The materials are made at the first frame drawn, their programs sent with the time of the frames
            runner.frames(2);
            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                drawn(capture, scene);
            }
            runner.frames(8);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                int expected = -1;
                for (EBatch2DMode mode : EBatch2DMode.values()) {
                    Views.engine().setBatch2DMode(mode);
                    int pixels = drawnPixels(drawn(capture, scene));
                    assertTrue(pixels > 2000, mode + ": " + pixels + " pixels drawn");
                    if (expected < 0)
                        expected = pixels;
                    assertEquals(expected, pixels, 50, mode.toString());
                    // Sent by the flush after them (the one before had nothing waiting)
                    if (mode.isBatched())
                        assertEquals(1, scene.batch2D().lastFrameBarriers(), mode.toString());
                }

                // Removed: nothing drawn
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                scene.debugDraw().clear();
                assertTrue(drawnPixels(drawn(capture, scene)) == 0);

                // The materials given back (checked by the leak check of the kit)
                scene.debugDraw().delete();
            }
        });
    }
}
