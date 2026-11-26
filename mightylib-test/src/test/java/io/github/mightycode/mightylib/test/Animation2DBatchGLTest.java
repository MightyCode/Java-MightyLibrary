package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.animation.Animation;
import io.github.mightycode.mightylib.resources.animation.AnimationData;
import io.github.mightycode.mightylib.resources.animation.Animator;
import io.github.mightycode.mightylib.resources.animation.FrameData;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.mightycode.mightylib.test.BatchPictures.*;
import static org.junit.jupiter.api.Assertions.*;

// Animation2DRenderer in the 2D batch: the same picture as drawn alone, one draw call for its frames
@MightySceneTest(config = SceneRunnerTest.Config.class)
class Animation2DBatchGLTest {
    // Animations without animator: the quad of their whole texture, flipped, shifted and turned
    @Test
    void theBatchedAnimationsAreTheAnimationsDrawnAlone(SceneRunner runner) {
        inScene(runner, scene -> {
            List<Animation2DRenderer> renderers = new ArrayList<>();
            Pattern pattern;
            EDirection[] shifts = {EDirection.None, EDirection.LeftUp, EDirection.RightDown, EDirection.Up};
            try (Views.Scope ignored = Views.enter(scene)) {
                pattern = new Pattern();
                for (int i = 0; i < 8; ++i) {
                    Animation2DRenderer animation = new Animation2DRenderer(EngineResources.TEXTURE_2D);
                    animation.setMainTextureChannel(pattern);
                    animation.setShiftRotation(shifts[i % shifts.length]);
                    animation.setHorizontalFlip(i % 2 == 1);
                    animation.setVerticalFlip(i % 4 >= 2);
                    animation.setScale(new Vector3f(80 + i * 11, 60 + i * 9, 1));
                    animation.setPosition(new Vector2f(100 + i * 110, 150 + (i % 3) * 150));
                    animation.setRotationZ(i * 0.5f - 1.5f);
                    renderers.add(animation);
                }
            }
            runner.frames(5);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                capture.assertEveryModeDrawsTheSame(scene, () -> renderers.forEach(Animation2DRenderer::display));
                assertEquals(renderers.size(), scene.batch2D().lastFrameDraws());
                assertEquals(1, scene.batch2D().lastFrameDrawCalls());

                renderers.forEach(Animation2DRenderer::unload);
                pattern.delete();
            }
        });
    }

    private static final int FRAMES = 4;

    // An animation of 4 frames of 8 x 10 pixels side by side (hot point i, 2), created by the code
    private static AnimationData fourFrames() {
        FrameData[] frames = new FrameData[FRAMES];
        for (int i = 0; i < FRAMES; ++i) {
            frames[i] = new FrameData();
            frames[i].init(i * 8 + ",0,8,10," + i + ",2,0.05", "test", i + 1);
        }
        AnimationData data = new AnimationData("frameChangeTest", null)
                .setTexture(EngineResources.WHITE_TEXTURE).setFramesData(frames);
        data.setPreloaded();
        return data;
    }

    // Animation.setFrame and the frame read without allocation (size, hot point mirrored by the flips)
    @Test
    void theFrameIsChosenAndReadInPlace(SceneRunner runner) {
        inScene(runner, scene -> {
            AnimationData data = fourFrames();
            try (Views.Scope ignored = Views.enter(scene)) {
                Views.engine().resources().addPreLoadedResource(data);
            }
            runner.frames(3);

            try (Views.Scope ignored = Views.enter(scene)) {
                try {
                    checkFrameOf(data);
                } finally {
                    Views.engine().resources().deleteResource(data);
                }
            }
        });
    }

    private static void checkFrameOf(AnimationData data) {
        assertTrue(data.isLoaded());
        Animation animation = new Animation();
        animation.init(data, true);
        animation.lateUpdate();
        animation.setFrame(2);
        assertEquals(2, animation.getCurrentFrame());
        assertTrue(animation.isCurrentFrameChanged(), "seen as a frame change");
        assertThrows(IndexOutOfBoundsException.class, () -> animation.setFrame(FRAMES));
        assertThrows(IndexOutOfBoundsException.class, () -> animation.setFrame(-1));

        Vector2i out = new Vector2i();
        assertSame(out, animation.getFrameSize(out));
        assertEquals(new Vector2i(8, 10), out);
        assertEquals(new Vector2i(2, 2), animation.currentHotPoint(false, false, out));
        assertEquals(new Vector2i(6, 8), animation.currentHotPoint(true, true, out));
        assertEquals(animation.currentHotPoint(true, false), animation.currentHotPoint(true, false, out));
        assertEquals(animation.currentTexturePosition(), animation.currentTexturePosition(new Vector4f()));
    }

    // A frame change of a renderer drawn by the batch allocates nothing and sends nothing to the GPU (the batch reads
    // the copy of the mesh on the CPU)
    @Test
    void aFrameChangeAllocatesAndSendsNothing(SceneRunner runner) {
        inScene(runner, scene -> {
            AnimationData data = fourFrames();
            try (Views.Scope ignored = Views.enter(scene)) {
                Views.engine().resources().addPreLoadedResource(data);
            }
            runner.frames(3);

            try (Views.Scope ignored = Views.enter(scene)) {
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                Animator animator = new Animator();
                animator.addAndInitAnimation("walk", data, true);
                Animation2DRenderer renderer = new Animation2DRenderer(EngineResources.TEXTURE_2D);
                renderer.init(animator);
                renderer.setPosition(new Vector2f(300, 200));
                renderer.setHorizontalFlip(true);
                Animation animation = animator.getCurrentAnimation();

                try {
                    // The cost of k changes against 4k: what grows with the count is allocated by each change
                    long[] allocated = new long[2];
                    for (int pass = 0; pass < 3; ++pass) {
                        for (int size = 0; size < 2; ++size) {
                            EngineStats.endSecond(1, 1, 0, 0);
                            allocated[size] = changeFrames(animation, renderer, size == 0 ? 1000 : 4000);
                            EngineStats.endSecond(1, 1, 0, 0);
                            long uploaded = EngineStats.toJson().getLong("bufferUploadBytesPerSecond");
                            scene.batch2D().endView();
                            assertEquals(0, uploaded, "the mesh sends nothing while batched");
                        }
                    }
                    // The first passes grow the buffers of the batch
                    assertTrue(allocated[1] - allocated[0] < 2_000, allocated[0] + " bytes allocated by 1000 frame "
                            + "changes, " + allocated[1] + " by 4000");
                } finally {
                    renderer.unload();
                    Views.engine().resources().deleteResource(data);
                }
            }
        });
    }

    // Bytes allocated by the thread for count frame changes of a renderer displayed after each
    private static long changeFrames(Animation animation, Animation2DRenderer renderer, int count) {
        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().getId();
        long before = threads.getThreadAllocatedBytes(thread);
        for (int i = 0; i < count; ++i) {
            animation.setFrame((i + 1) % FRAMES);
            renderer.update();
            renderer.display();
        }
        return threads.getThreadAllocatedBytes(thread) - before;
    }
}
