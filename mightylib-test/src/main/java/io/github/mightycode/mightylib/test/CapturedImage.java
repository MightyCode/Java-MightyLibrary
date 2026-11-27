package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.utils.KitGraphicsAccess;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An image of a test: its pixels in RGB (no alpha: the alpha of a window has no defined value), rows from the top,
 * as in a PNG. Immutable. Given by SceneRunner.capture, read from a PNG (reference images), or built by a test.
 */
public final class CapturedImage {
    private final int width;
    private final int height;
    // 0xRRGGBB, rows from the top
    private final int[] rgb;

    private CapturedImage(int width, int height, int[] rgb) {
        this.width = width;
        this.height = height;
        this.rgb = rgb;
    }

    /**
     * An image of these pixels (0xRRGGBB, the high byte ignored), rows from the top; the array is copied.
     * @throws IllegalArgumentException if the size is not positive or the array is not width * height long
     */
    public static CapturedImage of(int width, int height, int[] rgb) {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Image of size " + width + "x" + height);
        if (rgb.length != width * height)
            throw new IllegalArgumentException("Image " + width + "x" + height + " of " + rgb.length + " pixels");

        int[] copy = new int[rgb.length];
        for (int i = 0; i < rgb.length; ++i)
            copy[i] = rgb[i] & 0xFFFFFF;
        return new CapturedImage(width, height, copy);
    }

    // The final image of a window (RGBA, rows from the top): the alpha is dropped
    static CapturedImage of(KitGraphicsAccess.WindowImage image) {
        byte[] rgba = image.rgba();
        int[] rgb = new int[image.width() * image.height()];
        for (int i = 0; i < rgb.length; ++i)
            rgb[i] = (rgba[i * 4] & 0xFF) << 16 | (rgba[i * 4 + 1] & 0xFF) << 8 | rgba[i * 4 + 2] & 0xFF;
        return new CapturedImage(image.width(), image.height(), rgb);
    }

    /** @throws IOException if the file is not an image ImageIO reads */
    public static CapturedImage read(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            return read(input, file.toString());
        }
    }

    /** The stream is read to its end, not closed. */
    public static CapturedImage read(InputStream input) throws IOException {
        return read(input, "the stream");
    }

    private static CapturedImage read(InputStream input, String name) throws IOException {
        BufferedImage image = ImageIO.read(input);
        if (image == null)
            throw new IOException(name + " is not an image");

        int[] rgb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        for (int i = 0; i < rgb.length; ++i)
            rgb[i] &= 0xFFFFFF;
        return new CapturedImage(image.getWidth(), image.getHeight(), rgb);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** 0xRRGGBB of a pixel, (0, 0) at the top left. */
    public int rgb(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height)
            throw new IndexOutOfBoundsException("Pixel " + x + "," + y + " outside the image " + width + "x" + height);
        return rgb[x + y * width];
    }

    /**
     * A part of the image, (x, y) its top left corner.
     * @throws IllegalArgumentException if the part is empty or leaves the image
     */
    public CapturedImage crop(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0 || x < 0 || y < 0 || x + width > this.width || y + height > this.height)
            throw new IllegalArgumentException("Part " + x + "," + y + " " + width + "x" + height + " outside the image "
                    + this.width + "x" + this.height);

        int[] part = new int[width * height];
        for (int row = 0; row < height; ++row)
            System.arraycopy(rgb, x + (y + row) * this.width, part, row * width, width);
        return new CapturedImage(width, height, part);
    }

    /** Writes the image as a PNG, its folders created. */
    public void writePng(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null)
            Files.createDirectories(parent);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, rgb, 0, width);
        if (!ImageIO.write(image, "png", file.toFile()))
            throw new IOException("No PNG writer");
    }

    @Override
    public String toString() {
        return "CapturedImage " + width + "x" + height;
    }
}
