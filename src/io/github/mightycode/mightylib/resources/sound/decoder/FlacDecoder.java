package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CancellationException;

/** Flac (4 to 24 bits), decoded frame by frame by FlacStream and converted to 16 bit. */
public class FlacDecoder implements SoundDecoder {
    @Override
    public String[] extensions() {
        return new String[]{".flac"};
    }

    @Override
    public SoundLoadInfo decode(String path, Runnable checkCancelled) {
        PcmBuilder builder = null;

        try {
            FlacStream stream = new FlacStream(Files.readAllBytes(Paths.get(path)));
            int estimate = stream.totalSamples > 0 && stream.totalSamples < Integer.MAX_VALUE / 4
                    ? (int) stream.totalSamples * stream.channels : 0;
            builder = new PcmBuilder(estimate);

            int channels = stream.channels;
            int[][] frame = stream.createBuffers();
            short[] interleaved = new short[0];

            // To 16 bit: the missing bits are zero, the extra bits are dropped
            int shift = stream.bitsPerSample - 16;

            int size;
            while ((size = stream.nextFrame(frame)) > 0) {
                checkCancelled.run();

                if (interleaved.length < size * channels)
                    interleaved = new short[size * channels];

                for (int channel = 0; channel < channels; ++channel) {
                    int[] samples = frame[channel];
                    for (int i = 0; i < size; ++i)
                        interleaved[i * channels + channel] = (short) (shift >= 0 ? samples[i] >> shift : samples[i] << -shift);
                }

                builder.append(interleaved, 0, size * channels);
            }

            return new SoundLoadInfo(channels, stream.sampleRate, builder.finish(), true);
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
