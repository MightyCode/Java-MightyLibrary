package io.github.mightycode.mightylib.graphics.renderer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The copy on the CPU of a mesh (Mesh.keepCpuData) is what its updates sent, read by its batched draws
class MeshCpuDataTest {
    @Test
    void aVboIsCopied() {
        MeshCpuData data = new MeshCpuData();
        float[] positions = {1, 2, 3, 4};
        data.addVbo(0, positions);
        positions[0] = 9;
        assertArrayEquals(new float[]{1, 2, 3, 4}, data.vbo(0));

        // The caller changes its array then sends it again: the copy follows, in place at the same size
        float[] kept = data.vbo(0);
        data.setVbo(0, positions);
        assertSame(kept, data.vbo(0));
        assertArrayEquals(new float[]{9, 2, 3, 4}, data.vbo(0));

        data.setVbo(0, new float[]{5, 6});
        assertArrayEquals(new float[]{5, 6}, data.vbo(0));
    }

    // Mesh.updateVbo only writes the copy and marks the vbo dirty; Mesh.bind sends the dirty ones then clears them
    @Test
    void anUpdateMarksTheVboDirtyUntilSent() {
        MeshCpuData data = new MeshCpuData();
        data.addVbo(2, new float[]{1, 2, 3, 4});
        assertFalse(data.hasDirty(), "a new vbo is sent with its creation");
        for (int vbo = 0; vbo < 4; ++vbo)
            assertEquals(-1, data.dirtyStart(vbo));

        data.setVbo(2, new float[]{5, 6, 7, 8});
        assertTrue(data.hasDirty());
        assertEquals(0, data.dirtyStart(2));
        assertEquals(4, data.dirtyEnd(2));
        assertEquals(-1, data.dirtyStart(0));

        data.clearDirty();
        assertFalse(data.hasDirty());
        assertEquals(-1, data.dirtyStart(2));
        assertArrayEquals(new float[]{5, 6, 7, 8}, data.vbo(2));
    }

    // Several updates before a draw: one upload of the union of their ranges, with the last values
    @Test
    void theRangesOfSeveralUpdatesAreJoined() {
        MeshCpuData data = new MeshCpuData();
        data.addVbo(0, new float[8]);
        data.addVbo(1, new float[8]);
        data.setSubVbo(0, new float[]{1, 2}, 5 * Float.BYTES);
        assertEquals(5, data.dirtyStart(0));
        assertEquals(7, data.dirtyEnd(0));

        data.setSubVbo(0, new float[]{3}, 2 * Float.BYTES);
        data.setSubVbo(0, new float[]{4}, 5 * Float.BYTES);
        assertEquals(2, data.dirtyStart(0));
        assertEquals(7, data.dirtyEnd(0));
        assertArrayEquals(new float[]{0, 0, 3, 0, 0, 4, 2, 0}, data.vbo(0));
        assertEquals(-1, data.dirtyStart(1), "the other vbo is untouched");

        // Past the end: only what is inside counts; nothing inside: not dirty
        data.setSubVbo(1, new float[]{1, 2, 3}, 6 * Float.BYTES);
        assertEquals(6, data.dirtyStart(1));
        assertEquals(8, data.dirtyEnd(1));
        data.clearDirty();
        data.setSubVbo(1, new float[]{1}, 8 * Float.BYTES);
        assertFalse(data.hasDirty());

        // A part of an unknown vbo is not kept: not dirty (Mesh sends it now)
        data.addVbo(2, null);
        data.setSubVbo(2, new float[]{1}, 0);
        assertFalse(data.hasDirty());

        // A whole vbo of another size: dirty over its whole new length (Mesh reallocates it)
        data.setVbo(0, new float[]{1, 2, 3});
        assertEquals(0, data.dirtyStart(0));
        assertEquals(3, data.dirtyEnd(0));
    }

    // Mesh.unloadVBO drops what was not sent
    @Test
    void clearingTheVbosDropsTheirDirtyRanges() {
        MeshCpuData data = new MeshCpuData();
        data.addVbo(0, new float[]{1, 2});
        data.setVbo(0, new float[]{3, 4});
        data.clearVbos();
        assertFalse(data.hasDirty());

        data.addVbo(0, new float[]{5, 6});
        assertEquals(-1, data.dirtyStart(0));
    }

    @Test
    void anIntegerOrEarlierVboIsUnknownUntilUpdated() {
        MeshCpuData data = new MeshCpuData();
        data.addVbo(1, null);
        assertNull(data.vbo(0));
        assertNull(data.vbo(1));
        assertNull(data.vbo(5));

        // A part of an unknown vbo leaves it unknown
        data.setSubVbo(1, new float[]{1}, 0);
        assertNull(data.vbo(1));

        data.setVbo(1, new float[]{1, 2});
        assertArrayEquals(new float[]{1, 2}, data.vbo(1));
    }

    // Mesh.unloadVBO: the copies are dropped, the next vbo added is the first again
    @Test
    void theVbosAreDroppedWhenCleared() {
        MeshCpuData data = new MeshCpuData();
        data.addVbo(0, new float[]{1, 2});
        data.addVbo(1, new float[]{3, 4});
        data.clearVbos();
        assertNull(data.vbo(0));
        assertNull(data.vbo(1));

        data.addVbo(0, new float[]{5, 6});
        assertArrayEquals(new float[]{5, 6}, data.vbo(0));
        assertNull(data.vbo(1));
    }

    // The offset is in bytes, like glBufferSubData; what is past the end is ignored
    @Test
    void aPartIsReplacedFromItsOffsetInBytes() {
        MeshCpuData data = new MeshCpuData();
        data.addVbo(0, new float[]{0, 0, 0, 0});
        data.setSubVbo(0, new float[]{7, 8}, 2 * Float.BYTES);
        assertArrayEquals(new float[]{0, 0, 7, 8}, data.vbo(0));

        data.setSubVbo(0, new float[]{1, 2, 3}, 3 * Float.BYTES);
        assertArrayEquals(new float[]{0, 0, 7, 1}, data.vbo(0));
    }

    @Test
    void theIndicesGiveTheLargestOneDrawn() {
        MeshCpuData data = new MeshCpuData();
        assertEquals(-1, data.maxIndex());

        data.setIndices(new int[]{0, 3, 2, 0, 2, 1, 7, 7, 7}, 6);
        assertEquals(6, data.indexCount());
        assertEquals(3, data.maxIndex());

        // setEboNumberIndex: more indices drawn
        data.setIndexCount(9);
        assertEquals(7, data.maxIndex());

        // updateEbo: from an offset in bytes
        data.updateIndices(new int[]{12}, 8 * Integer.BYTES);
        assertEquals(12, data.maxIndex());
        assertEquals(12, data.indices()[8]);

        // More indices drawn than known: unknown
        data.setIndexCount(10);
        assertEquals(Integer.MAX_VALUE, data.maxIndex());
    }

    // The ebo sent before the copy was asked is unknown until set again
    @Test
    void earlierIndicesAreUnknown() {
        MeshCpuData data = new MeshCpuData();
        data.forgetIndices();
        assertEquals(Integer.MAX_VALUE, data.maxIndex());

        data.setIndices(new int[]{0, 1, 2}, 3);
        assertEquals(2, data.maxIndex());
    }

    // The box of the positions of a vbo (view culling): computed again only when a vbo changed
    @Test
    void theBoxOfThePositionsFollowsTheVbos() {
        MeshCpuData data = new MeshCpuData();
        float[] box = new float[4];
        assertFalse(data.positionBox(0, box));

        data.addVbo(0, new float[]{1, 2, -3, 4, 5, -6});
        data.addVbo(1, null);
        assertTrue(data.positionBox(0, box));
        assertArrayEquals(new float[]{-3, -6, 5, 4}, box);
        // An unknown vbo: no box
        assertFalse(data.positionBox(1, box));

        data.setVbo(0, new float[]{10, 10, 20, 30});
        assertTrue(data.positionBox(0, box));
        assertArrayEquals(new float[]{10, 10, 20, 30}, box);

        // Bytes 8 to 15: the second position
        data.setSubVbo(0, new float[]{-1, 50}, 8);
        assertTrue(data.positionBox(0, box));
        assertArrayEquals(new float[]{-1, 10, 10, 50}, box);

        // No position, or one that is not finite: no box
        data.setVbo(0, new float[0]);
        assertFalse(data.positionBox(0, box));
        data.setVbo(0, new float[]{0, Float.NaN});
        assertFalse(data.positionBox(0, box));

        data.clearVbos();
        assertFalse(data.positionBox(0, box));
    }
}
