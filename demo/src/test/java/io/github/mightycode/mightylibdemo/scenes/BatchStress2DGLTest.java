package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawSortKeys;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.test.MightySceneTest;
import io.github.mightycode.mightylib.test.SceneRunner;
import io.github.mightycode.mightylibdemo.main.DemoTestConfig;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static io.github.mightycode.mightylibdemo.scenes.TileMapBatchGLTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * The acceptance scene of the 2D batch: its mix (animated slimes and tiles of one atlas, shapes with strokes, texts)
 * batched is the mix drawn alone in every mode, sorted by state in a handful of draw calls, and a frame allocates
 * nothing per object. The CPU time is measured apart (tools/stress.py: it depends on the machine).
 */
@MightySceneTest(config = DemoTestConfig.class)
class BatchStress2DGLTest {
    private static final EBatch2DMode[] BATCHED = {EBatch2DMode.ORPHAN, EBatch2DMode.RING, EBatch2DMode.INSTANCED};
    // The mix sorted by state: one draw call per state (sprites of the atlas, shapes, texts), whatever N
    private static final int SORTED_MIX_MAX_CALLS = 3;

    // The scene after F pressed modes times (1: shapes, 2: mix) and N doubled doublings times from 1000
    private static BatchStress2D start(SceneRunner runner, int modes, int doublings) {
        runner.start("BatchStress2D").untilLoaded();
        runner.frames(2);
        for (int i = 0; i < modes; ++i)
            runner.key(GLFW_KEY_F, 1).frames(2);
        for (int i = 0; i < doublings; ++i)
            runner.key(GLFW_KEY_PAGE_UP, 1).frames(2);
        runner.frames(3);
        return (BatchStress2D) runner.scene();
    }

    @Test
    void theMixBatchedIsTheMixDrawnAlone(SceneRunner runner) {
        BatchStress2D scene = start(runner, 2, 1);
        assertEquals(2000, scene.objectCount());

        try (Views.Scope ignored = Views.enter(scene)) {
            try {
                // Unsorted: each batched mode is the picture drawn alone
                Views.engine().setBatch2DMode(EBatch2DMode.OFF);
                ByteBuffer alone = capture(scene, scene::displayObjects);
                int drawn = drawnPixels(alone);
                assertTrue(drawn > 50_000, drawn + " pixels drawn");
                assertEveryModeDraws(scene, alone, "");

                // Sorted by state: the overlapping objects of other states change order (drawn alone, the draws are
                // never sorted), the same picture in each batched mode, in a handful of draw calls
                scene.setDrawSortKey(DrawSortKeys.layerThenState());
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                ByteBuffer sorted = capture(scene, scene::displayObjects);
                assertEquals(drawn, drawnPixels(sorted), alone.capacity() / 2000, "the same objects drawn, sorted");
                assertEveryModeDraws(scene, sorted, "sorted ");
            } finally {
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                scene.setDrawSortKey(null);
            }
        }
    }

    // Each batched mode draws the expected picture; sorted, in at most SORTED_MIX_MAX_CALLS draw calls
    private static void assertEveryModeDraws(BatchStress2D scene, ByteBuffer expected, String sort) {
        for (EBatch2DMode mode : BATCHED) {
            Views.engine().setBatch2DMode(mode);
            ByteBuffer batched = capture(scene, scene::displayObjects);
            int calls = scene.batch2D().lastFrameDrawCalls();
            int different = countDifferent(expected, batched);
            String name = sort + mode;
            System.out.println("--Mix of 2000, " + name + ": " + calls + " draw calls, " + different
                    + " channels differ");
            assertTrue(different <= expected.capacity() / 2000, name + ": " + different + " channels differ");
            if (!sort.isEmpty())
                assertTrue(calls <= SORTED_MIX_MAX_CALLS, name + ": " + calls + " draw calls");
        }
    }

    // The seven vector shapes mixed, a third with a stroke: fills, strokes and grids in one draw call (the texts on the
    // screen of the scene add theirs)
    @Test
    void theShapesMixedAreOneDrawCall(SceneRunner runner) {
        BatchStress2D scene = start(runner, 1, 1);
        try (Views.Scope ignored = Views.enter(scene)) {
            try {
                for (EBatch2DMode mode : BATCHED) {
                    Views.engine().setBatch2DMode(mode);
                    capture(scene, scene::displayObjects);
                    assertEquals(1, scene.batch2D().lastFrameDrawCalls(), mode.toString());
                }
            } finally {
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
            }
        }
    }

    // A frame of the mix (turning, animating, drawing) allocates nothing per object: as much at 4N as at N
    @Test
    void aFrameOfTheMixAllocatesNothingPerObject(SceneRunner runner) {
        start(runner, 2, 1);
        long atN = allocatedBy(runner, 60);

        runner.key(GLFW_KEY_PAGE_UP, 1).frames(2).key(GLFW_KEY_PAGE_UP, 1).frames(2);
        assertEquals(8000, ((BatchStress2D) runner.scene()).objectCount());
        long at4N = allocatedBy(runner, 60);

        // A few KB per frame whatever N (texts of the counters...); a byte per object and frame would be 360 KB more
        System.out.println("--Mix, 60 frames: " + atN + " bytes allocated at 2000 objects, " + at4N + " at 8000");
        assertTrue(at4N - atN < 16_000, atN + " bytes allocated at 2000 objects, " + at4N + " at 8000");
    }

    // Bytes allocated by this thread (the main thread of the engine) in count frames: the least of 6 windows after as
    // many frames to warm up (the first windows still run code the JIT has not compiled, which allocates more)
    private static long allocatedBy(SceneRunner runner, int count) {
        runner.frames(count);
        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().getId();
        long least = Long.MAX_VALUE;
        StringBuilder all = new StringBuilder();
        for (int window = 0; window < 6; ++window) {
            long before = threads.getThreadAllocatedBytes(thread);
            runner.frames(count);
            long allocated = threads.getThreadAllocatedBytes(thread) - before;
            all.append(allocated).append(' ');
            least = Math.min(least, allocated);
        }
        System.out.println("--Mix of " + ((BatchStress2D) runner.scene()).objectCount() + ", bytes allocated by "
                + count + " frames: " + all);
        return least;
    }
}
