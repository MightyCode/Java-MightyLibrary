package io.github.mightycode.mightylib.graphics.text;

import java.util.ArrayList;
import java.util.List;

/**
 * The texts drawn during the last complete frame, to check what is on screen as text (command {@code gl text}).
 * Main thread only. Entries are reused from one frame to the next: nothing is allocated once the frames are stable.
 */
public final class DrawnTexts {
    public static final class Entry {
        private String text = "";
        private float x;
        private float y;
        private float width;
        private float height;
        private float fontSize;

        public String text() { return text; }

        /** Left of the first character, in the space of the camera of the text. */
        public float x() { return x; }

        /** Top of the first line, in the space of the camera of the text. */
        public float y() { return y; }

        public float width() { return width; }

        public float height() { return height; }

        public float fontSize() { return fontSize; }
    }

    private static List<Entry> current = new ArrayList<>();
    private static List<Entry> last = new ArrayList<>();
    private static int currentCount = 0;
    private static int lastCount = 0;

    private DrawnTexts() {}

    /** The frame that starts is the one being recorded, the one that ends becomes the one given by {@link #last()}. */
    public static void beginFrame() {
        List<Entry> swap = last;
        last = current;
        lastCount = currentCount;
        current = swap;
        currentCount = 0;
    }

    static void record(String text, float x, float y, float width, float height, float fontSize) {
        if (currentCount == current.size())
            current.add(new Entry());

        Entry entry = current.get(currentCount++);
        entry.text = text;
        entry.x = x;
        entry.y = y;
        entry.width = width;
        entry.height = height;
        entry.fontSize = fontSize;
    }

    /** Copy of the texts of the last complete frame, from top to bottom then from left to right. */
    public static List<Entry> last() {
        List<Entry> copy = new ArrayList<>(lastCount);
        for (int i = 0; i < lastCount; ++i) {
            Entry source = last.get(i);
            Entry entry = new Entry();
            entry.text = source.text;
            entry.x = source.x;
            entry.y = source.y;
            entry.width = source.width;
            entry.height = source.height;
            entry.fontSize = source.fontSize;
            copy.add(entry);
        }

        copy.sort((a, b) -> a.y != b.y ? Float.compare(a.y, b.y) : Float.compare(a.x, b.x));
        return copy;
    }

    static void clear() {
        currentCount = 0;
        lastCount = 0;
    }
}
