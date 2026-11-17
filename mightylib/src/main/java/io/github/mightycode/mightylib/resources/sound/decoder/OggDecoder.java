package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.sounds.SoundDataType;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.CancellationException;

public final class OggDecoder implements SoundDecoder {
    @Override
    public String[] extensions() {
        return new String[]{".ogg"};
    }

    @Override
    public SoundLoadInfo decode(ResourceSources sources, String path, Runnable checkCancelled) {
        ByteBuffer vorbis = null;
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            vorbis = sources.readNative(path);
            ShortBuffer samples = SoundDataType.readVorbis(vorbis, info, checkCancelled,
                    SoundDataType.DECODE_CHUNK_SAMPLES);
            return new SoundLoadInfo(info.channels(), info.sample_rate(), samples, true);
        } catch (ResourceException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceException("can't read the sound file " + path, e);
        } finally {
            // The decoder reads it until its end: freed once the samples are decoded
            if (vorbis != null)
                MemoryUtil.memFree(vorbis);
        }
    }
}
