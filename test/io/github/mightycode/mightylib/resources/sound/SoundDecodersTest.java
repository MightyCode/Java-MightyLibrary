package io.github.mightycode.mightylib.resources.sound;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.sound.decoder.*;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The same sound in four formats (resources/sounds/select*): each decoder gives 16 bit samples of about the same
 * length, can be stopped, and frees its memory. No OpenAL needed.
 */
class SoundDecodersTest {
    private static final String FOLDER = "resources/sounds/";

    private static SoundLoadInfo decode(SoundDecoder decoder, String file) {
        return decoder.decode(FOLDER + file, () -> {});
    }

    private static double seconds(SoundLoadInfo info) {
        return (double) info.getBuffer().remaining() / info.getChannels() / info.getSampleRate();
    }

    @Test
    void everyFormatGivesTheSameSoundDuration() {
        SoundLoadInfo ogg = decode(new OggDecoder(), "selectogg.ogg");
        SoundLoadInfo wav = decode(new WavDecoder(), "selectwav.wav");
        SoundLoadInfo mp3 = decode(new Mp3Decoder(), "selectmp3.mp3");
        SoundLoadInfo flac = decode(new FlacDecoder(), "selectflac.flac");

        try {
            for (SoundLoadInfo info : new SoundLoadInfo[]{ogg, wav, mp3, flac}) {
                assertTrue(info.isNativeBuffer());
                assertTrue(info.getChannels() == 1 || info.getChannels() == 2);
                assertTrue(info.getSampleRate() >= 8000);
                assertTrue(info.getBuffer().remaining() > 0);
                assertEquals(0, info.getBuffer().remaining() % info.getChannels(), "whole frames");
                // Not silent
                boolean sound = false;
                while (info.getBuffer().hasRemaining())
                    sound |= Math.abs(info.getBuffer().get()) > 100;
                info.getBuffer().rewind();
                assertTrue(sound);
            }

            // Lossy encoders add padding: a small difference is accepted
            double reference = seconds(wav);
            for (SoundLoadInfo info : new SoundLoadInfo[]{ogg, mp3, flac})
                assertEquals(reference, seconds(info), 0.15);
        } finally {
            ogg.release();
            wav.release();
            mp3.release();
            flac.release();
        }
    }

    @Test
    void everyDecoderCanBeStopped() {
        String[][] cases = {{"ogg", "selectogg.ogg"}, {"wav", "selectwav.wav"}, {"mp3", "selectmp3.mp3"}, {"flac", "selectflac.flac"}};
        SoundDecoder[] decoders = {new OggDecoder(), new WavDecoder(), new Mp3Decoder(), new FlacDecoder()};

        for (int i = 0; i < decoders.length; ++i) {
            AtomicInteger checks = new AtomicInteger();
            SoundDecoder decoder = decoders[i];
            String file = FOLDER + cases[i][1];

            assertThrows(CancellationException.class, () -> decoder.decode(file, () -> {
                if (checks.incrementAndGet() == 2)
                    throw new CancellationException();
            }), cases[i][0]);
            assertEquals(2, checks.get(), cases[i][0] + ": stopped at the check");
        }
    }

    @Test
    void missingOrInvalidFilesFail() {
        for (SoundDecoder decoder : new SoundDecoder[]{new OggDecoder(), new WavDecoder(), new Mp3Decoder(), new FlacDecoder()}) {
            assertThrows(ResourceException.class, () -> decoder.decode(FOLDER + "missing" + decoder.extensions()[0], () -> {}));
            // A file of another format
            assertThrows(ResourceException.class, () -> decoder.decode(FOLDER + "sounds.gaintree", () -> {}));
        }
    }

    @Test
    void loaderKnowsTheFormatsAndAddedDecoders() {
        SoundLoader loader = new SoundLoader();
        for (String file : new String[]{"a.ogg", "a.wav", "a.mp3", "a.flac", "a.OGG"})
            assertEquals("a", loader.filterFile("sounds/" + file), file);
        assertNull(loader.filterFile("sounds/a.txt"));
        assertNull(loader.filterFile("sounds/a.opus"));

        loader.addDecoder(new SoundDecoder() {
            @Override
            public String[] extensions() {
                return new String[]{".opus"};
            }

            @Override
            public SoundLoadInfo decode(String path, Runnable checkCancelled) {
                return null;
            }
        });
        assertEquals("a", loader.filterFile("sounds/a.opus"));
    }
}
