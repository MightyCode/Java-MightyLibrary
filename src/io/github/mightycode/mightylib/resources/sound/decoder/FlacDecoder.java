package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import org.jflac.FLACDecoder;
import org.jflac.PCMProcessor;
import org.jflac.frame.Frame;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CancellationException;

/** Flac (8, 16 or 24 bits), decoded frame by frame with jFLAC, converted to 16 bit. */
public class FlacDecoder implements SoundDecoder {
    @Override
    public String[] extensions() {
        return new String[]{".flac"};
    }

    private static class Collector implements PCMProcessor {
        final PcmBuilder builder;
        int channels, rate, bits;
        short[] samples = new short[0];

        Collector(PcmBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void processStreamInfo(StreamInfo info) {
            channels = info.getChannels();
            rate = info.getSampleRate();
            bits = info.getBitsPerSample();
        }

        @Override
        public void processPCM(ByteData pcm) {
            int bytesPerSample = bits / 8;
            int count = pcm.getLen() / bytesPerSample;
            if (samples.length < count)
                samples = new short[count];

            byte[] data = pcm.getData();
            for (int i = 0; i < count; ++i) {
                int at = i * bytesPerSample;
                if (bytesPerSample == 1) {
                    // 8 bit samples are unsigned
                    samples[i] = (short) (((data[at] & 0xFF) - 128) << 8);
                } else {
                    // Little endian: the 16 high bits are kept
                    samples[i] = (short) ((data[at + bytesPerSample - 2] & 0xFF) | (data[at + bytesPerSample - 1] << 8));
                }
            }

            builder.append(samples, 0, count);
        }
    }

    @Override
    public SoundLoadInfo decode(String path, Runnable checkCancelled) {
        PcmBuilder builder = null;

        try (InputStream input = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            FLACDecoder decoder = new FLACDecoder(input);
            builder = new PcmBuilder(0);
            Collector collector = new Collector(builder);

            decoder.readMetadata();
            collector.processStreamInfo(decoder.getStreamInfo());
            if (collector.bits != 8 && collector.bits != 16 && collector.bits != 24)
                throw new ResourceException("unsupported flac sample size (" + collector.bits + " bits): " + path);

            // One frame per step (readFrame fails on this library version: readNextFrame then decodeFrame work)
            Frame frame;
            while ((frame = decoder.readNextFrame()) != null) {
                checkCancelled.run();
                collector.processPCM(decoder.decodeFrame(frame, null));
            }

            SoundLoadInfo info = new SoundLoadInfo();
            info.Channel = collector.channels;
            info.SampleRate = collector.rate;
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
