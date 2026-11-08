package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.sounds.SoundDataType;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import org.lwjgl.stb.STBVorbisInfo;

import java.util.concurrent.CancellationException;

public class OggDecoder implements SoundDecoder {
    @Override
    public String[] extensions() {
        return new String[]{".ogg"};
    }

    @Override
    public SoundLoadInfo decode(String path, Runnable checkCancelled) {
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            SoundLoadInfo result = new SoundLoadInfo();
            result.Buffer = SoundDataType.readVorbis(path, 32 * 1024, info, checkCancelled, SoundDataType.DECODE_CHUNK_SAMPLES);
            result.nativeBuffer = true;
            result.Channel = info.channels();
            result.SampleRate = info.sample_rate();
            return result;
        } catch (ResourceException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceException("can't read the sound file " + path, e);
        }
    }
}
