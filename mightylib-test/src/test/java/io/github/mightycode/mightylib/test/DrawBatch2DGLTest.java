package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormats;
import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer.RenderState;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatchAttributeType;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchStateTable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.Batches2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchMaterial;
import io.github.mightycode.mightylib.graphics.shader.Shader;
import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL20C.*;

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
        public BatchFormat format() {
            return BatchFormats.SPRITE;
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
        // The shader and the texture are sent with the time of the frames: a fixed count of frames is not enough on a
        // slow machine
        runner.engine().loadUntilDone();
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

    // ---- Formats of the tests: an int and a color per vertex, a square per instance (gl_VertexID) ----

    private static final BatchFormat SHADED = BatchFormat.builder("shaded")
            .vertex("aPos", 2, EBatchAttributeType.FLOAT)
            .vertex("aShade", 1, EBatchAttributeType.INT)
            .vertex("aColor", 4, EBatchAttributeType.NORMALIZED_UBYTE)
            .build();
    private static final BatchFormat SQUARE = BatchFormat.builder("square")
            .instance("aOrigin", 2, EBatchAttributeType.FLOAT)
            .instance("aSize", 1, EBatchAttributeType.INT)
            .instance("aColor", 4, EBatchAttributeType.NORMALIZED_UBYTE)
            .template(null, new int[]{0, 1, 2, 0, 2, 3})
            .build();

    private static final String FRAGMENT = "#version 330 core\nin vec4 color;\nout vec4 fragColor;\n"
            + "void main() { fragColor = color; }\n";
    // The red of the color is the shade (0 to 255)
    private static final String SHADED_VERTEX = "#version 330 core\n"
            + "layout(location = 0) in vec2 aPos;\nlayout(location = 1) in int aShade;\n"
            + "layout(location = 2) in vec4 aColor;\nuniform mat4 projection;\nout vec4 color;\n"
            + "void main() { gl_Position = projection * vec4(aPos, 0.0, 1.0);"
            + " color = vec4(float(aShade) / 255.0, aColor.gba); }\n";
    private static final String SQUARE_VERTEX = "#version 330 core\n"
            + "layout(location = 0) in vec2 aOrigin;\nlayout(location = 1) in int aSize;\n"
            + "layout(location = 2) in vec4 aColor;\nuniform mat4 projection;\nout vec4 color;\n"
            + "void main() { vec2 corner = vec2(float(gl_VertexID == 1 || gl_VertexID == 2),"
            + " float(gl_VertexID >= 2));"
            + " gl_Position = projection * vec4(aOrigin + corner * float(aSize), 0.0, 1.0); color = aColor; }\n";

    // A program of the test, linked here
    private static int program(String vertex) {
        int program = GenDeleteResources.GenProgram();
        int[] stages = {glCreateShader(GL_VERTEX_SHADER), glCreateShader(GL_FRAGMENT_SHADER)};
        String[] sources = {vertex, FRAGMENT};
        for (int i = 0; i < 2; ++i) {
            glShaderSource(stages[i], sources[i]);
            glCompileShader(stages[i]);
            assertEquals(GL_TRUE, glGetShaderi(stages[i], GL_COMPILE_STATUS), glGetShaderInfoLog(stages[i]));
            glAttachShader(program, stages[i]);
        }
        glLinkProgram(program);
        assertEquals(GL_TRUE, glGetProgrami(program, GL_LINK_STATUS), glGetProgramInfoLog(program));
        for (int stage : stages)
            glDeleteShader(stage);
        return program;
    }

    // Draws with a program of the test, in its format
    private static final class FormatPart implements IBatchMaterial {
        private final BatchFormat format;
        private final int program;
        private final RenderState state = new RenderState(false, false, false);

        FormatPart(BatchFormat format, int program) {
            this.format = format;
            this.program = program;
        }

        @Override
        public BatchFormat format() {
            return format;
        }

        @Override
        public void apply(boolean linear, Matrix4f projection, Matrix4f view) {
            state.apply(linear);
            GLContext.currentState().useProgram(program);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                glUniformMatrix4fv(glGetUniformLocation(program, "projection"), false,
                        projection.get(stack.mallocFloat(16)));
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof FormatPart && ((FormatPart) other).program == program;
        }

        @Override
        public int hashCode() {
            return program;
        }
    }

    private static int formatState(Fixture fixture, BatchFormat format, int program) {
        BatchStateTable table = fixture.batch.stateTable();
        return table.state(table.materialPart(new FormatPart(format, program)),
                table.camera(fixture.projection, fixture.view), false);
    }

    /*
     * Each cell by its format, in turn: a sprite quad, a quad of the shaded format (the red its int), an instance of
     * the square format. Each batched mode draws the same picture, the formats cutting the runs.
     */
    @Test
    void formatsAreDrawnAlikeInEachMode(SceneRunner runner) {
        try (Fixture fixture = fixture(runner); Views.Scope ignored = Views.enter(runner.scene())) {
            int shaded = program(SHADED_VERTEX);
            int square = program(SQUARE_VERTEX);
            try {
                fixture.bind();
                for (EBatch2DMode mode : new EBatch2DMode[]{EBatch2DMode.ORPHAN, EBatch2DMode.RING,
                        EBatch2DMode.INSTANCED, EBatch2DMode.RING}) {
                    fixture.batches.setMode(mode);
                    int shift = mode.ordinal() * 13;
                    int sprite = fixture.state("a");
                    int shadedState = formatState(fixture, SHADED, shaded);
                    int squareState = formatState(fixture, SQUARE, square);
                    for (int y = 0; y < CELLS; ++y) {
                        for (int x = 0; x < CELLS; ++x)
                            drawCell(fixture, x, y, sprite, shadedState, squareState, color(x, y, shift));
                    }
                    fixture.batch.endView();

                    assertTrue(fixture.batch.lastFrameFormatCalls().keySet().containsAll(
                            java.util.List.of("shaded", "square")));
                    fixture.assertCells((x, y) -> color(x, y, shift), "formats in " + mode);
                }
            } finally {
                GenDeleteResources.DeleteProgram(shaded);
                GenDeleteResources.DeleteProgram(square);
                RenderState.invalidateContextState();
            }
        }
    }

    private static void drawCell(Fixture fixture, int x, int y, int sprite, int shaded, int square, int rgba) {
        float left = x * CELL, bottom = y * CELL;
        DrawBatch2D batch = fixture.batch;
        switch ((x + y * CELLS) % 3) {
            case 0:
                batch.quad(sprite, 0, 0f, left, bottom + CELL, 0, 0, left, bottom, 0, 1, left + CELL, bottom, 1, 1,
                        left + CELL, bottom + CELL, 1, 0, rgba);
                break;
            case 1:
                batch.beginDraw(shaded);
                float[] corners = {left, bottom + CELL, left, bottom, left + CELL, bottom, left + CELL, bottom + CELL};
                for (int corner = 0; corner < 4; ++corner) {
                    batch.put(corners[corner * 2], corners[corner * 2 + 1]);
                    batch.putInt(rgba >>> 24);
                    // The red of the color is not read
                    batch.putColor(rgba & 0x00FFFFFF);
                }
                for (int index : new int[]{0, 3, 2, 0, 2, 1})
                    batch.index(index);
                batch.endDraw();
                break;
            default:
                batch.beginDraw(square);
                batch.put(left, bottom);
                batch.putInt(CELL);
                batch.putColor(rgba);
                batch.endDraw();
                break;
        }
    }

    // The batch programs of the engine read the formats registered for them
    @Test
    void theBatchProgramsOfTheEngineReadTheirFormats(SceneRunner runner) {
        runner.frames(1);
        Engine engine = Views.engine();
        for (java.util.Map.Entry<String, BatchFormat> format : BatchFormats.engineFormats().entrySet()) {
            Shader program = (Shader) engine.glResources().acquire(
                    engine.resources().getResource(ShaderData.class, format.getKey()), true);
            try {
                runner.engine().loadUntilDone();
                assertTrue(program.isLoaded(), format.getKey());
                assertNull(format.getValue().check(program.attributes()), format.getKey());
                assertTrue(Batches2D.of(engine).readsFormat(program, format.getValue()), format.getKey());
            } finally {
                engine.glResources().releaseResource(program);
            }
        }
    }
}
