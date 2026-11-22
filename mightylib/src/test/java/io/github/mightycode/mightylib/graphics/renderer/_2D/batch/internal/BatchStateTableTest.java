package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormats;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The states of the batched draws: material parts interned by content, camera slots, reset of the ids
class BatchStateTableTest {
    private static final class Part implements IBatchMaterial {
        private final int content;
        private final BatchFormat format;

        Part(int content) {
            this(content, BatchFormats.SPRITE);
        }

        Part(int content, BatchFormat format) {
            this.content = content;
            this.format = format;
        }

        @Override
        public BatchFormat format() {
            return format;
        }

        @Override
        public void apply(boolean linear, Matrix4f projection, Matrix4f view) {}

        @Override
        public boolean equals(Object other) {
            return other instanceof Part && ((Part) other).content == content && ((Part) other).format == format;
        }

        @Override
        public int hashCode() {
            return content;
        }
    }

    // A material changed (another content) is another part; back to the first content, the first id again
    @Test
    void partsAreInternedByContent() {
        BatchStateTable table = new BatchStateTable();
        int first = table.materialPart(new Part(1));
        assertEquals(first, table.materialPart(new Part(1)));
        int second = table.materialPart(new Part(2));
        assertNotEquals(first, second);
        assertEquals(first, table.materialPart(new Part(1)));
        assertEquals(2, table.materialParts());
        assertEquals(new Part(2), table.materialPartById(second));
    }

    // The format is a part of the state: the same content in another format is another state, its format read back
    @Test
    void anotherFormatIsAnotherState() {
        BatchStateTable table = new BatchStateTable();
        int camera = table.camera(new Matrix4f(), new Matrix4f());
        int sprite = table.state(table.materialPart(new Part(1)), camera, false);
        int instanced = table.state(table.materialPart(new Part(1, BatchFormats.SPRITE_INSTANCED)), camera, false);

        assertNotEquals(sprite, instanced);
        assertSame(BatchFormats.SPRITE, table.formatOf(sprite));
        assertSame(BatchFormats.SPRITE_INSTANCED, table.formatOf(instanced));
    }

    @Test
    void theStatePacksItsParts() {
        BatchStateTable table = new BatchStateTable();
        int part = table.materialPart(new Part(7));
        int camera = table.camera(new Matrix4f(), new Matrix4f());
        int state = table.state(part, camera, true);

        assertTrue(state >= 0);
        assertEquals(part, BatchStateTable.materialPartOf(state));
        assertEquals(camera, BatchStateTable.cameraOf(state));
        assertTrue(BatchStateTable.isLinear(state));
        assertFalse(BatchStateTable.isLinear(table.state(part, camera, false)));
        assertTrue(table.isCurrent(state));
        assertFalse(table.isCurrent(table.state(part + 1, camera, true)));
    }

    // The highest ids still give a non-negative state
    @Test
    void theHighestIdsStayPositive() {
        BatchStateTable table = new BatchStateTable();
        int state = table.state(BatchStateTable.MAX_MATERIAL_PARTS - 1, BatchStateTable.MAX_CAMERAS - 1, true);
        assertTrue(state >= 0);
        assertEquals(BatchStateTable.MAX_MATERIAL_PARTS - 1, BatchStateTable.materialPartOf(state));
        assertEquals(BatchStateTable.MAX_CAMERAS - 1, BatchStateTable.cameraOf(state));
    }

    // The slot of a camera: its matrices are the same objects, their content is copied
    @Test
    void aCameraIsASlotOfItsMatrices() {
        BatchStateTable table = new BatchStateTable();
        Matrix4f projection = new Matrix4f().ortho2D(0, 100, 100, 0);
        Matrix4f view = new Matrix4f();
        int slot = table.camera(projection, view);
        assertEquals(slot, table.camera(projection, view));
        assertEquals(projection, table.projectionOf(slot));
        assertNotSame(projection, table.projectionOf(slot));

        // Same content, other objects: another camera
        assertNotEquals(slot, table.camera(new Matrix4f(projection), view));
        assertEquals(slot, table.camera(projection, view));
    }

    // Moved while no draw of it waits: the same slot, updated (the ids kept by the materials stay valid)
    @Test
    void aCameraMovedBetweenFlushesKeepsItsSlot() {
        BatchStateTable table = new BatchStateTable();
        Matrix4f projection = new Matrix4f();
        Matrix4f view = new Matrix4f();

        table.batchStarted();
        int slot = table.camera(projection, view);
        table.batchEnded();

        view.translate(10, 0, 0);
        table.batchStarted();
        assertEquals(slot, table.camera(projection, view));
        assertEquals(view, table.viewOf(slot));
        table.batchEnded();
        assertEquals(1, table.cameras());
    }

    // Moved while draws of it wait: a new slot, the waiting draws keep the previous camera
    @Test
    void aCameraMovedWhileItsDrawsWaitGetsANewSlot() {
        BatchStateTable table = new BatchStateTable();
        Matrix4f projection = new Matrix4f();
        Matrix4f view = new Matrix4f();

        table.batchStarted();
        int before = table.camera(projection, view);
        view.translate(10, 0, 0);
        int after = table.camera(projection, view);

        assertNotEquals(before, after);
        assertEquals(new Matrix4f(), table.viewOf(before));
        assertEquals(view, table.viewOf(after));
        // The new slot is the one of these matrices now
        assertEquals(after, table.camera(projection, view));
        table.batchEnded();
    }

    // Another batch waiting (another window) keeps the ids: the content of a slot it used is not changed
    @Test
    void anyWaitingBatchKeepsTheSlots() {
        BatchStateTable table = new BatchStateTable();
        Matrix4f projection = new Matrix4f();
        Matrix4f view = new Matrix4f();

        table.batchStarted();
        int slot = table.camera(projection, view);
        table.batchStarted();
        table.batchEnded();
        view.translate(1, 2, 3);
        assertNotEquals(slot, table.camera(projection, view));
        table.batchEnded();
    }

    // Emptied only when no batch waits, once above its thresholds: the generation tells the caches
    @Test
    void theTableIsResetOnlyWhenNoBatchWaits() {
        BatchStateTable table = new BatchStateTable(2, 100);
        table.batchStarted();
        int state = table.state(table.materialPart(new Part(1)), table.camera(new Matrix4f(), new Matrix4f()), false);
        table.materialPart(new Part(2));
        table.materialPart(new Part(3));
        table.batchStarted();
        table.batchEnded();
        // A batch still waits: kept
        assertEquals(0, table.generation());
        assertTrue(table.isCurrent(state));

        table.batchEnded();
        assertEquals(1, table.generation());
        assertEquals(1, table.resets());
        assertEquals(0, table.materialParts());
        assertEquals(0, table.cameras());
        assertFalse(table.isCurrent(state));

        // Below the thresholds: kept
        table.batchStarted();
        table.materialPart(new Part(1));
        table.batchEnded();
        assertEquals(1, table.generation());
        assertEquals(1, table.materialParts());
    }

    // Many cameras: only they are emptied, the material parts keep their ids, the copies of the matrices are reused
    @Test
    void theCamerasAloneAreEmptiedWithoutTheParts() {
        BatchStateTable table = new BatchStateTable(100, 2);
        table.batchStarted();
        int part = table.materialPart(new Part(1));
        int first = table.camera(new Matrix4f(), new Matrix4f());
        Matrix4f copy = table.projectionOf(first);
        int state = table.state(part, first, false);
        table.camera(new Matrix4f(), new Matrix4f());
        table.camera(new Matrix4f(), new Matrix4f());
        table.batchEnded();

        assertEquals(0, table.cameras());
        assertEquals(1, table.materialParts());
        assertEquals(0, table.partsGeneration());
        assertEquals(1, table.generation());
        assertFalse(table.isCurrent(state));

        table.batchStarted();
        assertEquals(part, table.materialPart(new Part(1)));
        Matrix4f projection = new Matrix4f().ortho2D(0, 10, 10, 0);
        int slot = table.camera(projection, new Matrix4f());
        assertSame(copy, table.projectionOf(slot));
        assertEquals(projection, table.projectionOf(slot));
        table.batchEnded();
    }

    // As many parts drawn each frame as the threshold: it doubles instead of building them all again each frame
    @Test
    void partsDrawnEachFrameRaiseTheThreshold() {
        BatchStateTable table = new BatchStateTable(4, 100);
        Matrix4f projection = new Matrix4f();
        Matrix4f view = new Matrix4f();

        table.frameStarted();
        table.batchStarted();
        for (int i = 0; i < 6; ++i)
            table.state(table.materialPart(new Part(i)), table.camera(projection, view), false);
        table.batchEnded();
        assertEquals(6, table.materialParts());
        assertEquals(8, table.materialPartsThreshold());
        assertEquals(0, table.partsGeneration());

        // Next frame: one part drawn, three new ones not drawn: the others are no longer used, emptied
        table.frameStarted();
        table.batchStarted();
        table.state(table.materialPart(new Part(0)), table.camera(projection, view), false);
        for (int i = 10; i < 13; ++i)
            table.materialPart(new Part(i));
        table.batchEnded();
        assertEquals(0, table.materialParts());
        assertEquals(1, table.partsGeneration());
        assertEquals(8, table.materialPartsThreshold());
    }

    // Every camera slot taken: no room for a state, emptied at the end of the batch
    @Test
    void aFullTableHasNoRoom() {
        BatchStateTable table = new BatchStateTable();
        table.batchStarted();
        Matrix4f projection = new Matrix4f();
        for (int i = 0; i < BatchStateTable.MAX_CAMERAS; ++i)
            table.camera(projection, new Matrix4f());
        assertFalse(table.hasRoom());
        assertThrows(IllegalStateException.class, () -> table.camera(projection, new Matrix4f()));

        table.batchEnded();
        assertTrue(table.hasRoom());
        assertEquals(0, table.cameras());
    }

    @Test
    void anEndWithoutStartFails() {
        assertThrows(IllegalStateException.class, () -> new BatchStateTable().batchEnded());
    }
}
