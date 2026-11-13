package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CancellationException;

/** Wav (and the other encodings of javax.sound.sampled), converted to 16 bit. */
public class WavDecoder implements SoundDecoder {
    private static final int CHUNK_BYTES = 64 * 1024;

    @Override
    public String[] extensions() {
        return new String[]{".wav"};
    }

    @Override
    public SoundLoadInfo decode(String path, Runnable checkCancelled) {
        PcmBuilder builder = null;

        try (AudioInputStream source = AudioSystem.getAudioInputStream(new File(path))) {
            AudioFormat format = source.getFormat();
            int channels = format.getChannels();
            int rate = (int) format.getSampleRate();
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, channels, channels * 2, rate, false);

            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(target, source)) {
                long frames = source.getFrameLength();
                builder = new PcmBuilder(frames > 0 && frames < Integer.MAX_VALUE / 4 ? (int) frames * channels : 0);

                byte[] bytes = new byte[CHUNK_BYTES - CHUNK_BYTES % (channels * 2)];
                short[] samples = new short[bytes.length / 2];
                int read;
                checkCancelled.run();
                while ((read = pcm.read(bytes)) > 0) {
                    int count = read / 2;
                    ByteBuffer.wrap(bytes, 0, count * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples, 0, count);
                    builder.append(samples, 0, count);
                    checkCancelled.run();
                }
            }

            return new SoundLoadInfo(channels, rate, builder.finish(), true);
        } catch (UnsupportedAudioFileException e) {
            throw new ResourceException("unsupported wav file " + path, e);
        } catch (ResourceException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceException("can't read the sound file " + path, e);
        } finally {
            if (builder != null)
                builder.free();
        }
    }
}
