package io.github.mightycode.mightylib.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// The images of the tests without GL: PNG round trip, orientation, part
class CapturedImageTest {
    // 3x2: every pixel different, red at the top left
    private static CapturedImage sample() {
        return CapturedImage.of(3, 2, new int[]{
                0xFF0000, 0x00FF00, 0x0000FF,
                0x102030, 0x405060, 0xFFFFFF});
    }

    @Test
    void thePixelsAreReadFromTheTopLeft() {
        CapturedImage image = sample();
        assertEquals(3, image.width());
        assertEquals(2, image.height());
        assertEquals(0xFF0000, image.rgb(0, 0));
        assertEquals(0x0000FF, image.rgb(2, 0));
        assertEquals(0x102030, image.rgb(0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> image.rgb(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> image.rgb(0, -1));
    }

    @Test
    void theArrayIsCopiedAndTheAlphaDropped() {
        int[] pixels = {0xAA123456};
        CapturedImage image = CapturedImage.of(1, 1, pixels);
        pixels[0] = 0;
        assertEquals(0x123456, image.rgb(0, 0));
        assertThrows(IllegalArgumentException.class, () -> CapturedImage.of(2, 2, new int[3]));
        assertThrows(IllegalArgumentException.class, () -> CapturedImage.of(0, 1, new int[0]));
    }

    @Test
    void aPngWrittenIsReadTheSame(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("sub/sample.png");
        sample().writePng(file);
        CapturedImage read = CapturedImage.read(file);
        assertEquals(3, read.width());
        assertEquals(2, read.height());
        for (int y = 0; y < 2; ++y) {
            for (int x = 0; x < 3; ++x)
                assertEquals(sample().rgb(x, y), read.rgb(x, y), x + "," + y);
        }
    }

    @Test
    void theTopOfAPngIsTheTopOfTheImage() throws IOException {
        BufferedImage png = new BufferedImage(1, 2, BufferedImage.TYPE_INT_RGB);
        png.setRGB(0, 0, 0xFF0000);
        png.setRGB(0, 1, 0x0000FF);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(png, "png", bytes);

        CapturedImage read = CapturedImage.read(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(0xFF0000, read.rgb(0, 0));
        assertEquals(0x0000FF, read.rgb(0, 1));
    }

    @Test
    void aFileThatIsNotAnImageIsRefused(@TempDir Path folder) throws IOException {
        Path file = Files.write(folder.resolve("text.png"), new byte[]{1, 2, 3});
        IOException e = assertThrows(IOException.class, () -> CapturedImage.read(file));
        assertTrue(e.getMessage().contains("text.png"), e.getMessage());
    }

    @Test
    void aPartKeepsItsPixels() {
        CapturedImage part = sample().crop(1, 0, 2, 2);
        assertEquals(2, part.width());
        assertEquals(2, part.height());
        assertEquals(0x00FF00, part.rgb(0, 0));
        assertEquals(0xFFFFFF, part.rgb(1, 1));
        assertThrows(IllegalArgumentException.class, () -> sample().crop(2, 0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> sample().crop(0, 0, 0, 1));
    }
}
