package io.github.mightycode.mightylib.sounds;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.sql.Types.NULL;
import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.stb.STBVorbis.*;

public class SoundDataType {

    // Samples (all channels) decoded between two checks of the cancellation
    public static final int DECODE_CHUNK_SAMPLES = 64 * 1024;

    public static ShortBuffer readVorbis(String resource, int bufferSize, STBVorbisInfo info) throws Exception {
        return readVorbis(resource, bufferSize, info, () -> {}, DECODE_CHUNK_SAMPLES);
    }

    /**
     * Reads then decodes the file in chunks of chunkSamples, calling checkCancelled between two steps (it throws to stop).
     * The result is native memory (MemoryUtil.memFree), freed here when the decoding stops or fails.
     */
    public static ShortBuffer readVorbis(String resource, int bufferSize, STBVorbisInfo info,
                                         Runnable checkCancelled, int chunkSamples) throws Exception {
        ByteBuffer vorbis = ioResourceToByteBuffer(resource, bufferSize);
        checkCancelled.run();

        long decoder = NULL;
        ShortBuffer pcm = null;
        boolean done = false;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            decoder = stb_vorbis_open_memory(vorbis, error, null);
            if (decoder == NULL)
                throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0));

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

    public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;

        Path path = Paths.get(resource);
        if (Files.isReadable(path)) {
            try (SeekableByteChannel fc = Files.newByteChannel(path)) {
                buffer = createByteBuffer((int) fc.size() + 1);
                while (fc.read(buffer) != -1) ;
            }
        } else {
            try (
                    InputStream source = SoundSource.class.getResourceAsStream(resource);
                    ReadableByteChannel rbc = Channels.newChannel(source)) {
                buffer = createByteBuffer(bufferSize);

                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) {
                        break;
                    }
                    if (buffer.remaining() == 0) {
                        buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                    }
                }
            }
        }

        buffer.flip();
        return buffer;
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
}
