package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.sounds.SoundDataType;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import org.lwjgl.stb.STBVorbisInfo;

import java.nio.ShortBuffer;
import java.util.concurrent.CancellationException;

public class OggDecoder implements SoundDecoder {
    @Override
    public String[] extensions() {
        return new String[]{".ogg"};
    }

    @Override
    public SoundLoadInfo decode(String path, Runnable checkCancelled) {
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            ShortBuffer samples = SoundDataType.readVorbis(path, 32 * 1024, info, checkCancelled,
                    SoundDataType.DECODE_CHUNK_SAMPLES);
            return new SoundLoadInfo(info.channels(), info.sample_rate(), samples, true);
        } catch (ResourceException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceException("can't read the sound file " + path, e);
        }
    }
}
