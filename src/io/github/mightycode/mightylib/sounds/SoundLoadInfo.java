package io.github.mightycode.mightylib.sounds;

import org.lwjgl.system.MemoryUtil;

import java.nio.ShortBuffer;

public class SoundLoadInfo {
    public int Channel;
    public ShortBuffer Buffer;
    public int SampleRate;
    // Buffer allocated by MemoryUtil (decoded files): freed by release; a BufferUtils buffer is left to the GC
    public boolean nativeBuffer;

    // The decoded samples are no longer needed (sent to OpenAL, or loading abandoned)
    public void release() {
        if (nativeBuffer && Buffer != null)
            MemoryUtil.memFree(Buffer);

        Buffer = null;
        nativeBuffer = false;
    }
}
