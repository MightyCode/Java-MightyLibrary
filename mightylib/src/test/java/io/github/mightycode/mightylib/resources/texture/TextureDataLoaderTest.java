package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.TestFiles;
import io.github.mightycode.mightylib.resources.LoaderTestAccess;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.source.ClasspathSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The texture files are decoded by stb_image (straight to RGBA), by ImageIO when stb cannot read them.
 * No OpenGL: only the CPU data is checked.
 */
class TextureDataLoaderTest {
    @TempDir
    Path folder;

    // Reads the files of the folder
    private TextureDataLoader loader;

    @BeforeEach
    void setUp() {
        loader = LoaderTestAccess.attached(new TextureDataLoader(), folder);
    }

    // Logical path of a file of the folder
    private String logical(File file) {
        return folder.relativize(file.toPath()).toString().replace(File.separatorChar, '/');
    }

    private TextureData read(File file) {
        return read(loader, logical(file));
    }

    private static TextureData read(TextureDataLoader loader, String path) {
        TextureData data = new TextureData("test", path);
        loader.initWithFile(data);
        assertTrue(data.internLoad());
        return data;
    }

    private static BufferedImage gradient(int width, int height, int type) {
        BufferedImage image = new BufferedImage(width, height, type);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int r = x * 255 / Math.max(1, width - 1);
                int g = y * 255 / Math.max(1, height - 1);
                int b = (x + y) * 255 / Math.max(1, width + height - 2);
                int a = type == BufferedImage.TYPE_INT_ARGB ? 255 - (x * 200 / Math.max(1, width - 1)) : 255;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return image;
    }

    private File write(BufferedImage image, String format, String name) throws IOException {
        File file = folder.resolve(name).toFile();
        assertTrue(ImageIO.write(image, format, file), "no ImageIO writer for " + format);
        return file;
    }

    @Test
    void pngKeepsEveryPixelAndAlpha() throws IOException {
        BufferedImage image = gradient(37, 23, BufferedImage.TYPE_INT_ARGB);
        TextureData data = read(write(image, "png", "alpha.png"));

        assertEquals(37, data.getWidth());
        assertEquals(23, data.getHeight());
        for (int y = 0; y < 23; ++y) {
            for (int x = 0; x < 37; ++x)
                assertEquals(image.getRGB(x, y), data.getPixel(x, y), "pixel " + x + "," + y);
        }
    }

    @Test
    void oneByOnePng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x80FF4020);
        TextureData data = read(write(image, "png", "one.png"));

        assertEquals(1, data.getWidth());
        assertEquals(1, data.getHeight());
        assertEquals(0x80FF4020, data.getPixel(0, 0));
    }

    @Test
    void opaqueRgbPngHasFullAlpha() throws IOException {
        BufferedImage image = gradient(16, 16, BufferedImage.TYPE_INT_RGB);
        TextureData data = read(write(image, "png", "rgb.png"));

        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x)
                assertEquals(image.getRGB(x, y), data.getPixel(x, y), "pixel " + x + "," + y);
        }
    }

    // The two decoders of a lossy format differ by a few levels
    @Test
    void jpegIsCloseToImageIO() throws IOException {
        File file = write(gradient(64, 48, BufferedImage.TYPE_INT_RGB), "jpg", "gradient.jpg");
        BufferedImage reference;
        try (FileInputStream input = new FileInputStream(file)) {
            reference = ImageIO.read(input);
        }

        TextureData data = read(file);
        assertEquals(64, data.getWidth());
        assertEquals(48, data.getHeight());

        long total = 0;
        for (int y = 0; y < 48; ++y) {
            for (int x = 0; x < 64; ++x) {
                int expected = reference.getRGB(x, y);
                int actual = data.getPixel(x, y);
                assertEquals(0xFF, actual >>> 24);
                for (int shift = 0; shift <= 16; shift += 8) {
                    int difference = Math.abs(((expected >> shift) & 0xFF) - ((actual >> shift) & 0xFF));
                    assertTrue(difference <= 12, "pixel " + x + "," + y + " differs by " + difference);
                    total += difference;
                }
            }
        }

        assertTrue(total / (64.0 * 48 * 3) < 2.0, "mean difference " + total / (64.0 * 48 * 3));
    }

    // WBMP is known by ImageIO only: the fallback reads it
    @Test
    void formatUnknownToStbIsReadByImageIO() throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_BINARY);
        for (int i = 0; i < 8; ++i)
            image.setRGB(i, i, 0xFFFFFFFF);

        TextureData data = read(write(image, "wbmp", "binary.wbmp"));

        assertEquals(8, data.getWidth());
        assertEquals(8, data.getHeight());
        assertEquals(0xFFFFFFFF, data.getPixel(3, 3));
        assertEquals(0xFF000000, data.getPixel(3, 4));
    }

    @Test
    void notAnImageFails() throws IOException {
        File file = folder.resolve("text.png").toFile();
        Files.write(file.toPath(), "this is not an image".getBytes());

        assertThrows(ResourceException.class, () -> loader.initWithFile(new TextureData("test", logical(file))));
    }

    @Test
    void emptyAndMissingFilesFail() throws IOException {
        File empty = folder.resolve("empty.png").toFile();
        Files.write(empty.toPath(), new byte[0]);

        assertThrows(ResourceException.class, () -> loader.initWithFile(new TextureData("test", logical(empty))));
        assertThrows(ResourceException.class, () -> loader.initWithFile(new TextureData("test", "missing.png")));
        // Out of the sources
        assertThrows(ResourceException.class, () -> loader.initWithFile(new TextureData("test", "../a.png")));
    }

    @Test
    void unloadFreesTheDataAndTheDataCanBeReadAgain() throws IOException {
        File file = write(gradient(20, 10, BufferedImage.TYPE_INT_ARGB), "png", "again.png");
        TextureData data = read(file);
        int pixel = data.getPixel(5, 5);

        data.internUnload();
        assertNull(data.getData());
        assertEquals(-1, data.getWidth());
        // A second unload frees nothing twice
        data.internUnload();

        loader.initWithFile(data);
        assertTrue(data.internLoad());
        assertEquals(pixel, data.getPixel(5, 5));
    }

    @Test
    void cancelledPreloadFreesTheDecodedPixels() throws IOException {
        File file = write(gradient(20, 10, BufferedImage.TYPE_INT_ARGB), "png", "cancel.png");
        TextureData data = new TextureData("test", logical(file));
        loader.initWithFile(data);

        data.internCancelPreload();
        data.internCancelPreload();
        assertNull(data.getData());
    }

    @Test
    void textureOfTheRepositoryIsDecodedLikeImageIO() throws IOException {
        File file = TestFiles.path("textures/world/container2.png").toFile();

        BufferedImage reference;
        try (FileInputStream input = new FileInputStream(file)) {
            reference = ImageIO.read(input);
        }

        // Read from the classpath (the test resources), as from the jar of a plugin
        TextureDataLoader classpath = LoaderTestAccess.attached(new TextureDataLoader(),
                ResourceSources.of(new ClasspathSource(getClass().getClassLoader(), "textures")));
        TextureData data = read(classpath, "world/container2.png");
        assertEquals(reference.getWidth(), data.getWidth());
        assertEquals(reference.getHeight(), data.getHeight());
        for (int y = 0; y < reference.getHeight(); y += 7) {
            for (int x = 0; x < reference.getWidth(); x += 5)
                assertEquals(reference.getRGB(x, y), data.getPixel(x, y), "pixel " + x + "," + y);
        }
    }
}
