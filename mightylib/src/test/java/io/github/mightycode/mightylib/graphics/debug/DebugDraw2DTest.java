package io.github.mightycode.mightylib.graphics.debug;

import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Vector2f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The debug drawings without GL: lifetimes asked once per frame, handles, switch, clear, no allocation
class DebugDraw2DTest {
    // A view the drawings are recorded for (drawn without batch here)
    private static final IView VIEW = new IView() {
        @Override
        public Engine engine() {
            return null;
        }

        @Override
        public WindowContext context() {
            return null;
        }

        @Override
        public Camera2D camera2D() {
            return null;
        }

        @Override
        public Camera3D camera3D() {
            return null;
        }
    };

    private static final Color4f RED = new Color4f(1f, 0f, 0f, 1f);

    private DebugDraws draws;
    private DebugDraw2D draw;

    @BeforeEach
    void create() {
        draws = new DebugDraws(true);
        draw = new DebugDraw2D(draws, VIEW, "View");
    }

    // A frame: the lifetimes asked, the drawings alive drawn (none here: no batch), those of one frame dropped
    private int frame() {
        draw.drawFrame(null);
        return draw.size();
    }

    @Test
    void aDrawingOfOneFrameIsDroppedAfterItsFrame() {
        DebugDrawHandle handle = draw.line(0, 0, 1, 1, RED);
        assertSame(DebugDrawHandle.NONE, handle);
        assertEquals(1, draw.size());
        assertEquals(0, frame());
    }

    @Test
    void aDrawingOfNFramesIsDrawnInNFrames() {
        draw.cross(EDebugSpace.WORLD, 0, 0, 4, RED, 1, DebugLifetimes.frames(3));
        // Drawn in its frame and the two after it, dropped in the fourth
        assertEquals(1, frame());
        assertEquals(1, frame());
        assertEquals(1, frame());
        assertEquals(0, frame());
    }

    // The time of the frames of the game: the frame it is asked in is not counted
    @Test
    void aDrawingOfSecondsFollowsTheTimeOfTheGame() {
        double[] frameDuration = {0.25};
        IDebugLifetime lifetime = DebugLifetimes.seconds(1f, () -> frameDuration[0]);
        draw.circle(EDebugSpace.WORLD, 0, 0, 5, RED, 1, lifetime);
        // Paused: no time passes
        frameDuration[0] = 0;
        for (int i = 0; i < 10; ++i)
            assertEquals(1, frame());
        frameDuration[0] = 0.25;
        assertEquals(1, frame());
        assertEquals(1, frame());
        assertEquals(1, frame());
        // 1 second passed
        assertEquals(0, frame());
    }

    @Test
    void aDrawingWhileTrueLivesWhileItsConditionIs() {
        boolean[] held = {true};
        draw.text(EDebugSpace.SCREEN, 10, 10, "held", RED, 16, DebugLifetimes.whileTrue(() -> held[0]));
        assertEquals(1, frame());
        assertEquals(1, frame());
        held[0] = false;
        assertEquals(0, frame());
    }

    @Test
    void aDrawingUntilRemovedLivesUntilItsHandleIsRemoved() {
        DebugDrawHandle handle = draw.rect(EDebugSpace.WORLD, 0, 0, 2, 2, RED, 1, DebugLifetimes.untilRemoved());
        for (int i = 0; i < 5; ++i)
            assertEquals(1, frame());
        handle.remove();
        assertTrue(handle.isRemoved());
        assertEquals(0, frame());
        // The handle of one frame removes nothing
        DebugDrawHandle.NONE.remove();
        assertFalse(DebugDrawHandle.NONE.isRemoved());
    }

    // The lifetimes are asked once per frame, in the order of the drawings; the others stay in their order
    @Test
    void eachLifetimeIsAskedOncePerFrame() {
        int[] asked = {0};
        draw.arrow(EDebugSpace.WORLD, 0, 0, 1, 0, RED, 1, () -> {
            ++asked[0];
            return true;
        });
        draw.line(0, 0, 1, 1, RED);
        draw.polyline(EDebugSpace.WORLD, List.of(new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1)), true,
                RED, 2, DebugLifetimes.frames(2));
        assertEquals(2, frame());
        assertEquals(1, asked[0]);
        assertEquals(1, draw.countByLifetime().get("frames"));
        // The polyline drawn in its second frame, then dropped
        assertEquals(2, frame());
        assertEquals(1, frame());
        assertEquals(3, asked[0]);
    }

    // Off: nothing recorded, the drawings waiting dropped; on again: recorded
    @Test
    void theSwitchOfTheEngineStopsTheDrawings() {
        draw.line(EDebugSpace.WORLD, 0, 0, 1, 1, RED, 1, DebugLifetimes.untilRemoved());
        draws.setEnabled(false);
        assertEquals(0, draw.size());
        assertSame(DebugDrawHandle.NONE, draw.circle(0, 0, 1, RED));
        assertEquals(0, draw.size());
        assertFalse(draw.isEnabled());

        draws.setEnabled(true);
        draw.circle(0, 0, 1, RED);
        assertEquals(1, draw.size());
    }

    @Test
    void clearDropsEveryLifetime() {
        draw.line(EDebugSpace.WORLD, 0, 0, 1, 1, RED, 1, DebugLifetimes.untilRemoved());
        draw.line(EDebugSpace.WORLD, 0, 0, 1, 1, RED, 1, DebugLifetimes.seconds(10));
        draws.clear();
        assertEquals(0, draw.size());
        assertEquals(0, draws.describe().getInt("drawings"));
    }

    // The values of the drawings kept move down with them when others are dropped
    @Test
    void theValuesFollowTheirDrawing() {
        DebugDrawQueue queue = new DebugDrawQueue();
        queue.add(DebugDrawQueue.LINE, EDebugSpace.WORLD, 1, 1f, null, DebugLifetimes.oneFrame());
        queue.value(1);
        queue.value(2);
        queue.add(DebugDrawQueue.CROSS, EDebugSpace.SCREEN, 2, 3f, null, DebugLifetimes.frames(5));
        queue.value(7);
        queue.value(8);
        queue.value(9);
        queue.beginFrame();
        queue.endFrame();

        assertEquals(1, queue.size());
        assertEquals(DebugDrawQueue.CROSS, queue.kind(0));
        assertEquals(EDebugSpace.SCREEN, queue.space(0));
        assertEquals(3, queue.valueCount(0));
        assertEquals(7f, queue.value(0, 0));
        assertEquals(9f, queue.value(0, 2));
    }

    // Drawings of one frame asked at each frame: nothing grows once the frames are stable
    @Test
    void nothingGrowsOnceTheFramesAreStable() {
        DebugDrawQueue queue = new DebugDrawQueue();
        int[] capacities = null;
        for (int frame = 0; frame < 5; ++frame) {
            for (int i = 0; i < 300; ++i) {
                assertSame(DebugDrawHandle.NONE, queue.add(DebugDrawQueue.LINE, EDebugSpace.WORLD, i, 1f, null,
                        DebugLifetimes.oneFrame()));
                for (int v = 0; v < 4; ++v)
                    queue.value(v);
            }
            queue.beginFrame();
            queue.endFrame();
            assertEquals(0, queue.size());

            int[] now = {queue.capacity(), queue.valueCapacity()};
            if (frame > 0)
                assertArrayEquals(capacities, now);
            capacities = now;
        }
    }
}
