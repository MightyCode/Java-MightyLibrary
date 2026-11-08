package io.github.mightycode.mightylib.sounds;

import org.junit.jupiter.api.Test;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryUtil;

import java.nio.ShortBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decoding of ogg files by chunks (no OpenAL needed): same samples whatever the chunk, stopped between two chunks.
 */
class SoundDecodingTest {
    private static final String FILE = "resources/sounds/select.ogg";

    private static ShortBuffer decode(int chunkSamples, Runnable checkCancelled, int[] channels) throws Exception {
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            ShortBuffer pcm = SoundDataType.readVorbis(FILE, 32 * 1024, info, checkCancelled, chunkSamples);
            channels[0] = info.channels();
            return pcm;
        }
    }

    @Test
    void chunksGiveTheSameSamplesAsOneBlock() throws Exception {
        int[] channels = new int[1];
        ShortBuffer whole = decode(Integer.MAX_VALUE / 4, () -> {}, channels);
        ShortBuffer chunked = decode(100, () -> {}, channels);

        try {
            assertTrue(whole.remaining() > 0);
            assertEquals(whole.remaining(), chunked.remaining());
            assertEquals(0, whole.compareTo(chunked), "same samples");
            assertEquals(0, whole.remaining() % channels[0], "whole frames");
        } finally {
            MemoryUtil.memFree(whole);
            MemoryUtil.memFree(chunked);
        }
    }

    @Test
    void checksTheCancellationBetweenChunks() throws Exception {
        AtomicInteger checks = new AtomicInteger();
        int[] channels = new int[1];

        assertThrows(CancellationException.class, () -> decode(100, () -> {
            if (checks.incrementAndGet() == 4)
                throw new CancellationException();
        }, channels));

        assertEquals(4, checks.get(), "stopped at the check, not decoded to the end");
    }

    @Test
    void missingFileFails() {
        assertThrows(Exception.class, () -> {
            try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
                SoundDataType.readVorbis("resources/sounds/missing.ogg", 1024, info, () -> {}, 1000);
            }
        });
    }

    @Test
    void releaseFreesOnlyNativeBuffers() {
        SoundLoadInfo info = new SoundLoadInfo();
        info.Buffer = MemoryUtil.memAllocShort(16);
        info.nativeBuffer = true;
        info.release();
        assertNull(info.Buffer);
        assertFalse(info.nativeBuffer);

        // A GC managed buffer must never be passed to memFree
        info.Buffer = org.lwjgl.BufferUtils.createShortBuffer(16);
        info.release();
        assertNull(info.Buffer);
    }
}
