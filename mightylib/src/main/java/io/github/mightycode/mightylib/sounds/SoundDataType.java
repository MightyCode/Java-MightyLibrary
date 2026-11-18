package io.github.mightycode.mightylib.sounds;

import org.apiguardian.api.API;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.stb.STBVorbis.*;

public final class SoundDataType {
    private SoundDataType() {}

    // Samples (all channels) decoded between two checks of the cancellation
    public static final int DECODE_CHUNK_SAMPLES = 64 * 1024;

    /**
     * Decodes an Ogg Vorbis file read in a direct buffer (ResourceSources.readNative), in chunks of chunkSamples,
     * calling checkCancelled between two steps (it throws to stop). The file stays the caller's. The result is native
     * memory (MemoryUtil.memFree), freed here when the decoding stops or fails.
     * For OggDecoder; exposes a type of LWJGL, which the engine does not export (docs/later.md).
     */
    @API(status = INTERNAL)
    public static ShortBuffer readVorbis(ByteBuffer vorbis, STBVorbisInfo info, Runnable checkCancelled,
                                         int chunkSamples) {
        checkCancelled.run();

        long decoder = NULL;
        ShortBuffer pcm = null;
        boolean done = false;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            decoder = stb_vorbis_open_memory(vorbis, error, null);
            if (decoder == NULL)
                throw new IllegalStateException("Failed to open Ogg Vorbis file. Error: " + error.get(0));

            stb_vorbis_get_info(decoder, info);
            int channels = info.channels();

            pcm = MemoryUtil.memAllocShort(stb_vorbis_stream_length_in_samples(decoder) * channels);

            int decoded = 0;
            while (decoded < pcm.capacity()) {
                checkCancelled.run();

                pcm.limit(Math.min(pcm.capacity(), decoded + chunkSamples)).position(decoded);
                int perChannel = stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
                if (perChannel == 0)
                    break;

                decoded += perChannel * channels;
            }

            pcm.limit(decoded).position(0);
            done = true;
            return pcm;
        } finally {
            if (decoder != NULL)
                stb_vorbis_close(decoder);

            if (!done && pcm != null)
                MemoryUtil.memFree(pcm);
        }
    }
}
