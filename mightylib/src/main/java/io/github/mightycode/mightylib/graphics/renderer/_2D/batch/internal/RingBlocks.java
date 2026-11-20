package io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal;

/**
 * The blocks of the ring buffer still maybe read by the GPU, oldest first: [start, end) in the buffer and the fence
 * put after their draws (a circular queue). No GL: RingUploader waits for the fences.
 */
final class RingBlocks {
    private long[] starts = new long[32];
    private long[] ends = new long[32];
    private long[] fences = new long[32];
    private int first;
    private int count;

    int count() {
        return count;
    }

    // The fence of the oldest block
    long firstFence() {
        if (count == 0)
            throw new IllegalStateException("No block in the ring");
        return fences[first];
    }

    // The oldest block is done with (its fence deleted by the caller)
    void removeFirst() {
        if (count == 0)
            throw new IllegalStateException("No block in the ring");
        fences[first] = 0L;
        first = (first + 1) % fences.length;
        --count;
    }

    void add(long start, long end, long fence) {
        if (count == fences.length) {
            int size = fences.length * 2;
            long[] newStarts = new long[size];
            long[] newEnds = new long[size];
            long[] newFences = new long[size];
            for (int i = 0; i < count; ++i) {
                int from = (first + i) % fences.length;
                newStarts[i] = starts[from];
                newEnds[i] = ends[from];
                newFences[i] = fences[from];
            }
            starts = newStarts;
            ends = newEnds;
            fences = newFences;
            first = 0;
        }

        int last = (first + count) % fences.length;
        starts[last] = start;
        ends[last] = end;
        fences[last] = fence;
        ++count;
    }

    /*
     * The oldest blocks to wait for before [start, end) is written: up to the last one over it, wherever it is in the
     * queue (after the ring wrapped twice, an old block at the end of the buffer comes before a newer one at its
     * start: the first block may be clear of the range while a later one is not).
     */
    int blocksOver(long start, long end) {
        int over = 0;
        for (int i = 0; i < count; ++i) {
            int at = (first + i) % fences.length;
            if (starts[at] < end && ends[at] > start)
                over = i + 1;
        }

        return over;
    }
}
