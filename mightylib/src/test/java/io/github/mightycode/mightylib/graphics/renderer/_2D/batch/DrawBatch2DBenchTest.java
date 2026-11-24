package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchGeometry2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchStateTable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchBackend;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchMaterial;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchTarget;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchUploader;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/*
 * Bench of the CPU cost of one draw of the batch without OpenGL: the writing of a rectangle, the culling test, the
 * send of the runs to an uploader that does nothing. Only with -Dmightylib.bench=true (python tools/bench.py): it
 * measures, it checks nothing. One JSON line per case: ns per draw, median of the passes after a warm-up.
 */
@EnabledIfSystemProperty(named = "mightylib.bench", matches = "true")
class DrawBatch2DBenchTest {
    private static final int DRAWS = 16000;
    private static final int WARM_UP_PASSES = 40;
    private static final int PASSES = 25;

    private static final class Part implements IBatchMaterial {
        @Override
        public BatchFormat format() {
            return BatchFormats.SPRITE;
        }

        @Override
        public BatchFormat instancedFormat() {
            return null;
        }

        @Override
        public void apply(boolean linear, Matrix4f projection, Matrix4f view) {
        }

        @Override
        public boolean hasInstanced() {
            return false;
        }

        @Override
        public void applyInstanced(boolean linear, Matrix4f projection, Matrix4f view) {
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Part;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static final class NothingUploader implements IBatchUploader {
        @Override
        public void upload(ByteBuffer vertices, IntBuffer indices, ByteBuffer instances) {
        }

        @Override
        public void draw(BatchFormat format, int firstIndex, int indexCount) {
        }

        @Override
        public void drawInstances(BatchFormat format, int firstByte, int count) {
        }
    }

    private static final class Target implements IBatchTarget {
        final BatchStateTable table = new BatchStateTable();
        final IBatchUploader uploader = new NothingUploader();

        @Override
        public void begin(DrawBatch2D batch) {
        }

        @Override
        public void ended(DrawBatch2D batch) {
        }

        @Override
        public IBatchUploader uploader() {
            return uploader;
        }

        @Override
        public BatchStateTable table() {
            return table;
        }
    }

    private interface Pass {
        // The draws of one pass; returns nothing, the batch is ended by the caller
        void run(int draws);
    }

    private final Target target = new Target();
    private final DrawBatch2D batch = new DrawBatch2D(new IBatchBackend() {
        @Override
        public IBatchTarget target() {
            return target;
        }

        @Override
        public EBatch2DMode mode() {
            return EBatch2DMode.ORPHAN;
        }
    });
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final ViewCullBounds bounds = new ViewCullBounds();

    // Rectangles of 0.1 by 0.1 world unit: inside the view (-1..1) or far outside it
    private final Matrix4f[] models = new Matrix4f[DRAWS];
    private final Matrix4f[] farModels = new Matrix4f[DRAWS];
    private final float[] positions = {-0.05f, 0.05f, -0.05f, -0.05f, 0.05f, -0.05f, 0.05f, 0.05f};
    private final float[] textures = {0, 0, 0, 1, 1, 1, 1, 0};
    private final int[] indices = {0, 1, 2, 0, 2, 3};

    DrawBatch2DBenchTest() {
        for (int i = 0; i < DRAWS; ++i) {
            float x = (i % 160) / 160f * 1.8f - 0.9f, y = (i / 160) / 100f * 1.8f - 0.9f;
            models[i] = new Matrix4f().translate(x, y, 0).rotateZ(i * 0.01f);
            farModels[i] = new Matrix4f().translate(x + 10, y + 10, 0).rotateZ(i * 0.01f);
        }
    }

    private int state() {
        BatchStateTable table = batch.stateTable();
        return table.state(table.materialPart(new Part()), table.camera(projection, view), false);
    }

    private double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    // Times the draws of each pass (the batch is ended after, not timed), then the end of the view
    private void bench(String name, Pass pass) {
        double[] drawNanos = new double[PASSES];
        double[] sendNanos = new double[PASSES];
        for (int i = -WARM_UP_PASSES; i < PASSES; ++i) {
            long start = System.nanoTime();
            pass.run(DRAWS);
            long drawn = System.nanoTime();
            batch.endView();
            long sent = System.nanoTime();
            if (i >= 0) {
                drawNanos[i] = (drawn - start) / (double) DRAWS;
                sendNanos[i] = (sent - drawn) / (double) DRAWS;
            }
        }

        System.out.printf(java.util.Locale.ROOT, "{\"bench\":\"batch\",\"case\":\"%s\",\"n\":%d,\"nsPerDraw\":%.1f,"
                + "\"sendNsPerDraw\":%.1f}%n", name, DRAWS, median(drawNanos), median(sendNanos));
    }

    private void write(int state, Matrix4f[] from, int count) {
        for (int i = 0; i < count; ++i)
            BatchGeometry2D.WriteTriangles(batch, state, 0, 0f, from[i], positions, textures, 4, 0xFFFFFFFF, indices, 6);
    }

    private void cull(int state, Matrix4f[] from, int count) {
        // Starts the batch for cullBounds (a renderer starts it with its state)
        batch.isCulled(state, bounds.clear(null, DrawBatch2DBenchTest.class).world(0, 0, 1, 1));
        for (int i = 0; i < count; ++i) {
            ViewCullBounds box = batch.cullBounds(state, from[i], DrawBatch2DBenchTest.class)
                    .local(-0.05f, -0.05f, 0.05f, 0.05f);
            if (batch.isCulled(state, box))
                continue;
            BatchGeometry2D.WriteTriangles(batch, state, 0, 0f, from[i], positions, textures, 4, 0xFFFFFFFF, indices, 6);
        }
    }

    @Test
    void benchDraws() {
        // Write only: the rectangle of a renderer of the engine (4 vertices of a model, 6 indices)
        bench("write visible rectangle", count -> write(state(), models, count));
        // The test of the culling and the write of those visible (every draw of a pass is visible)
        bench("cull + write visible rectangle", count -> cull(state(), models, count));
        // Every draw culled: the test only
        bench("cull hidden rectangle", count -> cull(state(), farModels, count));
    }
}
