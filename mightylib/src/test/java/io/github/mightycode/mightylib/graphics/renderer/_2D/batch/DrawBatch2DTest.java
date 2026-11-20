package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchStateTable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchMaterial;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchTarget;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchUploader;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// The core of the batch without OpenGL: merge, barriers, order, sort keys, buffers
class DrawBatch2DTest {
    // A material part named by its content; applying it is logged
    private static final class FakePart implements IBatchMaterial {
        private final String name;
        private final List<String> log;

        FakePart(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public void apply(boolean linear, Matrix4f projection, Matrix4f view) {
            log.add("apply " + name);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof FakePart && ((FakePart) other).name.equals(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    // Keeps what was uploaded and logs the draw calls
    private static final class FakeUploader implements IBatchUploader {
        private final List<String> log;
        final List<float[]> vertexUploads = new ArrayList<>();
        final List<int[]> colorUploads = new ArrayList<>();
        final List<int[]> indexUploads = new ArrayList<>();

        FakeUploader(List<String> log) {
            this.log = log;
        }

        @Override
        public void upload(ByteBuffer vertices, int vertexCount, IntBuffer indices, int indexCount) {
            assertEquals(vertexCount * DrawBatch2D.VERTEX_BYTES, vertices.remaining());
            assertEquals(indexCount, indices.remaining());
            float[] floats = new float[vertexCount * 4];
            int[] colors = new int[vertexCount];
            for (int i = 0; i < vertexCount; ++i) {
                for (int j = 0; j < 4; ++j)
                    floats[i * 4 + j] = vertices.getFloat(i * DrawBatch2D.VERTEX_BYTES + j * 4);
                int at = i * DrawBatch2D.VERTEX_BYTES + 16;
                // Bytes in memory: R, G, B, A
                colors[i] = (vertices.get(at) & 0xFF) << 24 | (vertices.get(at + 1) & 0xFF) << 16
                        | (vertices.get(at + 2) & 0xFF) << 8 | (vertices.get(at + 3) & 0xFF);
            }
            int[] read = new int[indexCount];
            for (int i = 0; i < indexCount; ++i)
                read[i] = indices.get(i);

            vertexUploads.add(floats);
            colorUploads.add(colors);
            indexUploads.add(read);
            log.add("upload " + vertexCount + " " + indexCount);
        }

        @Override
        public void draw(int firstIndex, int indexCount) {
            log.add("draw " + firstIndex + " " + indexCount);
        }
    }

    // One context: flushes the other batch with waiting draws, like the real target
    private static final class FakeTarget implements IBatchTarget {
        final BatchStateTable table = new BatchStateTable();
        final FakeUploader uploader;
        DrawBatch2D active;
        int begins;

        FakeTarget(List<String> log) {
            uploader = new FakeUploader(log);
        }

        @Override
        public void begin(DrawBatch2D batch) {
            ++begins;
            if (active != null && active != batch)
                active.barrier("other");
            active = batch;
        }

        @Override
        public void ended(DrawBatch2D batch) {
            if (active == batch)
                active = null;
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

    private final List<String> log = new ArrayList<>();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private FakeTarget target;
    private DrawBatch2D batch;

    @BeforeEach
    void create() {
        target = new FakeTarget(log);
        batch = new DrawBatch2D(() -> target);
    }

    private int state(String name) {
        BatchStateTable table = batch.stateTable();
        return table.state(table.materialPart(new FakePart(name, log)), table.camera(projection, view), false);
    }

    // A unit quad at x, its corners carrying their order in u
    private void quad(int state, float x) {
        quad(state, x, 0, 0f);
    }

    private void quad(int state, float x, int layer, float depth) {
        batch.quad(state, layer, depth,
                x, 1, 0, 0,
                x, 0, 1, 0,
                x + 1, 0, 2, 0,
                x + 1, 1, 3, 0,
                0x11223344);
    }

    private List<String> draws() {
        List<String> draws = new ArrayList<>();
        for (String line : log) {
            if (line.startsWith("draw") || line.startsWith("apply"))
                draws.add(line);
        }

        return draws;
    }

    // The x of the first vertex of each quad in the order of the uploaded indices
    private static List<Float> quadOrder(float[] vertices, int[] indices) {
        List<Float> order = new ArrayList<>();
        for (int i = 0; i < indices.length; i += 6)
            order.add(vertices[indices[i] * 4]);
        return order;
    }

    @Test
    void neighborsOfTheSameStateAreOneDrawCall() {
        int a = state("a");
        for (int i = 0; i < 5; ++i)
            quad(a, i);
        batch.endView();

        assertEquals(List.of("apply a", "draw 0 30"), draws());
        assertEquals(1, batch.lastFrameDrawCalls());
        assertEquals(5, batch.lastFrameDraws());
        assertEquals(0, batch.lastFrameBarriers());
    }

    @Test
    void aDifferentStateCutsTheRun() {
        int a = state("a");
        int b = state("b");
        quad(a, 0);
        quad(a, 1);
        quad(b, 2);
        quad(a, 3);
        batch.endView();

        assertEquals(List.of("apply a", "draw 0 12", "apply b", "draw 12 6", "apply a", "draw 18 6"), draws());
        assertEquals(3, batch.lastFrameDrawCalls());
    }

    // Two parts of equal content are one state: interned by content, compared as ints
    @Test
    void equalContentsMerge() {
        int first = state("a");
        int second = state("a");
        assertEquals(first, second);

        quad(first, 0);
        quad(second, 1);
        batch.endView();
        assertEquals(List.of("apply a", "draw 0 12"), draws());
    }

    @Test
    void theQuadIsTwoTrianglesLikeRectangularFace() {
        quad(state("a"), 0);
        batch.endView();

        assertArrayEquals(new int[]{0, 3, 2, 0, 2, 1}, target.uploader.indexUploads.get(0));
        assertArrayEquals(new int[]{0x11223344, 0x11223344, 0x11223344, 0x11223344},
                target.uploader.colorUploads.get(0));
        float[] vertices = target.uploader.vertexUploads.get(0);
        assertArrayEquals(new float[]{0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 2, 0, 1, 1, 3, 0}, vertices);
    }

    @Test
    void aBarrierSendsWhatWasAskedBefore() {
        int a = state("a");
        quad(a, 0);
        batch.barrier(String.class);
        quad(state("a"), 1);
        batch.endView();

        assertEquals(List.of("apply a", "draw 0 6", "apply a", "draw 0 6"), draws());
        assertEquals(2, log.stream().filter(line -> line.startsWith("upload")).count());
        assertEquals(1, batch.lastFrameBarriers());
        assertEquals(2, batch.lastFrameFlushes());
        assertArrayEquals(new int[]{1, 0}, batch.lastFrameBarrierCauses().get(String.class.getName()));
    }

    // A barrier with nothing waiting costs nothing and is not counted
    @Test
    void anEmptyBarrierIsNotCounted() {
        batch.barrier("nothing");
        batch.flush();
        batch.endView();

        assertEquals(0, batch.lastFrameBarriers());
        assertTrue(log.isEmpty());
    }

    @Test
    void aDrawWithoutIndexDrawsNothing() {
        int a = state("a");
        batch.beginDraw(a);
        batch.vertex(0, 0, 0, 0, -1);
        batch.endDraw();
        quad(a, 5);
        batch.endView();

        assertEquals(4, target.uploader.vertexUploads.get(0).length / 4);
        assertEquals(1, batch.lastFrameDraws());
    }

    @Test
    void indicesAreOfTheVerticesOfTheDraw() {
        int a = state("a");
        batch.beginDraw(a);
        batch.vertex(0, 0, 0, 0, -1);
        assertThrows(IllegalArgumentException.class, () -> batch.index(1));
        assertThrows(IllegalArgumentException.class, () -> batch.index(-1));
        batch.index(0);
        assertThrows(IllegalStateException.class, () -> batch.beginDraw(a));
        assertThrows(IllegalStateException.class, () -> batch.flush());
        batch.endDraw();
        assertThrows(IllegalStateException.class, () -> batch.vertex(0, 0, 0, 0, 0));
        assertThrows(IllegalStateException.class, () -> batch.endDraw());
    }

    // Without a key: the order of the calls, no key computed, nothing sorted, no draw kept
    @Test
    void withoutKeyTheOrderIsTheOrderOfTheCalls() {
        int a = state("a");
        int b = state("b");
        for (int i = 0; i < 6; ++i)
            quad(i % 3 == 0 ? b : a, i);
        batch.endView();

        float[] vertices = target.uploader.vertexUploads.get(0);
        assertEquals(List.of(0f, 1f, 2f, 3f, 4f, 5f), quadOrder(vertices, target.uploader.indexUploads.get(0)));
        assertEquals(0, batch.lastFrameSorts());
        assertEquals(0, batch.entryCapacity());
        assertEquals(0, batch.sortedIndexCapacity());
    }

    // Equal keys keep the order of the calls; the vertices stay where they were written, only the indices move
    @Test
    void theSortIsStableAndMovesOnlyTheIndices() {
        List<Integer> layersSeen = new ArrayList<>();
        batch.setDrawSortKey((layer, depth, state) -> {
            layersSeen.add(layer);
            return (long) layer << 40;
        });
        int a = state("a");
        int[] layers = {1, 0, 1, 0, 2, 0};
        for (int i = 0; i < layers.length; ++i)
            quad(a, i, layers[i], 0f);
        batch.endView();

        float[] vertices = target.uploader.vertexUploads.get(0);
        for (int i = 0; i < layers.length; ++i)
            assertEquals(i, vertices[i * 16], "vertices in the order of the calls");
        assertEquals(List.of(1f, 3f, 5f, 0f, 2f, 4f), quadOrder(vertices, target.uploader.indexUploads.get(0)));
        // One key per draw, computed when it is asked
        assertEquals(List.of(1, 0, 1, 0, 2, 0), layersSeen);
        // Same state everywhere: still one draw call
        assertEquals(List.of("apply a", "draw 0 36"), draws());
        assertEquals(1, batch.lastFrameSorts());
    }

    // The low bits are the index of the draw whatever the function returns there
    @Test
    void theLowBitsOfTheKeyAreIgnored() {
        batch.setDrawSortKey((layer, depth, state) -> layer == 0 ? DrawBatch2D.CALL_INDEX_MASK : 0);
        int a = state("a");
        quad(a, 0, 0, 0f);
        quad(a, 1, 1, 0f);
        batch.endView();

        float[] vertices = target.uploader.vertexUploads.get(0);
        assertEquals(List.of(0f, 1f), quadOrder(vertices, target.uploader.indexUploads.get(0)));
    }

    // A draw of a higher layer after a barrier is not moved over what the barrier sent
    @Test
    void theSortStopsAtABarrier() {
        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        int a = state("a");
        quad(a, 0, 5, 0f);
        quad(a, 1, 3, 0f);
        batch.barrier(Integer.class);
        quad(state("a"), 2, 0, 0f);
        batch.endView();

        assertEquals(List.of(1f, 0f), quadOrder(target.uploader.vertexUploads.get(0),
                target.uploader.indexUploads.get(0)));
        assertEquals(List.of(2f), quadOrder(target.uploader.vertexUploads.get(1),
                target.uploader.indexUploads.get(1)));
        assertEquals(1, batch.lastFrameBarriersWhileSorted());
        assertArrayEquals(new int[]{1, 1}, batch.lastFrameBarrierCauses().get(Integer.class.getName()));
    }

    @Test
    void negativeAndEqualDepthsAreOrdered() {
        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        int a = state("a");
        float[] depths = {0.5f, -1f, -0.0f, 0f, -1f, 2f, -1000f, Float.MIN_VALUE};
        for (int i = 0; i < depths.length; ++i)
            quad(a, i, 0, depths[i]);
        batch.endView();

        // -1000, -1 (first), -1 (second), -0 and 0 in the order of the calls, min value, 0.5, 2
        assertEquals(List.of(6f, 1f, 4f, 2f, 3f, 7f, 0f, 5f),
                quadOrder(target.uploader.vertexUploads.get(0), target.uploader.indexUploads.get(0)));
    }

    @Test
    void layersComeBeforeDepthsNegativeLayersFirst() {
        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        int a = state("a");
        quad(a, 0, 1, -5f);
        quad(a, 1, -1, 10f);
        quad(a, 2, 0, 3f);
        quad(a, 3, 1, -6f);
        quad(a, 4, Integer.MIN_VALUE, 0f);
        batch.endView();

        assertEquals(List.of(4f, 1f, 2f, 3f, 0f),
                quadOrder(target.uploader.vertexUploads.get(0), target.uploader.indexUploads.get(0)));
    }

    // layerThenState groups the states inside a layer: fewer draw calls
    @Test
    void layerThenStateGroupsTheStates() {
        batch.setDrawSortKey(DrawSortKeys.layerThenState());
        int a = state("a");
        int b = state("b");
        quad(a, 0, 0, 0f);
        quad(b, 1, 0, 0f);
        quad(a, 2, 0, 0f);
        quad(b, 3, 1, 0f);
        quad(a, 4, 1, 0f);
        quad(b, 5, 0, 0f);
        batch.endView();

        assertEquals(List.of("apply a", "draw 0 12", "apply b", "draw 12 12", "apply a", "draw 24 6",
                "apply b", "draw 30 6"), draws());
    }

    // Changing the order sends what was asked with the previous one
    @Test
    void settingTheKeyIsABarrier() {
        int a = state("a");
        quad(a, 0);
        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        quad(state("a"), 1);
        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        batch.endView();

        assertEquals(2, batch.lastFrameFlushes());
        assertNotNull(batch.lastFrameBarrierCauses().get(DrawBatch2D.CAUSE_SORT_KEY));
    }

    // The target is told once per start, the table knows when no batch waits anymore
    @Test
    void theBatchIsStartedUntilItsFlush() {
        assertFalse(batch.isStarted());
        int a = state("a");
        assertTrue(batch.isStarted());
        assertEquals(1, target.table.pendingBatches());
        quad(a, 0);
        quad(a, 1);
        assertEquals(1, target.begins);
        batch.endView();

        assertFalse(batch.isStarted());
        assertEquals(0, target.table.pendingBatches());
        assertNull(target.active);
    }

    // Two views of one context: the second one sends the draws of the first before its own
    @Test
    void anotherBatchOfTheContextFlushesTheFirst() {
        DrawBatch2D other = new DrawBatch2D(() -> target);
        int a = state("a");
        quad(a, 0);

        BatchStateTable table = other.stateTable();
        int b = table.state(table.materialPart(new FakePart("b", log)), table.camera(projection, view), false);
        other.quad(b, 0, 0f, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, -1);
        other.endView();
        batch.endView();

        assertEquals(List.of("apply a", "draw 0 6", "apply b", "draw 0 6"), draws());
        assertEquals(1, batch.lastFrameBarriers());
        assertEquals(0, target.table.pendingBatches());
    }

    // The counters of a frame are rolled by the end of the view
    @Test
    void theCountersAreThoseOfTheLastFrame() {
        int a = state("a");
        quad(a, 0);
        quad(a, 1);
        batch.endView();
        assertEquals(2, batch.lastFrameDraws());

        quad(state("a"), 0);
        assertEquals(2, batch.lastFrameDraws());
        batch.endView();
        assertEquals(1, batch.lastFrameDraws());

        batch.endView();
        assertEquals(0, batch.lastFrameDraws());
        assertEquals(0, batch.lastFrameDrawCalls());
    }

    // Stable frames: once the arrays fit the frame, nothing grows anymore
    @Test
    void nothingGrowsOnceTheFramesAreStable() {
        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        int[] capacities = null;
        for (int frame = 0; frame < 5; ++frame) {
            int a = state("a");
            int b = state("b");
            for (int i = 0; i < 1000; ++i)
                quad(i % 7 == 0 ? b : a, i, i % 3, i * 0.5f);
            batch.endView();

            int[] now = {batch.vertexCapacity(), batch.indexCapacity(), batch.entryCapacity(), batch.runCapacity(),
                    batch.sortedIndexCapacity()};
            if (frame > 0)
                assertArrayEquals(capacities, now, "frame " + frame);
            capacities = now;
        }

        assertTrue(capacities[0] >= 4000);
        assertTrue(capacities[2] >= 1000);
    }

    // A state kept across a reset of the table is refused instead of drawing another material
    @Test
    void aStateOfAnotherGenerationIsRefused() {
        int a = state("a");
        quad(a, 0);
        batch.endView();
        assertThrows(IllegalArgumentException.class, () -> quad(a + (1 << 27), 0));
        assertThrows(IllegalArgumentException.class, () -> quad(-1, 0));
        batch.endView();
    }

    // Many vertices: the buffers grow and keep what was written
    @Test
    void theBuffersGrowWithTheirContent() {
        int a = state("a");
        for (int i = 0; i < 3000; ++i)
            quad(a, i);
        batch.endView();

        float[] vertices = target.uploader.vertexUploads.get(0);
        int[] indices = target.uploader.indexUploads.get(0);
        assertEquals(12000, vertices.length / 4);
        assertEquals(18000, indices.length);
        for (int i = 0; i < 3000; ++i)
            assertEquals(i, vertices[i * 16]);
        assertEquals(11999, Arrays.stream(indices).max().orElse(-1));
        Map<String, int[]> causes = batch.lastFrameBarrierCauses();
        assertTrue(causes.isEmpty());
    }
}
