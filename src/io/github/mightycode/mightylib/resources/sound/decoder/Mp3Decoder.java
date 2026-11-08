package io.github.mightycode.mightylib.resources.sound.decoder;

import fr.delthas.javamp3.Sound;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CancellationException;

/** Mp3 (MPEG 1/2/2.5 layers I to III), decoded with JavaMP3 (MIT) which gives 16 bit little endian samples. */
public class Mp3Decoder implements SoundDecoder {
    // About one frame: a file cut in the middle of a frame loses at most that much of its end
    private static final int CHUNK_BYTES = 4608;

    @Override
    public String[] extensions() {
        return new String[]{".mp3"};
    }

    @Override
    public SoundLoadInfo decode(String path, Runnable checkCancelled) {
        PcmBuilder builder = null;

        try (InputStream input = new BufferedInputStream(Files.newInputStream(Paths.get(path)));
             Sound sound = new Sound(input)) {
            builder = new PcmBuilder(0);

            // Whole frames only: a read can stop in the middle of a sample
            byte[] bytes = new byte[CHUNK_BYTES];
            short[] samples = new short[CHUNK_BYTES / 2];
            int pending = 0;
            boolean hasSamples = false;
            checkCancelled.run();
            while (true) {
                int read;
                try {
                    read = sound.read(bytes, pending, bytes.length - pending);
                } catch (java.io.IOException | IndexOutOfBoundsException e) {
                    // JavaMP3 fails on a last frame cut short (the file is truncated or ends with a tag): the sound is kept
                    if (!hasSamples)
                        throw e;

                    System.err.println("--Mp3 file ends with an incomplete frame, the end is ignored: " + path);
                    break;
                }

                if (read <= 0)
                    break;

                hasSamples = true;
                int available = pending + read;
                int count = available / 2;
                ByteBuffer.wrap(bytes, 0, count * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples, 0, count);
                builder.append(samples, 0, count);

                pending = available - count * 2;
                if (pending > 0)
                    bytes[0] = bytes[count * 2];

                checkCancelled.run();
            }

            SoundLoadInfo info = new SoundLoadInfo();
            info.Channel = sound.isStereo() ? 2 : 1;
            info.SampleRate = sound.getSamplingFrequency();
            info.Buffer = builder.finish();
            info.nativeBuffer = true;
            return info;
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
