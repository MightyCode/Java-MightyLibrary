package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The FLAC decoder is checked by the MD5 of the samples that every encoder writes in the STREAMINFO block:
 * a wrong prediction, residual or stereo decorrelation changes it.
 */
class FlacStreamTest {
    private static final String FILE = "resources/sounds/selectflac.flac";

    // MD5 of the samples as the FLAC format defines it: interleaved, little endian, ceil(bits / 8) bytes each
    static byte[] md5Of(FlacStream stream) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        int bytes = (stream.bitsPerSample + 7) / 8;
        int[][] frame = stream.createBuffers();

        int size;
        while ((size = stream.nextFrame(frame)) > 0) {
            for (int i = 0; i < size; ++i) {
                for (int channel = 0; channel < stream.channels; ++channel) {
                    int sample = frame[channel][i];
                    for (int b = 0; b < bytes; ++b)
                        md5.update((byte) (sample >> (8 * b)));
                }
            }
        }

        return md5.digest();
    }

    @Test
    void decodedSamplesMatchTheMd5OfTheFile() throws Exception {
        FlacStream stream = new FlacStream(Files.readAllBytes(Paths.get(FILE)));

        assertEquals(44100, stream.sampleRate);
        assertTrue(stream.totalSamples > 0);
        assertFalse(Arrays.equals(new byte[16], stream.md5), "the file must have an MD5 for this test to mean something");
        assertArrayEquals(stream.md5, md5Of(stream));
    }

    @Test
    void rejectsFilesThatAreNotFlac() {
        assertThrows(ResourceException.class, () -> new FlacStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}));
        assertThrows(ResourceException.class, () -> new FlacStream(Files.readAllBytes(Paths.get("resources/sounds/selectwav.wav"))));
    }

    @Test
    void truncatedFileFailsInsteadOfLooping() throws Exception {
        byte[] whole = Files.readAllBytes(Paths.get(FILE));
        byte[] cut = Arrays.copyOf(whole, whole.length - 60);

        assertThrows(ResourceException.class, () -> md5Of(new FlacStream(cut)));
    }

    @Test
    void bitReaderReadsBitsUnaryAndSignedValues() {
        // 1010 1100 | 0001 0000 | 1111 0000
        FlacBitReader reader = new FlacBitReader(new byte[]{(byte) 0xAC, 0x10, (byte) 0xF0});

        assertEquals(1, reader.readInt(1));
        assertEquals(1, reader.readUnary());          // "01"
        assertEquals(0b0110, reader.readInt(4));
        assertEquals(0, reader.readSigned(1));        // last bit of the first byte
        assertEquals(3, reader.readUnary());          // "0001"
        assertEquals(0, reader.readInt(4));
        assertEquals(-1, reader.readSigned(4));       // "1111"
        assertEquals(0, reader.readSigned(4));
        assertFalse(reader.hasBits(1));
    }
}
