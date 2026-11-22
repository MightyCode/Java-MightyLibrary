package io.github.mightycode.mightylib.graphics.debug;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/*
 * The drawings of a DebugDraw2D, without GL: their kind, space, color, width, values (in one array of floats), text,
 * lifetime and handle, in the order they were asked. Arrays reused: once they fit, drawings of one frame allocate
 * nothing.
 */
final class DebugDrawQueue {
    static final byte LINE = 0, POLYLINE = 1, CLOSED_POLYLINE = 2, RECT = 3, CIRCLE = 4, CROSS = 5, ARROW = 6, TEXT = 7;

    private int count;
    private byte[] kinds = new byte[16];
    private EDebugSpace[] spaces = new EDebugSpace[16];
    private int[] colors = new int[16];
    private float[] widths = new float[16];
    private int[] firstValues = new int[16];
    private int[] valueCounts = new int[16];
    private String[] texts = new String[16];
    private IDebugLifetime[] lifetimes = new IDebugLifetime[16];
    private DebugDrawHandle[] handles = new DebugDrawHandle[16];

    private float[] values = new float[64];
    private int valueCount;

    // A drawing (its values follow, with value): its handle, NONE for one frame
    DebugDrawHandle add(byte kind, EDebugSpace space, int color, float width, String text, IDebugLifetime lifetime) {
        if (count == kinds.length) {
            int size = count * 2;
            kinds = Arrays.copyOf(kinds, size);
            spaces = Arrays.copyOf(spaces, size);
            colors = Arrays.copyOf(colors, size);
            widths = Arrays.copyOf(widths, size);
            firstValues = Arrays.copyOf(firstValues, size);
            valueCounts = Arrays.copyOf(valueCounts, size);
            texts = Arrays.copyOf(texts, size);
            lifetimes = Arrays.copyOf(lifetimes, size);
            handles = Arrays.copyOf(handles, size);
        }

        DebugDrawHandle handle = DebugLifetimes.isOneFrame(lifetime) ? DebugDrawHandle.NONE : DebugDrawHandle.create();
        kinds[count] = kind;
        spaces[count] = space;
        colors[count] = color;
        widths[count] = width;
        firstValues[count] = valueCount;
        valueCounts[count] = 0;
        texts[count] = text;
        lifetimes[count] = lifetime;
        handles[count] = handle;
        ++count;
        return handle;
    }

    // A value of the last drawing added
    void value(float value) {
        if (valueCount == values.length)
            values = Arrays.copyOf(values, valueCount * 2);
        values[valueCount++] = value;
        ++valueCounts[count - 1];
    }

    /*
     * Before the drawings are drawn: the removed ones and those whose lifetime ends are dropped (each lifetime asked
     * once)
     */
    void beginFrame() {
        int kept = 0;
        int keptValues = 0;
        for (int i = 0; i < count; ++i) {
            if (handles[i].isRemoved() || !lifetimes[i].alive())
                continue;
            keptValues = keep(i, kept++, keptValues);
        }
        truncate(kept, keptValues);
    }

    // After they are drawn: those of one frame are dropped
    void endFrame() {
        int kept = 0;
        int keptValues = 0;
        for (int i = 0; i < count; ++i) {
            if (DebugLifetimes.isOneFrame(lifetimes[i]))
                continue;
            keptValues = keep(i, kept++, keptValues);
        }
        truncate(kept, keptValues);
    }

    // Moves drawing from to the place to, its values to keptValues; the values kept after it
    private int keep(int from, int to, int keptValues) {
        int first = firstValues[from];
        int length = valueCounts[from];
        if (first != keptValues)
            System.arraycopy(values, first, values, keptValues, length);

        kinds[to] = kinds[from];
        spaces[to] = spaces[from];
        colors[to] = colors[from];
        widths[to] = widths[from];
        firstValues[to] = keptValues;
        valueCounts[to] = length;
        texts[to] = texts[from];
        lifetimes[to] = lifetimes[from];
        handles[to] = handles[from];
        return keptValues + length;
    }

    private void truncate(int kept, int keptValues) {
        Arrays.fill(texts, kept, count, null);
        Arrays.fill(lifetimes, kept, count, null);
        Arrays.fill(handles, kept, count, null);
        Arrays.fill(spaces, kept, count, null);
        count = kept;
        valueCount = keptValues;
    }

    void clear() {
        truncate(0, 0);
    }

    int size() {
        return count;
    }

    byte kind(int i) { return kinds[i]; }
    EDebugSpace space(int i) { return spaces[i]; }
    int color(int i) { return colors[i]; }
    float width(int i) { return widths[i]; }
    int valueCount(int i) { return valueCounts[i]; }
    float value(int i, int index) { return values[firstValues[i] + index]; }
    String text(int i) { return texts[i]; }

    // The drawings by the name of their lifetime
    Map<String, Integer> countByLifetime() {
        Map<String, Integer> counts = new TreeMap<>();
        for (int i = 0; i < count; ++i)
            counts.merge(lifetimes[i].name(), 1, Integer::sum);
        return counts;
    }

    // Capacities (tests: nothing grows once the frames are stable)
    int capacity() { return kinds.length; }
    int valueCapacity() { return values.length; }
}
