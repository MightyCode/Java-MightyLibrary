package io.github.mightycode.mightylib.sounds;

import org.lwjgl.system.MemoryUtil;

import java.nio.ShortBuffer;
import java.util.Objects;

// Decoded samples of a sound, until they are sent to OpenAL
public class SoundLoadInfo {
    private final int channels;
    private final int sampleRate;
    private ShortBuffer buffer;
    // Buffer allocated by MemoryUtil (decoded files): freed by release; a BufferUtils buffer is left to the GC
    private boolean nativeBuffer;

    public SoundLoadInfo(int channels, int sampleRate, ShortBuffer buffer, boolean nativeBuffer) {
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.buffer = Objects.requireNonNull(buffer, "buffer");
        this.nativeBuffer = nativeBuffer;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    // The samples; null once released
    public ShortBuffer getBuffer() {
        return buffer;
    }

    public boolean isNativeBuffer() {
        return nativeBuffer;
    }

    // The decoded samples are no longer needed (sent to OpenAL, or loading abandoned)
    public void release() {
        if (nativeBuffer && buffer != null)
            MemoryUtil.memFree(buffer);

        buffer = null;
        nativeBuffer = false;
    }
}
