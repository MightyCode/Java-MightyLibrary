package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer.RenderState;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.scenes.Scene;
import org.joml.Vector2i;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL30C.*;

// What the GL tests of the batch share: a texture of the test, the capture of a picture, the comparison of two
final class BatchPictures {
    private BatchPictures() {}

    // A 2D texture of the test, different in every corner (a gradient and a grid): flips and rotations show
    static final class Pattern implements IRenderTextureBindable {
        static final int SIZE = 64;
        final int id;

        Pattern() {
            ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
            for (int y = 0; y < SIZE; ++y) {
                for (int x = 0; x < SIZE; ++x) {
                    pixels.put((byte) (x * 4)).put((byte) (y * 4)).put((byte) ((x / 8 + y / 8) % 2 * 200))
                            .put((byte) 255);
                }
            }
            pixels.flip();

            id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, SIZE, SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            RenderState.invalidateContextState();
        }

        @Override
        public void bindRenderTexture(int position) {
            GLContext.currentState().bindTexture(position, GL_TEXTURE_2D, id);
        }

        @Override public int getWidth() { return SIZE; }
        @Override public int getHeight() { return SIZE; }
        @Override public int getRenderTextureId() { return id; }

        void delete() {
            GLContext.forgetTexture(id);
            glDeleteTextures(id);
        }
    }

    // The draws of the scene go to a framebuffer of the size of its camera, read back
    static final class Capture implements AutoCloseable {
        final Vector2i size;
        final int texture;
        final int framebuffer;
        final int[] viewport = new int[4];

        Capture(Scene scene) {
            size = scene.context().getWindow().getInfo().getVirtualSizeCopy();
            texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, size.x, size.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            framebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
            assertEquals(GL_FRAMEBUFFER_COMPLETE, glCheckFramebufferStatus(GL_FRAMEBUFFER));
            glGetIntegerv(GL_VIEWPORT, viewport);
            GLContext.setViewport(0, 0, size.x, size.y);
            RenderState.invalidateContextState();
        }

        ByteBuffer draw(Scene scene, Runnable drawing) {
            glClearColor(0.2f, 0.3f, 0.4f, 1f);
            GLContext.ClearColorAndDepth();
            drawing.run();
            scene.batch2D().endView();

            ByteBuffer pixels = BufferUtils.createByteBuffer(size.x * size.y * 4);
            glReadPixels(0, 0, size.x, size.y, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            return pixels;
        }

        // The picture drawn alone (mode OFF) is the picture of every batched mode
        void assertEveryModeDrawsTheSame(Scene scene, Runnable drawing) {
            Engine engine = Views.engine();
            engine.setBatch2DMode(EBatch2DMode.OFF);
            ByteBuffer alone = draw(scene, drawing);
            int drawn = drawnPixels(alone);
            assertTrue(drawn > 200, "Almost nothing drawn: " + drawn + " pixels");
            StringBuilder report = new StringBuilder("--Batch against alone (" + drawn + " pixels drawn)");
            for (EBatch2DMode mode : EBatch2DMode.values()) {
                if (mode == EBatch2DMode.OFF)
                    continue;

                engine.setBatch2DMode(mode);
                ByteBuffer batched = draw(scene, drawing);
                assertTrue(scene.batch2D().lastFrameDraws() > 0, mode + ": nothing batched");
                assertTrue(countDifferent(alone, batched) <= size.x * size.y * 4 / 2000,
                        mode + ": " + compare(alone, batched));
                report.append(", ").append(mode).append(": ").append(compare(alone, batched));
            }
            System.out.println(report);
        }

        @Override
        public void close() {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(framebuffer);
            glDeleteTextures(texture);
            GLContext.setViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderState.invalidateContextState();
        }
    }

    // Pixels whose channels differ (by more than 1: the rounding of the vertices may differ between the two ways)
    static String compare(ByteBuffer expected, ByteBuffer actual) {
        int different = 0, maximum = 0;
        for (int i = 0; i < expected.capacity(); ++i) {
            int difference = Math.abs((expected.get(i) & 0xFF) - (actual.get(i) & 0xFF));
            maximum = Math.max(maximum, difference);
            if (difference > 1)
                ++different;
        }

        return different + " channels differ by more than 1 (largest difference " + maximum + ")";
    }

    // Pixels not of the clear color of Capture.draw
    static int drawnPixels(ByteBuffer picture) {
        int drawn = 0;
        for (int i = 0; i < picture.capacity(); i += 4) {
            if (Math.abs((picture.get(i) & 0xFF) - 51) > 2 || Math.abs((picture.get(i + 1) & 0xFF) - 77) > 2
                    || Math.abs((picture.get(i + 2) & 0xFF) - 102) > 2)
                ++drawn;
        }
        return drawn;
    }

    static int countDifferent(ByteBuffer expected, ByteBuffer actual) {
        int different = 0;
        for (int i = 0; i < expected.capacity(); ++i) {
            if (Math.abs((expected.get(i) & 0xFF) - (actual.get(i) & 0xFF)) > 1)
                ++different;
        }
        return different;
    }

    // Runs the test in the scene of the runner, the batch back to its default mode after it
    static void inScene(SceneRunner runner, Consumer<Scene> test) {
        runner.frames(1);
        Scene scene = runner.scene();
        Engine engine;
        try (Views.Scope ignored = Views.enter(scene)) {
            engine = Views.engine();
        }

        try {
            test.accept(scene);
        } finally {
            try (Views.Scope ignored = Views.enter(scene)) {
                engine.setBatch2DMode(EBatch2DMode.ORPHAN);
            }
        }
    }
}
