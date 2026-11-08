package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CancellationException;

/** Mp3, decoded frame by frame with JLayer. */
public class Mp3Decoder implements SoundDecoder {
    @Override
    public String[] extensions() {
        return new String[]{".mp3"};
    }

    @Override
    public SoundLoadInfo decode(String path, Runnable checkCancelled) {
        PcmBuilder builder = null;

        try (InputStream input = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            Bitstream bitstream = new Bitstream(input);
            Decoder decoder = new Decoder();
            builder = new PcmBuilder(0);
            int channels = 0, rate = 0;

            try {
                Header header;
                while ((header = bitstream.readFrame()) != null) {
                    checkCancelled.run();

                    SampleBuffer out = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    channels = out.getChannelCount();
                    rate = out.getSampleFrequency();
                    builder.append(out.getBuffer(), 0, out.getBufferLength());
                    bitstream.closeFrame();
                }
            } finally {
                bitstream.close();
            }

            if (channels == 0)
                throw new ResourceException("no mp3 frame in " + path);

            SoundLoadInfo info = new SoundLoadInfo();
            info.Channel = channels;
            info.SampleRate = rate;
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
