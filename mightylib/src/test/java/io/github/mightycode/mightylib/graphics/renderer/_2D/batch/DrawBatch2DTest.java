package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchGeometry2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchStateTable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchBackend;
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
        private final BatchFormat format;

        FakePart(String name, List<String> log, BatchFormat format) {
            this.name = name;
            this.log = log;
            this.format = format;
        }

        @Override
        public BatchFormat format() {
            return format;
        }

        @Override
        public BatchFormat instancedFormat() {
            return hasInstanced() ? BatchFormats.SPRITE_INSTANCED : null;
        }

        @Override
        public void apply(boolean linear, Matrix4f projection, Matrix4f view) {
            log.add("apply " + name);
        }

        // A name starting with "i" has an instanced program
        @Override
        public boolean hasInstanced() {
            return name.startsWith("i");
        }

        @Override
        public void applyInstanced(boolean linear, Matrix4f projection, Matrix4f view) {
            log.add("applyInstanced " + name);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof FakePart && ((FakePart) other).name.equals(name)
                    && ((FakePart) other).format == format;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    // Keeps what was uploaded and logs the draw calls (the vertices read as BatchFormats.SPRITE)
    private static final class FakeUploader implements IBatchUploader {
        private final List<String> log;
        final List<float[]> vertexUploads = new ArrayList<>();
        final List<int[]> colorUploads = new ArrayList<>();
        final List<int[]> indexUploads = new ArrayList<>();
        // Instances read as BatchFormats.SPRITE_INSTANCED (12 floats), and their bytes
        final List<float[]> instanceUploads = new ArrayList<>();
        final List<ByteBuffer> instanceBytes = new ArrayList<>();
        final List<ByteBuffer> vertexBytes = new ArrayList<>();

        FakeUploader(List<String> log) {
            this.log = log;
        }

        @Override
        public void upload(ByteBuffer vertices, IntBuffer indices, ByteBuffer instances) {
            assertEquals(0, vertices.position());
            assertEquals(0, indices.position());
            assertEquals(0, instances.position());
            if (vertices.remaining() > 0 || indices.remaining() > 0) {
                int vertexCount = vertices.remaining() / DrawBatch2D.VERTEX_BYTES;
                int indexCount = indices.remaining();
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
                vertexBytes.add(copy(vertices));
                log.add("upload " + vertexCount + " " + indexCount);
            }

            if (instances.remaining() > 0) {
                int count = instances.remaining() / DrawBatch2D.INSTANCE_BYTES;
                float[] floats = new float[count * 12];
                for (int i = 0; i < count; ++i) {
                    for (int j = 0; j < 12; ++j)
                        floats[i * 12 + j] = instances.getFloat(i * DrawBatch2D.INSTANCE_BYTES + j * 4);
                }
                instanceUploads.add(floats);
                instanceBytes.add(copy(instances));
                log.add("uploadInstances " + count);
            }
        }

        private static ByteBuffer copy(ByteBuffer buffer) {
            ByteBuffer copy = ByteBuffer.allocate(buffer.remaining()).order(buffer.order());
            copy.put(buffer.duplicate());
            copy.flip();
            return copy;
        }

        @Override
        public void draw(BatchFormat format, int firstIndex, int indexCount) {
            log.add("draw " + (format == BatchFormats.SPRITE ? "" : format.name() + " ") + firstIndex + " "
                    + indexCount);
        }

        // Sprite instances counted in instances, the others named by their format with their first byte
        int lastSpriteInstanceByte = -1;

        @Override
        public void drawInstances(BatchFormat format, int firstByte, int count) {
            if (format == BatchFormats.SPRITE_INSTANCED)
                lastSpriteInstanceByte = firstByte;
            if (format == BatchFormats.SPRITE_INSTANCED)
                log.add("instances " + firstByte / DrawBatch2D.INSTANCE_BYTES + " " + count);
            else
                log.add("instances " + format.name() + " " + firstByte + " " + count);
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

    // The mode of the backend of the tests (INSTANCED: sprite quads as instances)
    private EBatch2DMode mode = EBatch2DMode.ORPHAN;

    private IBatchBackend backend(IBatchTarget target) {
        return new IBatchBackend() {
            @Override
            public IBatchTarget target() {
                return target;
            }

            @Override
            public EBatch2DMode mode() {
                return mode;
            }
        };
    }

    private final List<String> log = new ArrayList<>();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private FakeTarget target;
    private DrawBatch2D batch;

    @BeforeEach
    void create() {
        target = new FakeTarget(log);
        batch = new DrawBatch2D(backend(target));
    }

    private int state(String name) {
        return state(name, BatchFormats.SPRITE);
    }

    private int state(String name, BatchFormat format) {
        BatchStateTable table = batch.stateTable();
        return table.state(table.materialPart(new FakePart(name, log, format)), table.camera(projection, view), false);
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

    // No room for one more state: the draws waiting are sent, the table emptied, the drawing goes on
    @Test
    void aFullTableSendsTheDrawsFirst() {
        quad(state("a"), 0);
        BatchStateTable table = target.table;
        while (table.hasRoom())
            table.camera(projection, new Matrix4f());

        int b = state("b");
        assertTrue(table.cameras() < 2);
        quad(b, 1);
        batch.endView();

        assertEquals(List.of("apply a", "draw 0 6", "apply b", "draw 0 6"), draws());
        assertArrayEquals(new int[]{1, 0}, batch.lastFrameBarrierCauses().get(DrawBatch2D.CAUSE_TABLE_FULL));
    }

    // A view is shown in the last frame once displayed in it: a view of another frame is an old one (command batch)
    @Test
    void aViewIsShownInTheFrameItWasDisplayedIn() {
        long[] frame = {1};
        DrawBatch2D counted = new DrawBatch2D(new IBatchBackend() {
            @Override
            public IBatchTarget target() {
                return target;
            }

            @Override
            public EBatch2DMode mode() {
                return EBatch2DMode.ORPHAN;
            }

            @Override
            public long frame() {
                return frame[0];
            }
        });

        assertFalse(counted.shownLastFrame());
        counted.endView();
        assertTrue(counted.shownLastFrame());
        assertTrue(counted.describe().getBoolean("shownLastFrame"));
        ++frame[0];
        assertFalse(counted.shownLastFrame());
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

    // A culled draw is not asked: nothing written, the run of its neighbors not cut, no index of a sorted segment
    @Test
    void aCulledDrawWritesNothingAndCutsNothing() {
        ViewCullBounds bounds = new ViewCullBounds();
        int a = state("a");
        quad(a, 0);
        // Identity camera: the view is -1..1
        assertTrue(batch.isCulled(a, bounds.clear(null, getClass()).world(5, 5, 6, 6)));
        quad(a, 0.5f);
        batch.endView();

        assertEquals(List.of("apply a", "draw 0 12"), draws());
        assertEquals(8, target.uploader.vertexUploads.get(0).length / 4);
        assertEquals(2, batch.lastFrameDraws());
        assertEquals(1, batch.lastFrameCulled());

        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        int b = state("b");
        a = state("a");
        quad(a, 0, 1, 0f);
        assertTrue(batch.isCulled(b, bounds.clear(null, getClass()).world(5, 5, 6, 6)));
        quad(a, 0.5f, 0, 0f);
        batch.endView();

        float[] vertices = target.uploader.vertexUploads.get(1);
        assertEquals(List.of(0.5f, 0f), quadOrder(vertices, target.uploader.indexUploads.get(1)));
        assertEquals(List.of("apply a", "draw 0 12"), draws().subList(2, 4));
        assertEquals(2, batch.lastFrameDraws());
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
        DrawBatch2D other = new DrawBatch2D(backend(target));
        int a = state("a");
        quad(a, 0);

        BatchStateTable table = other.stateTable();
        int b = table.state(table.materialPart(new FakePart("b", log, BatchFormats.SPRITE)), table.camera(projection, view), false);
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

    // Bytes: written by the draws (format size x vertices + 4 x indices), sent by the flushes, per draw call
    @Test
    void theBytesAreThoseOfTheFormatsTimesTheValues() {
        int a = state("a");
        for (int i = 0; i < 5; ++i)
            quad(a, i);
        ViewCullBounds bounds = new ViewCullBounds();
        // Culled (outside the view of the identity camera): writes nothing
        assertTrue(batch.isCulled(a, bounds.clear(null, getClass()).world(5, 5, 6, 6)));
        batch.endView();

        long quadBytes = 4L * DrawBatch2D.VERTEX_BYTES + 6 * Integer.BYTES;
        assertEquals(5 * quadBytes, batch.lastFrameBytes());
        assertEquals(Map.of("sprite", 5 * quadBytes), batch.lastFrameFormatBytes());
        assertEquals(5 * quadBytes, batch.lastFrameBytesPerCall());
        assertEquals(5.0, batch.lastFrameDrawsPerCall());
        assertEquals(5 * quadBytes, batch.describe().getLong("bytes"));
        assertEquals(1, batch.describe().getJSONObject("viewCull").getInt("culled"));

        // The next frame starts again
        quad(state("a"), 0);
        batch.endView();
        assertEquals(quadBytes, batch.lastFrameBytes());
    }

    // Mode INSTANCED: the quads sent as instances are bytes of instances, not of vertices
    @Test
    void quadsSentAsInstancesAreCountedAsInstances() {
        mode = EBatch2DMode.INSTANCED;
        int a = state("i");
        for (int i = 0; i < 3; ++i)
            affineQuad(a, i, 0);
        batch.endView();

        assertEquals(3L * DrawBatch2D.INSTANCE_BYTES, batch.lastFrameBytes());
        // What the draws wrote is still vertices and indices
        assertEquals(3L * (4L * DrawBatch2D.VERTEX_BYTES + 6 * Integer.BYTES),
                batch.lastFrameFormatBytes().get("sprite"));
    }

    // The draws without box are counted by the class that asked, the most often first
    @Test
    void theDrawsWithoutBoxAreCountedByClass() {
        int a = state("a");
        ViewCullBounds bounds = new ViewCullBounds();
        for (int i = 0; i < 3; ++i)
            batch.isCulled(a, bounds.clear(null, String.class));
        for (int i = 0; i < 5; ++i)
            batch.isCulled(a, bounds.clear(null, Integer.class));
        batch.isCulled(a, null);
        batch.endView();

        assertEquals(9, batch.lastFrameCullNoBounds());
        List<Map.Entry<String, Integer>> classes = new ArrayList<>(batch.lastFrameNoBoundsClasses().entrySet());
        assertEquals("java.lang.Integer", classes.get(0).getKey());
        assertEquals(5, classes.get(0).getValue());
        assertEquals("java.lang.String", classes.get(1).getKey());
        assertEquals("?", classes.get(2).getKey());
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

    // ---- Mode INSTANCED ----

    // A unit quad at x with texture coordinates of a parallelogram (an affine shape, like a RectangleRenderer)
    private void affineQuad(int state, float x, int layer) {
        batch.quad(state, layer, 0f,
                x, 1, 0, 1,
                x, 0, 0, 0,
                x + 1, 0, 1, 0,
                x + 1, 1, 1, 1,
                -1);
    }

    private List<String> instancedLog() {
        List<String> lines = new ArrayList<>();
        for (String line : log)
            lines.add(line.startsWith("upload ") ? "upload vertices" : line);
        return lines;
    }

    // A quad turned and scaled: its instance is the affine map of the unit quad, positions and texture coordinates
    @Test
    void aQuadIsItsAffineInstance() {
        mode = EBatch2DMode.INSTANCED;
        int state = state("i");
        // Corners (0, 1), (0, 0), (1, 0), (1, 1) of the unit quad by x' = 2x - y + 10, y' = x + 3y + 20
        batch.quad(state, 0, 0f,
                9, 23, 0.1f, 0.9f,
                10, 20, 0.1f, 0.5f,
                12, 21, 0.4f, 0.5f,
                11, 24, 0.4f, 0.9f,
                0x11223344);
        batch.endView();

        assertEquals(List.of("uploadInstances 1", "applyInstanced i", "instances 0 1"), instancedLog());
        float[] instance = target.uploader.instanceUploads.get(0);
        // Rows of the transform (x axis, y axis, origin), texture origin, its change along x, then along y
        assertArrayEquals(new float[]{2, -1, 10, 1, 3, 20, 0.1f, 0.5f, 0.3f, 0f, 0f, 0.4f}, instance, 1e-6f);
        assertEquals(1, batch.lastFrameInstancedDrawCalls());
    }

    @Test
    void quadsOfOneStateAreOneInstancedCall() {
        mode = EBatch2DMode.INSTANCED;
        int state = state("i");
        for (int i = 0; i < 100; ++i)
            affineQuad(state, i, 0);
        batch.endView();

        assertEquals(List.of("uploadInstances 100", "applyInstanced i", "instances 0 100"), instancedLog());
    }

    // A triangle in the run, a quad that is not a parallelogram, two colors: the run is drawn by its vertices
    @Test
    void aRunOfOtherShapesIsDrawnByVertices() {
        mode = EBatch2DMode.INSTANCED;
        int state = state("i");
        affineQuad(state, 0, 0);
        batch.beginDraw(state);
        batch.vertex(0, 0, 0, 0, -1);
        batch.vertex(1, 0, 0, 0, -1);
        batch.vertex(0, 1, 0, 0, -1);
        batch.index(0);
        batch.index(1);
        batch.index(2);
        batch.endDraw();
        batch.endView();
        assertEquals(List.of("upload vertices", "apply i", "draw 0 9"), instancedLog());

        log.clear();
        int other = state("i");
        batch.quad(other, 0, 0f, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 5, 5, 0, 0, -1);
        batch.endView();
        assertEquals(List.of("upload vertices", "apply i", "draw 0 6"), instancedLog());

        log.clear();
        int colors = state("i");
        batch.beginDraw(colors);
        batch.vertex(0, 1, 0, 0, -1);
        batch.vertex(0, 0, 0, 0, 0x00FF00FF);
        batch.vertex(1, 0, 0, 0, -1);
        batch.vertex(1, 1, 0, 0, -1);
        for (int index : new int[]{0, 3, 2, 0, 2, 1})
            batch.index(index);
        batch.endDraw();
        batch.endView();
        assertEquals(List.of("upload vertices", "apply i", "draw 0 6"), instancedLog());
    }

    // One flush: the runs of quads by instances, the others by vertices, in the order of the runs
    @Test
    void instancedAndVertexRunsMix() {
        mode = EBatch2DMode.INSTANCED;
        int instanced = state("i");
        int plain = state("p");
        affineQuad(instanced, 0, 0);
        affineQuad(instanced, 1, 0);
        affineQuad(plain, 2, 0);
        affineQuad(instanced, 3, 0);
        batch.endView();

        assertEquals(List.of("upload vertices", "uploadInstances 3", "applyInstanced i", "instances 0 2",
                "apply p", "draw 12 6", "applyInstanced i", "instances 2 1"), instancedLog());
        assertEquals(2, batch.lastFrameInstancedDrawCalls());
    }

    // Sorted: the runs are built from the sorted indices, their quads become instances alike
    @Test
    void sortedQuadsAreInstancesInTheirOrder() {
        mode = EBatch2DMode.INSTANCED;
        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        int state = state("i");
        affineQuad(state, 0, 2);
        affineQuad(state, 1, 1);
        affineQuad(state, 2, 3);
        batch.endView();

        float[] instances = target.uploader.instanceUploads.get(0);
        // The origin x of each instance (vertex 1 of the quad): sorted by layer
        assertEquals(1f, instances[2]);
        assertEquals(0f, instances[12 + 2]);
        assertEquals(2f, instances[24 + 2]);
    }

    // ---- Formats ----

    // Per vertex: a position, an int, a color (16 bytes: not the size of a sprite vertex)
    private static final BatchFormat LINE = BatchFormat.builder("line")
            .vertex("aPos", 2, EBatchAttributeType.FLOAT)
            .vertex("aValue", 1, EBatchAttributeType.INT)
            .vertex("aColor", 4, EBatchAttributeType.NORMALIZED_UBYTE)
            .build();
    // Per instance: two floats and a color, drawn as a triangle of gl_VertexID
    private static final BatchFormat DOT = BatchFormat.builder("dot")
            .instance("aCenter", 2, EBatchAttributeType.FLOAT)
            .instance("aColor", 4, EBatchAttributeType.NORMALIZED_UBYTE)
            .template(null, new int[]{0, 1, 2})
            .build();

    private void lineDraw(int state, float x, int value) {
        batch.beginDraw(state);
        for (int i = 0; i < 3; ++i) {
            batch.put(x + i, 0);
            batch.putInt(value);
            batch.putColor(0x11223344);
        }
        batch.index(0);
        batch.index(1);
        batch.index(2);
        batch.endDraw();
    }

    private void dots(int state, float x, int count) {
        batch.beginDraw(state);
        for (int i = 0; i < count; ++i) {
            batch.put(x + i, 1);
            batch.putColor(0x11223344);
        }
        batch.endDraw();
    }

    // The draw calls, instanced ones included
    private List<String> calls() {
        List<String> calls = new ArrayList<>();
        for (String line : log) {
            if (line.startsWith("instances") || line.startsWith("apply") || line.startsWith("draw"))
                calls.add(line);
        }

        return calls;
    }

    // The values land at the offsets of the format; the vertices of a draw start on a whole vertex of their format
    @Test
    void valuesAreWrittenInTheFormat() {
        int sprite = state("s");
        int line = state("l", LINE);
        quad(sprite, 0);
        lineDraw(line, 5, 42);
        batch.endView();

        ByteBuffer vertices = target.uploader.vertexBytes.get(0);
        // 4 sprite vertices (80 bytes) then the line vertices from byte 80 (vertex 5 of 16 bytes)
        assertEquals(80 + 3 * 16, vertices.remaining());
        assertEquals(5f, vertices.getFloat(80));
        assertEquals(42, vertices.getInt(80 + 8));
        assertEquals(0x11, vertices.get(80 + 12) & 0xFF);
        assertEquals(7f, vertices.getFloat(80 + 2 * 16));
        assertArrayEquals(new int[]{0, 3, 2, 0, 2, 1, 5, 6, 7}, target.uploader.indexUploads.get(0));
        assertEquals(List.of("apply s", "draw 0 6", "apply l", "draw line 6 3"), draws());
    }

    // A sprite vertex after a line one: padded to a whole sprite vertex
    @Test
    void theVerticesOfAnotherFormatArePadded() {
        int sprite = state("s");
        int line = state("l", LINE);
        lineDraw(line, 0, 1);
        quad(sprite, 0);
        batch.endView();

        // 48 bytes of lines, the sprite starts at 60 (vertex 3 of 20 bytes)
        assertEquals(60 + 80, target.uploader.vertexBytes.get(0).remaining());
        assertArrayEquals(new int[]{0, 1, 2, 3, 6, 5, 3, 5, 4}, target.uploader.indexUploads.get(0));
    }

    // The padding of a larger vertex goes past the end of the full buffer: it grows, the vertices before are kept
    @Test
    void thePaddingPastTheEndOfTheBufferGrowsIt() {
        int sprite = state("s");
        int stroke = state("t", BatchFormats.STROKE);
        // 252 sprite vertices: 5040 bytes of 5120, padded to 5168 for a vertex of 136 bytes
        for (int q = 0; q < 63; ++q)
            quad(sprite, q);
        batch.beginDraw(stroke);
        for (int v = 0; v < 3; ++v) {
            for (int f = 0; f < 20; ++f)
                batch.put(v);
            batch.putInt(v);
            batch.putInt(v);
            for (int f = 0; f < 12; ++f)
                batch.put(v);
        }
        batch.index(0);
        batch.index(1);
        batch.index(2);
        batch.endDraw();
        batch.endView();

        ByteBuffer vertices = target.uploader.vertexBytes.get(0);
        assertEquals(5168 + 3 * 136, vertices.remaining());
        // The last sprite quad kept, the stroke vertices from vertex 38 of 136 bytes
        assertEquals(62f, vertices.getFloat(62 * 80));
        assertEquals(2f, vertices.getFloat(5168 + 2 * 136));
        int[] indices = target.uploader.indexUploads.get(0);
        assertArrayEquals(new int[]{38, 39, 40}, Arrays.copyOfRange(indices, indices.length - 3, indices.length));
    }

    // A value of the wrong kind, an index in the middle of a vertex, an end in the middle: refused
    @Test
    void writesAreCheckedAgainstTheFormat() {
        int line = state("l", LINE);
        batch.beginDraw(line);
        batch.put(1, 2);
        assertThrows(IllegalStateException.class, () -> batch.put(3));
        assertThrows(IllegalStateException.class, () -> batch.index(0));
        assertThrows(IllegalStateException.class, batch::endDraw);
        // The draw is dropped: the batch goes on
        assertEquals(0, batch.pendingDraws());
        lineDraw(line, 0, 1);
        batch.endView();
        assertEquals(List.of("apply l", "draw line 0 3"), draws());

        int dot = state("d", DOT);
        batch.beginDraw(dot);
        batch.put(0, 0);
        batch.putColor(-1);
        assertThrows(IllegalStateException.class, () -> batch.index(0));
        batch.endDraw();
    }

    // Draws of a format with instances: neighbors of one state are one instanced call, no vertex sent
    @Test
    void instancesOfOneStateAreOneCall() {
        int dot = state("d", DOT);
        dots(dot, 0, 2);
        dots(dot, 10, 3);
        batch.endView();

        assertEquals(List.of("apply d", "instances dot 0 5"), calls());
        ByteBuffer instances = target.uploader.instanceBytes.get(0);
        assertEquals(5 * 12, instances.remaining());
        assertEquals(10f, instances.getFloat(2 * 12));
        assertEquals(1, batch.lastFrameInstancedDrawCalls());
        assertTrue(target.uploader.vertexUploads.isEmpty());
    }

    // A change of format is a change of state: the run is cut, each run drawn in its format
    @Test
    void anotherFormatCutsTheRun() {
        int sprite = state("s");
        int dot = state("d", DOT);
        quad(sprite, 0);
        dots(dot, 0, 1);
        quad(sprite, 1);
        batch.endView();

        assertEquals(List.of("apply s", "draw 0 6", "apply d", "instances dot 0 1", "apply s", "draw 6 6"), calls());
        assertEquals(Map.of("sprite", 2, "dot", 1), batch.lastFrameFormatCalls());
    }

    // Sorted: the instances are gathered in the order of the keys
    @Test
    void sortedInstancesAreGatheredInTheirOrder() {
        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        int dot = state("d", DOT);
        batch.beginDraw(dot, 2, 0f);
        batch.put(2, 0);
        batch.putColor(-1);
        batch.endDraw();
        batch.beginDraw(dot, 1, 0f);
        batch.put(1, 0);
        batch.putColor(-1);
        batch.put(1.5f, 0);
        batch.putColor(-1);
        batch.endDraw();
        batch.endView();

        assertEquals(List.of("apply d", "instances dot 0 3"), calls());
        ByteBuffer instances = target.uploader.instanceBytes.get(0);
        assertEquals(1f, instances.getFloat(0));
        assertEquals(1.5f, instances.getFloat(12));
        assertEquals(2f, instances.getFloat(24));
    }

    // Instances, sprite quads made instances (mode INSTANCED) after them, sorted: nothing grows once stable
    @Test
    void instancesGrowNoMoreOnceTheFramesAreStable() {
        mode = EBatch2DMode.INSTANCED;
        batch.setDrawSortKey(DrawSortKeys.layerThenDepth());
        int[] capacities = null;
        for (int frame = 0; frame < 4; ++frame) {
            int dot = state("d", DOT);
            int sprite = state("i");
            for (int i = 0; i < 500; ++i) {
                dots(dot, i, 2);
                affineQuad(sprite, i, i % 3);
            }
            batch.endView();

            int[] now = {batch.instanceCapacity(), batch.sortedInstanceCapacity(), batch.vertexCapacity(),
                    batch.sortedIndexCapacity()};
            if (frame > 0)
                assertArrayEquals(capacities, now, "frame " + frame);
            capacities = now;
        }
    }

    // The sprite quads made instances come after the instances of the formats, in the same upload
    @Test
    void convertedQuadsFollowTheInstancesOfTheFormats() {
        mode = EBatch2DMode.INSTANCED;
        int dot = state("d", DOT);
        int sprite = state("i");
        dots(dot, 0, 2);
        affineQuad(sprite, 0, 0);
        batch.endView();

        assertEquals(List.of("apply d", "instances dot 0 2", "applyInstanced i"), calls().subList(0, 3));
        // The quad instance from byte 24: 24 / 52 is not a whole sprite instance, logged by the format below
        assertEquals(2 * 12 + DrawBatch2D.INSTANCE_BYTES, target.uploader.instanceBytes.get(0).remaining());
        assertEquals(24, target.uploader.lastSpriteInstanceByte);
    }

    // ---- Draws of triangles at once ----

    private static final float[] TRIANGLE_POSITIONS = {0, 0, 2, 0, 2, 1, 0, 1};
    private static final float[] TRIANGLE_TEXTURES = {0.1f, 0.2f, 0.3f, 0.2f, 0.3f, 0.4f, 0.1f, 0.4f};
    private static final float[] TRIANGLE_COLORS = {1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0.5f, 1, 1, 1, 1};
    private static final int[] TRIANGLE_INDICES = {0, 1, 2, 0, 2, 3};

    // The same draw value by value, like vertex and index
    private void trianglesByValues(int state, Matrix4f model, float[] textures, float[] colors, int color) {
        batch.beginDraw(state);
        for (int i = 0; i < 4; ++i) {
            float x = TRIANGLE_POSITIONS[2 * i], y = TRIANGLE_POSITIONS[2 * i + 1];
            batch.vertex(model.m00() * x + model.m10() * y + model.m30(), model.m01() * x + model.m11() * y + model.m31(),
                    textures == null ? 0 : textures[2 * i], textures == null ? 0 : textures[2 * i + 1],
                    colors == null ? color : BatchGeometry2D.PackRGBA(colors, 4 * i));
        }
        for (int index : TRIANGLE_INDICES)
            batch.index(index);
        batch.endDraw();
    }

    // Written at once: the bytes and indices of the draw written value by value, one run with its neighbors
    @Test
    void trianglesAtOnceAreTheDrawWrittenValueByValue() {
        int a = state("a");
        Matrix4f model = new Matrix4f().translate(3, 4, 0).rotateZ(0.3f).scale(2, 3, 1);
        quad(a, 0);
        trianglesByValues(a, model, TRIANGLE_TEXTURES, null, 0x11223344);
        trianglesByValues(a, model, null, TRIANGLE_COLORS, 0);
        batch.endView();
        quad(a, 0);
        batch.triangles(a, 0, 0f, model, TRIANGLE_POSITIONS, TRIANGLE_TEXTURES, null, 4, 0x11223344, TRIANGLE_INDICES,
                6);
        batch.triangles(a, 0, 0f, model, TRIANGLE_POSITIONS, null, TRIANGLE_COLORS, 4, 0, TRIANGLE_INDICES, 6);
        batch.endView();

        assertEquals(target.uploader.vertexBytes.get(0), target.uploader.vertexBytes.get(1));
        assertArrayEquals(target.uploader.indexUploads.get(0), target.uploader.indexUploads.get(1));
        assertEquals(List.of("apply a", "draw 0 18", "apply a", "draw 0 18"), draws());
        assertEquals(3, batch.lastFrameDraws());
    }

    // An index outside of the vertices: refused, nothing written, the batch goes on
    @Test
    void trianglesWithAnIndexOutsideAreRefused() {
        int a = state("a");
        Matrix4f model = new Matrix4f();
        quad(a, 0);
        assertThrows(IllegalArgumentException.class, () -> batch.triangles(a, 0, 0f, model, TRIANGLE_POSITIONS, null,
                null, 4, 0, new int[]{0, 1, 4}, 3));
        assertThrows(IllegalArgumentException.class, () -> batch.triangles(a, 0, 0f, model, TRIANGLE_POSITIONS, null,
                null, 4, 0, new int[]{0, -1, 2}, 3));
        // Arrays shorter than the counts
        assertThrows(IllegalArgumentException.class, () -> batch.triangles(a, 0, 0f, model, TRIANGLE_POSITIONS, null,
                null, 5, 0, TRIANGLE_INDICES, 6));
        assertEquals(1, batch.pendingDraws());
        quad(a, 1);
        batch.endView();

        assertEquals(8, target.uploader.vertexUploads.get(0).length / 4);
        assertEquals(12, target.uploader.indexUploads.get(0).length);
        assertEquals(2, batch.lastFrameDraws());
    }

    @Test
    void trianglesInTheMiddleOfADrawAreRefused() {
        int a = state("a");
        batch.beginDraw(a);
        assertThrows(IllegalStateException.class, () -> batch.triangles(a, 0, 0f, new Matrix4f(), TRIANGLE_POSITIONS,
                null, null, 4, 0, TRIANGLE_INDICES, 6));
        batch.endDraw();
    }

    // Once the buffers have grown, a draw of triangles allocates nothing
    @Test
    void trianglesAtOnceAllocateNothing() {
        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().getId();
        Matrix4f model = new Matrix4f().translate(1, 2, 0);
        for (int pass = 0; pass < 3; ++pass) {
            long before = threads.getThreadAllocatedBytes(thread);
            int a = state("a");
            for (int i = 0; i < 2000; ++i)
                batch.triangles(a, 0, 0f, model, TRIANGLE_POSITIONS, TRIANGLE_TEXTURES, TRIANGLE_COLORS, 4, 0,
                        TRIANGLE_INDICES, 6);
            long allocated = threads.getThreadAllocatedBytes(thread) - before;
            batch.endView();
            target.uploader.vertexUploads.clear();
            target.uploader.colorUploads.clear();
            target.uploader.indexUploads.clear();
            target.uploader.vertexBytes.clear();
            log.clear();
            // The first pass grows the buffers; the state asked each pass is a little
            if (pass > 0)
                assertTrue(allocated < 2_000, allocated + " bytes allocated by 2000 draws");
        }
    }
}
