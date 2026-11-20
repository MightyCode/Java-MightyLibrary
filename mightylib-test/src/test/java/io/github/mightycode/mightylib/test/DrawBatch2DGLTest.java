package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer.RenderState;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchStateTable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.Batches2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchMaterial;
import io.github.mightycode.mightylib.graphics.shader.Shader;
import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL30C.*;

// The batch on OpenGL 3.3 core: one draw call for a state, one per run, the ring over many flushes, read back in pixels
@MightySceneTest(config = SceneRunnerTest.Config.class)
class DrawBatch2DGLTest {
    private static final int SIZE = 64;
    private static final int CELL = 4;
    private static final int CELLS = SIZE / CELL;

    // Draws with the batch shader of the engine and its white texture: the color is the one of the vertices
    private static final class SpritePart implements IBatchMaterial {
        private final String name;
        private final Shader shader;
        private final Texture white;
        private final RenderState state = new RenderState(false, false, false);

        SpritePart(String name, Shader shader, Texture white) {
            this.name = name;
            this.shader = shader;
            this.white = white;
        }

        @Override
        public void apply(boolean linear, Matrix4f projection, Matrix4f view) {
            state.apply(linear);
            shader.use();
            white.bindRenderTexture(0);
            shader.sendValueToShader(new ShaderValue("projection", Matrix4f.class, projection));
            shader.sendValueToShader(new ShaderValue("view", Matrix4f.class, view));
            shader.sendValueToShader(new ShaderValue("mlLinearColors", Integer.class, linear ? 1 : 0));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SpritePart && ((SpritePart) other).name.equals(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    // A framebuffer of SIZE pixels, the shader and the texture of the engine, and a batch of the engine
    private static final class Fixture implements AutoCloseable {
        final Engine engine = Views.engine();
        final GLResources glResources = engine.glResources();
        final Batches2D batches = Batches2D.of(engine);
        final DrawBatch2D batch = new DrawBatch2D(batches);
        final Shader shader;
        final Texture white;
        final int texture;
        final int framebuffer;
        final int[] viewport = new int[4];
        final Matrix4f projection = new Matrix4f().ortho(0, SIZE, 0, SIZE, -1, 1);
        final Matrix4f view = new Matrix4f();

        Fixture() {
            shader = (Shader) glResources.acquire(
                    engine.resources().getResource(ShaderData.class, EngineResources.SPRITE_2D), true);
            white = (Texture) glResources.acquire(
                    engine.resources().getResource(TextureData.class, EngineResources.WHITE_TEXTURE), true);

            texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, SIZE, SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            framebuffer = glGenFramebuffers();
            glGetIntegerv(GL_VIEWPORT, viewport);
            RenderState.invalidateContextState();
        }

        void bind() {
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
            assertEquals(GL_FRAMEBUFFER_COMPLETE, glCheckFramebufferStatus(GL_FRAMEBUFFER));
            GLContext.setViewport(0, 0, SIZE, SIZE);
            glClearColor(0, 0, 0, 1);
            GLContext.ClearColorAndDepth();
        }

        int state(String name) {
            BatchStateTable table = batch.stateTable();
            return table.state(table.materialPart(new SpritePart(name, shader, white)),
                    table.camera(projection, view), false);
        }

        // One quad per cell, of the color given for its cell
        void drawCells(IntBinaryOperator states, IntBinaryOperator colors) {
            for (int y = 0; y < CELLS; ++y) {
                for (int x = 0; x < CELLS; ++x) {
                    float left = x * CELL, bottom = y * CELL;
                    batch.quad(states.applyAsInt(x, y), 0, 0f,
                            left, bottom + CELL, 0, 0,
                            left, bottom, 0, 1,
                            left + CELL, bottom, 1, 1,
                            left + CELL, bottom + CELL, 1, 0,
                            colors.applyAsInt(x, y));
                }
            }
        }

        // The color read at the center of each cell is the one drawn there
        void assertCells(IntBinaryOperator colors, String when) {
            ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
            glReadPixels(0, 0, SIZE, SIZE, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            for (int y = 0; y < CELLS; ++y) {
                for (int x = 0; x < CELLS; ++x) {
                    int at = ((y * CELL + CELL / 2) * SIZE + x * CELL + CELL / 2) * 4;
                    int read = (pixels.get(at) & 0xFF) << 24 | (pixels.get(at + 1) & 0xFF) << 16
                            | (pixels.get(at + 2) & 0xFF) << 8 | (pixels.get(at + 3) & 0xFF);
                    int expected = colors.applyAsInt(x, y);
                    if (read != expected)
                        fail(when + ": cell " + x + "," + y + " is " + Integer.toHexString(read) + ", expected "
                                + Integer.toHexString(expected));
                }
            }
        }

        @Override
        public void close() {
            batches.setMode(EBatch2DMode.ORPHAN);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(framebuffer);
            glDeleteTextures(texture);
            GLContext.setViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderState.invalidateContextState();
            glResources.releaseResource(shader);
            glResources.releaseResource(white);
        }
    }

    private static int color(int x, int y, int shift) {
        return ((x * 16 + shift) & 0xFF) << 24 | ((y * 16 + shift * 3) & 0xFF) << 16 | ((x * y + shift) & 0xFF) << 8
                | 0xFF;
    }

    private static Fixture fixture(SceneRunner runner) {
        runner.frames(1);
        Fixture fixture;
        try (Views.Scope ignored = Views.enter(runner.scene())) {
            fixture = new Fixture();
        }
        // The shader and the texture are sent with the time of the frames
        runner.frames(3);
        assertTrue(fixture.shader.isLoaded() && fixture.white.isLoaded());
        return fixture;
    }

    @Test
    void quadsOfOneStateAreOneDrawCall(SceneRunner runner) {
        try (Fixture fixture = fixture(runner); Views.Scope ignored = Views.enter(runner.scene())) {
            fixture.bind();
            int state = fixture.state("a");
            fixture.drawCells((x, y) -> state, (x, y) -> color(x, y, 0));
            fixture.batch.endView();

            assertEquals(CELLS * CELLS, fixture.batch.lastFrameDraws());
            assertEquals(1, fixture.batch.lastFrameDrawCalls());
            fixture.assertCells((x, y) -> color(x, y, 0), "orphan");
        }
    }

    @Test
    void alternatedStatesAreOneDrawCallEach(SceneRunner runner) {
        try (Fixture fixture = fixture(runner); Views.Scope ignored = Views.enter(runner.scene())) {
            fixture.bind();
            int a = fixture.state("a");
            int b = fixture.state("b");
            // By the index of the draw: no two neighbors share a state
            fixture.drawCells((x, y) -> (x + y * CELLS) % 2 == 0 ? a : b, (x, y) -> color(x, y, 7));
            fixture.batch.endView();

            assertEquals(CELLS * CELLS, fixture.batch.lastFrameDrawCalls());
            fixture.assertCells((x, y) -> color(x, y, 7), "alternated");
        }
    }

    // Many flushes go round the ring several times: each picture is the one just drawn
    @Test
    void theRingKeepsEachFlushIntact(SceneRunner runner) {
        try (Fixture fixture = fixture(runner); Views.Scope ignored = Views.enter(runner.scene())) {
            fixture.batches.setMode(EBatch2DMode.RING);
            fixture.bind();
            for (int round = 0; round < 90; ++round) {
                int shift = round;
                int state = fixture.state("a");
                // Two flushes in a round: the second covers the first
                fixture.drawCells((x, y) -> state, (x, y) -> color(x, y, shift + 100));
                GLContext.FlushCurrentDraws("test");
                fixture.drawCells((x, y) -> fixture.state("a"), (x, y) -> color(x, y, shift));
                fixture.batch.endView();

                assertEquals(1, fixture.batch.lastFrameBarriers());
                fixture.assertCells((x, y) -> color(x, y, shift), "ring round " + round);
            }
        }
    }

    // Back and forth between the modes: drawn alike, the same GL objects at the end (checked by the kit)
    @Test
    void theModeChangesWhileTheGameRuns(SceneRunner runner) {
        try (Fixture fixture = fixture(runner); Views.Scope ignored = Views.enter(runner.scene())) {
            fixture.bind();
            EBatch2DMode[] modes = {EBatch2DMode.RING, EBatch2DMode.INSTANCED, EBatch2DMode.ORPHAN, EBatch2DMode.OFF,
                    EBatch2DMode.INSTANCED, EBatch2DMode.RING, EBatch2DMode.ORPHAN};
            for (int i = 0; i < modes.length; ++i) {
                int shift = i * 11;
                fixture.batches.setMode(modes[i]);
                assertEquals(modes[i], fixture.batch.mode());
                int state = fixture.state("a");
                fixture.drawCells((x, y) -> state, (x, y) -> color(x, y, shift));
                fixture.batch.endView();
                fixture.assertCells((x, y) -> color(x, y, shift), "mode " + modes[i]);
            }
        }
    }
}
