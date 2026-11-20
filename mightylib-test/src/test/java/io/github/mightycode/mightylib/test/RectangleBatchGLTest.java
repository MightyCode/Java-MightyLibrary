package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer.EBlendMode;
import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.RenderState;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.Batches2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylib.utils.math.geometry.EFlip;
import io.github.mightycode.mightylib.utils.math.geometry.ERotation;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL30C.*;

// RectangleRenderer in the 2D batch: the same picture as drawn alone, one draw call for a state, alone when it can't
@MightySceneTest(config = SceneRunnerTest.Config.class)
class RectangleBatchGLTest {
    // A 2D texture of the test, different in every corner (a gradient and a grid): flips and rotations show
    private static final class Pattern implements IRenderTextureBindable {
        private static final int SIZE = 64;
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
    private static final class Capture implements AutoCloseable {
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
    private static String compare(ByteBuffer expected, ByteBuffer actual) {
        int different = 0, maximum = 0;
        for (int i = 0; i < expected.capacity(); ++i) {
            int difference = Math.abs((expected.get(i) & 0xFF) - (actual.get(i) & 0xFF));
            maximum = Math.max(maximum, difference);
            if (difference > 1)
                ++different;
        }

        return different + " channels differ by more than 1 (largest difference " + maximum + ")";
    }

    private static int countDifferent(ByteBuffer expected, ByteBuffer actual) {
        int different = 0;
        for (int i = 0; i < expected.capacity(); ++i) {
            if (Math.abs((expected.get(i) & 0xFF) - (actual.get(i) & 0xFF)) > 1)
                ++different;
        }
        return different;
    }

    private static void inScene(SceneRunner runner, Consumer<Scene> test) {
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

    // Textured rectangles (turned, flipped, a texture turned, several references), colored ones half
    // transparent over them, one additive, two on one material, one with a value of its own (drawn alone)
    private static List<RectangleRenderer> picture(Pattern pattern) {
        List<RectangleRenderer> renderers = new ArrayList<>();
        EFlip[] flips = EFlip.values();
        ERotation[] rotations = {ERotation.None, ERotation.HalfPi, ERotation.Pi, ERotation.ThreeHalfPi};
        EDirection[] references = {EDirection.LeftUp, EDirection.None, EDirection.RightDown, EDirection.Up};
        for (int i = 0; i < 8; ++i) {
            RectangleRenderer sprite = new RectangleRenderer(EngineResources.TEXTURE_2D);
            sprite.setMainTextureChannel(pattern);
            sprite.setTexturePosition(new Vector4f(0.05f * i, 0.05f * i + 0.3f, 0.1f, 0.45f));
            sprite.setTextureFlip(flips[i % flips.length]);
            sprite.setTextureRotation(rotations[i % rotations.length]);
            sprite.setReferenceDirection(references[i % references.length]);
            sprite.setSizePix(90 + i * 13, 70 + i * 7);
            sprite.setPosition(new Vector2f(80 + i * 120, 120 + (i % 3) * 140));
            sprite.setRotationZ(i * 0.4f - 1f);
            renderers.add(sprite);
        }

        for (int i = 0; i < 6; ++i) {
            RectangleRenderer colored = new RectangleRenderer(EngineResources.COLOR_SHAPE_2D);
            colored.setColorMode(new Color4f(0.2f * i, 1f - 0.15f * i, 0.5f, 0.5f));
            colored.setSizePix(140, 60);
            colored.setPosition(new Vector2f(60 + i * 150, 160 + (i % 2) * 120));
            colored.setRotationZ(-0.3f * i);
            if (i == 4)
                colored.setBlendMode(EBlendMode.ADDITIVE);
            renderers.add(colored);
        }

        Material shared = new Material(EngineResources.TEXTURE_2D).setMainTexture(pattern);
        for (int i = 0; i < 2; ++i) {
            RectangleRenderer sprite = new RectangleRenderer(shared);
            sprite.setSizePix(200, 100);
            sprite.setPosition(new Vector2f(300 + i * 250, 450));
            renderers.add(sprite);
        }

        // A value the batch does not give: drawn alone, between batched ones
        RectangleRenderer alone = new RectangleRenderer(EngineResources.COLOR_SHAPE_2D);
        alone.setColorMode(new Color4f(1f, 0.8f, 0.1f, 0.7f));
        alone.addShaderValue("mlViewportSize2", Float.class, 1f);
        alone.setSizePix(100, 100);
        alone.setPosition(new Vector2f(500, 200));
        renderers.add(8, alone);
        return renderers;
    }

    @Test
    void theBatchedPictureIsThePictureDrawnAlone(SceneRunner runner) {
        inScene(runner, scene -> {
            List<RectangleRenderer> renderers;
            Pattern pattern;
            try (Views.Scope ignored = Views.enter(scene)) {
                pattern = new Pattern();
                renderers = picture(pattern);
            }
            // The textures and the batch shader are sent with the time of the frames
            runner.frames(5);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                Engine engine = Views.engine();
                Runnable drawing = () -> renderers.forEach(RectangleRenderer::display);

                engine.setBatch2DMode(EBatch2DMode.OFF);
                ByteBuffer alone = capture.draw(scene, drawing);
                assertEquals(0, scene.batch2D().lastFrameDraws());

                engine.setBatch2DMode(EBatch2DMode.ORPHAN);
                ByteBuffer orphan = capture.draw(scene, drawing);
                assertEquals(renderers.size() - 1, scene.batch2D().lastFrameDraws());
                // The one with a value of its own cut the batch
                assertTrue(scene.batch2D().lastFrameBarrierCauses().containsKey(RectangleRenderer.class.getName()));

                engine.setBatch2DMode(EBatch2DMode.RING);
                ByteBuffer ring = capture.draw(scene, drawing);

                engine.setBatch2DMode(EBatch2DMode.INSTANCED);
                ByteBuffer instanced = capture.draw(scene, drawing);
                // The rectangles are drawn by instances (the one with a value of its own, alone)
                assertTrue(scene.batch2D().lastFrameInstancedDrawCalls() > 0);
                assertEquals(scene.batch2D().lastFrameDrawCalls(), scene.batch2D().lastFrameInstancedDrawCalls());

                int pixels = capture.size.x * capture.size.y * 4;
                assertTrue(countDifferent(alone, orphan) <= pixels / 2000, "orphan: " + compare(alone, orphan));
                assertTrue(countDifferent(alone, ring) <= pixels / 2000, "ring: " + compare(alone, ring));
                assertTrue(countDifferent(alone, instanced) <= pixels / 2000,
                        "instanced: " + compare(alone, instanced));
                System.out.println("--Batch against alone, orphan: " + compare(alone, orphan) + "; ring: "
                        + compare(alone, ring) + "; instanced: " + compare(alone, instanced));
            }

            try (Views.Scope ignored = Views.enter(scene)) {
                renderers.forEach(RectangleRenderer::unload);
                pattern.delete();
            }
        });
    }

    // Each renderer has its own material: equal contents are one state, a single draw call
    @Test
    void equalMaterialsAreOneDrawCall(SceneRunner runner) {
        inScene(runner, scene -> {
            List<RectangleRenderer> renderers = new ArrayList<>();
            try (Views.Scope ignored = Views.enter(scene)) {
                for (int i = 0; i < 50; ++i) {
                    RectangleRenderer colored = new RectangleRenderer(EngineResources.COLOR_SHAPE_2D);
                    colored.setColorMode(new Color4f(i / 50f, 0.5f, 0.2f, 1f));
                    colored.setSizePix(10, 10);
                    colored.setPosition(new Vector2f(i * 12, 10));
                    renderers.add(colored);
                }
            }
            runner.frames(3);

            try (Views.Scope ignored = Views.enter(scene)) {
                renderers.forEach(RectangleRenderer::display);
                scene.batch2D().endView();
                assertEquals(50, scene.batch2D().lastFrameDraws());
                assertEquals(1, scene.batch2D().lastFrameDrawCalls());

                // A change of one material: its own state, the run is cut around it
                renderers.get(25).setBlendMode(EBlendMode.ADDITIVE);
                renderers.forEach(RectangleRenderer::display);
                scene.batch2D().endView();
                assertEquals(3, scene.batch2D().lastFrameDrawCalls());

                renderers.forEach(RectangleRenderer::unload);
            }
        });
    }

    // A material changed gets another part; back to its first content, the first part again
    @Test
    void aChangedMaterialIsInternedAgain(SceneRunner runner) {
        inScene(runner, scene -> {
            Material material;
            try (Views.Scope ignored = Views.enter(scene)) {
                material = new Material(EngineResources.COLOR_SHAPE_2D);
                material.retain();
            }
            runner.frames(3);

            try (Views.Scope ignored = Views.enter(scene)) {
                DrawBatch2D batch = scene.batch2D();
                int first = material.batchPart(batch.stateTable());
                assertTrue(first >= 0);
                assertEquals(first, material.batchPart(batch.stateTable()));

                material.setBlendMode(EBlendMode.MULTIPLY);
                int multiply = material.batchPart(batch.stateTable());
                assertNotEquals(first, multiply);

                material.setBlendMode(EBlendMode.ALPHA);
                assertEquals(first, material.batchPart(batch.stateTable()));
                batch.endView();
                material.release();
            }
        });
    }

    // The explicit draws give the picture of the renderer with the same rectangle
    @Test
    void drawRegionIsARectangleRenderer(SceneRunner runner) {
        inScene(runner, scene -> {
            RectangleRenderer sprite;
            Material material;
            Pattern pattern;
            try (Views.Scope ignored = Views.enter(scene)) {
                pattern = new Pattern();
                sprite = new RectangleRenderer(EngineResources.TEXTURE_2D);
                sprite.setMainTextureChannel(pattern);
                sprite.setTexturePosition(new Vector4f(0.1f, 0.6f, 0.2f, 0.5f));
                sprite.setTextureFlip(EFlip.Horizontal);
                sprite.setReferenceDirection(EDirection.None);
                sprite.setSizePix(300, 180);
                sprite.setPosition(new Vector2f(400, 300));
                sprite.setRotationZ(0.7f);
                material = sprite.material();
            }
            runner.frames(5);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                Views.engine().setBatch2DMode(EBatch2DMode.OFF);
                ByteBuffer renderer = capture.draw(scene, sprite::display);

                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                // RectangularFace (x, y, z, w) = (u0, u1, v0, v1); drawRegion takes (u0, v0, u1, v1)
                ByteBuffer explicit = capture.draw(scene, () -> assertTrue(scene.batch2D().drawRegion(material,
                        new Vector2f(400, 300), new Vector2f(300, 180), new Vector2f(0.5f, 0.5f), 0.7f,
                        new Vector4f(0.1f, 0.2f, 0.6f, 0.5f), EFlip.Horizontal, null)));

                int pixels = capture.size.x * capture.size.y * 4;
                assertTrue(countDifferent(renderer, explicit) <= pixels / 2000,
                        "drawRegion: " + compare(renderer, explicit));
                sprite.unload();
                pattern.delete();
            }
        });
    }

    // Not created by the batch: the batches of the engine exist from its start
    @Test
    void theBatchesExistFromTheStart(SceneRunner runner) {
        runner.frames(1);
        try (Views.Scope ignored = Views.enter(runner.scene())) {
            Batches2D batches = Batches2D.of(Views.engine());
            assertNotNull(batches.whiteTexture());
            assertEquals(EBatch2DMode.ORPHAN, Views.engine().batch2DMode());
        }
    }
}
