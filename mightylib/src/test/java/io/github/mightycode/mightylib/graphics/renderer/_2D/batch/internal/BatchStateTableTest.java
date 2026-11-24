package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormats;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
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

    // A tracked camera (its values marked at each change) moved while its draws wait: a new slot, seen by the version
    @Test
    void aTrackedCameraMovedWhileItsDrawsWaitGetsANewSlot() {
        BatchStateTable table = new BatchStateTable();
        Matrix4f view = new Matrix4f();
        ShaderValue projectionValue = new ShaderValue("projection", Matrix4f.class, new Matrix4f()).trackChanges();
        ShaderValue viewValue = new ShaderValue("view", Matrix4f.class, view).trackChanges();

        table.batchStarted();
        int before = table.camera(projectionValue, viewValue);
        assertEquals(before, table.camera(projectionValue, viewValue));
        view.translate(10, 0, 0);
        viewValue.markChanged();
        int after = table.camera(projectionValue, viewValue);

        assertNotEquals(before, after);
        assertEquals(new Matrix4f(), table.viewOf(before));
        assertEquals(view, table.viewOf(after));
        assertEquals(after, table.camera(projectionValue, viewValue));
        table.batchEnded();

        // Moved between flushes: the same slot, updated in place
        view.translate(5, 0, 0);
        viewValue.markChanged();
        table.batchStarted();
        assertEquals(after, table.camera(projectionValue, viewValue));
        assertEquals(view, table.viewOf(after));
        table.batchEnded();
    }

    // A tracked value is known by its version: its content changed in place without markChanged is not seen (the
    // promise of its owner); an untracked one is compared by its content: changed in place, it is seen
    @Test
    void anUntrackedCameraIsComparedByItsContent() {
        BatchStateTable table = new BatchStateTable();
        Matrix4f trackedView = new Matrix4f();
        ShaderValue projection = new ShaderValue("projection", Matrix4f.class, new Matrix4f()).trackChanges();
        ShaderValue tracked = new ShaderValue("view", Matrix4f.class, trackedView).trackChanges();
        Matrix4f rawView = new Matrix4f();
        ShaderValue raw = new ShaderValue("view", Matrix4f.class, rawView);

        table.batchStarted();
        int trackedSlot = table.camera(projection, tracked);
        int rawSlot = table.camera(projection, raw);
        assertNotEquals(trackedSlot, rawSlot);

        trackedView.translate(3, 0, 0);
        assertEquals(trackedSlot, table.camera(projection, tracked));
        assertEquals(new Matrix4f(), table.viewOf(trackedSlot));

        rawView.translate(3, 0, 0);
        int moved = table.camera(projection, raw);
        assertNotEquals(rawSlot, moved);
        assertEquals(rawView, table.viewOf(moved));

        // Given by its matrices only: compared by content, even for a tracked camera
        assertNotEquals(trackedSlot, table.camera((Matrix4f) projection.getObject(), trackedView));
        table.batchEnded();
    }

    // Another matrix put in a tracked value (setObject): the slot of the new matrix
    @Test
    void anotherMatrixInATrackedValueIsAnotherCamera() {
        BatchStateTable table = new BatchStateTable();
        ShaderValue projection = new ShaderValue("projection", Matrix4f.class, new Matrix4f()).trackChanges();
        ShaderValue view = new ShaderValue("view", Matrix4f.class, new Matrix4f()).trackChanges();
        int first = table.camera(projection, view);
        Matrix4f other = new Matrix4f().translate(1, 2, 0);
        view.setObject(other);
        int second = table.camera(projection, view);
        assertNotEquals(first, second);
        assertEquals(other, table.viewOf(second));
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

    // The box of the view of a slot follows its content: updated in place, new slot, slot reused after a reset
    @Test
    void theViewBoxFollowsTheSlot() {
        BatchStateTable table = new BatchStateTable(100, 1);
        Matrix4f projection = new Matrix4f().setOrtho(0, 800, 600, 0, -10, 10);
        Matrix4f view = new Matrix4f();

        table.batchStarted();
        int slot = table.camera(projection, view);
        assertViewBox(table, slot, 0, 0, 800, 600);
        table.batchEnded();

        // Moved between flushes: the same slot, its box updated
        view.translation(-100, -50, 0);
        table.batchStarted();
        assertEquals(slot, table.camera(projection, view));
        assertViewBox(table, slot, 100, 50, 900, 650);

        // Moved while its draws wait: the new slot has the new box, the retired one keeps its own
        view.translation(-200, 0, 0).scaleLocal(2, 2, 1);
        int moved = table.camera(projection, view);
        assertNotEquals(slot, moved);
        assertViewBox(table, slot, 100, 50, 900, 650);
        assertViewBox(table, moved, 200, 0, 600, 300);
        table.batchEnded();

        // Above the threshold of the cameras: emptied, the slot reused by another camera gets its box
        assertEquals(0, table.cameras());
        table.batchStarted();
        int reused = table.camera(new Matrix4f().setOrtho(-10, 10, -5, 5, -1, 1), new Matrix4f());
        assertEquals(slot, reused);
        assertViewBox(table, reused, -10, -5, 10, 5);
        // A perspective has no box
        int perspective = table.camera(new Matrix4f().setPerspective(1f, 1f, 0.1f, 10f), new Matrix4f());
        assertFalse(table.hasViewBox(perspective));
        // A flat scale neither
        int flat = table.camera(new Matrix4f().scaling(0, 1, 1), new Matrix4f());
        assertFalse(table.hasViewBox(flat));
        table.batchEnded();
    }

    // Within the slack kept against the rounding of floats
    private static void assertViewBox(BatchStateTable table, int slot, float minX, float minY, float maxX,
                                      float maxY) {
        assertTrue(table.hasViewBox(slot));
        assertEquals(minX, table.viewMinX(slot), 0.02f);
        assertEquals(minY, table.viewMinY(slot), 0.02f);
        assertEquals(maxX, table.viewMaxX(slot), 0.02f);
        assertEquals(maxY, table.viewMaxY(slot), 0.02f);
        assertTrue(table.viewMinX(slot) <= minX && table.viewMaxX(slot) >= maxX);
    }

    @Test
    void anEndWithoutStartFails() {
        assertThrows(IllegalStateException.class, () -> new BatchStateTable().batchEnded());
    }
}
