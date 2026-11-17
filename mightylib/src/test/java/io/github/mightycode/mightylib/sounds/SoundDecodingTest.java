package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.TestFiles;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.junit.jupiter.api.Test;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decoding of ogg files by chunks (no OpenAL needed): same samples whatever the chunk, stopped between two chunks.
 */
class SoundDecodingTest {
    private static final ResourceSources SOURCES = ResourceSources.of(
            new FolderSource(TestFiles.path("sounds")));
    private static final String FILE = "selectogg.ogg";

    private static ShortBuffer decode(int chunkSamples, Runnable checkCancelled, int[] channels) throws Exception {
        ByteBuffer file = SOURCES.readNative(FILE);
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            ShortBuffer pcm = SoundDataType.readVorbis(file, info, checkCancelled, chunkSamples);
            channels[0] = info.channels();
            return pcm;
        } finally {
            MemoryUtil.memFree(file);
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
    void checksTheCancellationBetweenChunks() {
        AtomicInteger checks = new AtomicInteger();
        int[] channels = new int[1];

        assertThrows(CancellationException.class, () -> decode(100, () -> {
            if (checks.incrementAndGet() == 4)
                throw new CancellationException();
        }, channels));

        assertEquals(4, checks.get(), "stopped at the check, not decoded to the end");
    }

    @Test
    void notAVorbisFileFails() throws IOException {
        ByteBuffer file = SOURCES.readNative("sounds.gaintree");
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            assertThrows(IllegalStateException.class, () -> SoundDataType.readVorbis(file, info, () -> {}, 1000));
        } finally {
            MemoryUtil.memFree(file);
        }
    }

    @Test
    void releaseFreesOnlyNativeBuffers() {
        SoundLoadInfo info = new SoundLoadInfo(1, 44100, MemoryUtil.memAllocShort(16), true);
        info.release();
        assertNull(info.getBuffer());
        assertFalse(info.isNativeBuffer());
        // Released twice: nothing freed twice
        info.release();

        // A GC managed buffer must never be passed to memFree
        info = new SoundLoadInfo(1, 44100, org.lwjgl.BufferUtils.createShortBuffer(16), false);
        info.release();
        assertNull(info.getBuffer());
    }

    @Test
    void aMissingFileGivesAClearError() {
        NoSuchFileException error = assertThrows(NoSuchFileException.class, () -> SOURCES.readNative("missing.ogg"));
        assertTrue(error.getMessage().contains("missing.ogg"));
    }
}
