package io.github.mightycode.mightylib.resources.sound;

import io.github.mightycode.mightylib.TestFiles;
import io.github.mightycode.mightylib.resources.LoaderTestAccess;
import io.github.mightycode.mightylib.resources.sound.decoder.*;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The memory estimate of a sound: before reading, the file size times the usual ratio of its format; once decoded,
 * the exact size of the samples. The test sounds last 30 ms, their headers outweigh the samples: the ratios
 * (meant for real sounds) cannot be checked on them. No OpenAL needed.
 */
class SoundDataEstimateTest {
    private static final Path FOLDER = TestFiles.path("sounds");

    private static void check(SoundDecoder decoder, String file, long ratio) throws IOException {
        SoundData sound = LoaderTestAccess.attached(new SoundData("estimate-" + file, file), FOLDER);
        assertEquals(Files.size(FOLDER.resolve(file)) * ratio, sound.estimatedCpuBytes(), file);

        SoundLoadInfo info = decoder.decode(ResourceSources.of(new FolderSource(FOLDER)), file, () -> {});
        try {
            sound.setSoundLoadInfo(info);
            assertEquals((long) info.getBuffer().capacity() * Short.BYTES, sound.estimatedCpuBytes(), file);
            assertEquals(0, sound.estimatedGpuBytes());
        } finally {
            info.release();
        }
    }

    @Test
    void guessFromTheFileThenDecodedSize() throws IOException {
        check(new OggDecoder(), "selectogg.ogg", 10);
        check(new WavDecoder(), "selectwav.wav", 1);
        check(new Mp3Decoder(), "selectmp3.mp3", 10);
        check(new FlacDecoder(), "selectflac.flac", 2);
    }
}
