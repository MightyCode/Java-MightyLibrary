package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The blocks of the ring buffer waited for before a range is written again
class RingBlocksTest {
    @Test
    void onlyTheBlocksOverTheRangeAreWaitedFor() {
        RingBlocks blocks = new RingBlocks();
        blocks.add(0, 100, 1);
        blocks.add(100, 200, 2);
        blocks.add(200, 300, 3);

        assertEquals(0, blocks.blocksOver(300, 400));
        assertEquals(1, blocks.blocksOver(0, 50));
        // The second one over it: the first one is waited for too (in order)
        assertEquals(2, blocks.blocksOver(150, 180));
        // Touching is not overlapping
        assertEquals(0, blocks.blocksOver(300, 1000));
        assertEquals(1, blocks.blocksOver(50, 100));
    }

    // The ring wrapped twice: an old block at the end of the buffer comes before a newer one at its start
    @Test
    void aNewerBlockOverTheRangeIsWaitedForAfterAClearOldOne() {
        RingBlocks blocks = new RingBlocks();
        blocks.add(950, 1000, 1);
        blocks.add(0, 100, 2);

        assertEquals(2, blocks.blocksOver(0, 950));
    }

    @Test
    void theQueueGrowsInOrder() {
        RingBlocks blocks = new RingBlocks();
        for (int i = 0; i < 100; ++i) {
            blocks.add(i * 10L, i * 10L + 10, i + 1);
            if (i % 3 == 0) {
                assertEquals(i / 3 + 1, blocks.firstFence());
                blocks.removeFirst();
            }
        }

        assertEquals(66, blocks.count());
        assertEquals(35, blocks.firstFence());
        assertEquals(66, blocks.blocksOver(0, 1000));
        assertEquals(1, blocks.blocksOver(340, 350));
    }

    @Test
    void anEmptyRingHasNoFence() {
        RingBlocks blocks = new RingBlocks();
        assertEquals(0, blocks.blocksOver(0, 100));
        assertThrows(IllegalStateException.class, blocks::firstFence);
        assertThrows(IllegalStateException.class, blocks::removeFirst);
    }
}
