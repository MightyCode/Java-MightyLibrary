package io.github.mightycode.mightylib.resources.sound.decoder;

import org.lwjgl.system.MemoryUtil;

import java.nio.ShortBuffer;

/** Native 16 bit samples growing while a decoder produces them, for the formats whose length is not known first. */
public final class PcmBuilder {
    private ShortBuffer buffer;

    // initialShorts: a guess of the final size (0 when unknown)
    public PcmBuilder(int initialShorts) {
        buffer = MemoryUtil.memAllocShort(Math.max(initialShorts, 16 * 1024));
    }

    public void append(short[] samples, int offset, int length) {
        if (buffer.remaining() < length) {
            int capacity = buffer.capacity();
            int needed = buffer.position() + length;
            while (capacity < needed)
                capacity = capacity > Integer.MAX_VALUE / 2 ? Integer.MAX_VALUE : capacity * 2;

            buffer = MemoryUtil.memRealloc(buffer, capacity);
        }

        buffer.put(samples, offset, length);
    }

    // The samples, owned by the caller (MemoryUtil.memFree) from now on
    public ShortBuffer finish() {
        ShortBuffer result = buffer;
        buffer = null;
        result.flip();
        return result;
    }

    // Frees what finish did not give away
    public void free() {
        if (buffer != null) {
            MemoryUtil.memFree(buffer);
            buffer = null;
        }
    }
}
