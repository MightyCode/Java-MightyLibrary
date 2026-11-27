package io.github.mightycode.mightylib.test;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

// The comparison of two images without GL: channel difference, number and part of pixels, sizes, box, difference image
class ImageComparisonTest {
    private static final int GRAY = 0x606060;

    private static int[] filled(int width, int height, int rgb) {
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, rgb);
        return pixels;
    }

    private static CapturedImage gray(int width, int height) {
        return CapturedImage.of(width, height, filled(width, height, GRAY));
    }

    // The gray image with these pixels (x, y pairs) changed to rgb
    private static CapturedImage changed(int width, int height, int rgb, int... places) {
        int[] pixels = filled(width, height, GRAY);
        for (int i = 0; i < places.length; i += 2)
            pixels[places[i] + places[i + 1] * width] = rgb;
        return CapturedImage.of(width, height, pixels);
    }

    @Test
    void identicalImagesMatch() {
        ImageComparison comparison = ImageComparison.compare(gray(8, 4), gray(8, 4));
        assertTrue(comparison.matches());
        assertEquals(0, comparison.differentPixels());
        assertEquals(0, comparison.maximumDifference());
        assertNull(comparison.differenceBox());
        assertEquals(0, comparison.differentFraction());
    }

    @Test
    void strictAllowsTwoLevelsAndNoPixelBeyond() {
        // +2 on the green channel of one pixel: within STRICT
        ImageComparison two = ImageComparison.compare(gray(8, 4), changed(8, 4, 0x606260, 3, 1));
        assertTrue(two.matches(), two.describe());
        assertEquals(0, two.differentPixels());
        assertEquals(2, two.maximumDifference());

        // +3 on the blue channel: one different pixel, refused
        ImageComparison three = ImageComparison.compare(gray(8, 4), changed(8, 4, 0x606063, 3, 1));
        assertFalse(three.matches());
        assertEquals(1, three.differentPixels());
        assertEquals(3, three.maximumDifference());
        AssertionError e = assertThrows(AssertionError.class, () -> three.assertMatches("one pixel"));
        assertTrue(e.getMessage().contains("1 different pixels"), e.getMessage());

        // A channel going down counts the same
        assertFalse(ImageComparison.compare(gray(8, 4), changed(8, 4, 0x5D6060, 0, 0)).matches());
    }

    @Test
    void theAllowedPixelsAreANumberOrAPart() {
        CapturedImage threePixels = changed(10, 10, 0xFFFFFF, 1, 1, 2, 2, 3, 3);
        ImageTolerance two = ImageTolerance.STRICT.withDifferentPixels(2).withReason("test");
        ImageTolerance three = ImageTolerance.STRICT.withDifferentPixels(3).withReason("test");
        assertFalse(ImageComparison.compare(gray(10, 10), threePixels, two).matches());
        assertTrue(ImageComparison.compare(gray(10, 10), threePixels, three).matches());

        // 3 of 100 pixels: 3 %
        ImageTolerance part = ImageTolerance.STRICT.withDifferentFraction(0.03).withReason("test");
        ImageComparison comparison = ImageComparison.compare(gray(10, 10), threePixels, part);
        assertTrue(comparison.matches());
        assertEquals(0.03, comparison.differentFraction(), 1e-9);
        assertFalse(ImageComparison.compare(gray(10, 10), threePixels,
                ImageTolerance.STRICT.withDifferentFraction(0.029).withReason("test")).matches());

        // The last given wins
        assertEquals(5, ImageTolerance.STRICT.withDifferentFraction(0.5).withDifferentPixels(5).allowedPixels(100));
        assertEquals(50, ImageTolerance.STRICT.withDifferentPixels(5).withDifferentFraction(0.5).allowedPixels(100));
    }

    @Test
    void imagesOfDifferentSizesNeverMatch() {
        ImageComparison comparison = ImageComparison.compare(gray(8, 4),
                gray(4, 8), ImageTolerance.STRICT.withDifferentFraction(1).withReason("anything"));
        assertFalse(comparison.matches());
        assertFalse(comparison.isSameSize());
        assertNull(comparison.differenceImage());
        assertTrue(comparison.describe().contains("4x8") && comparison.describe().contains("8x4"),
                comparison.describe());
    }

    @Test
    void theBoxSurroundsTheDifferentPixels() {
        ImageComparison comparison = ImageComparison.compare(gray(16, 8), changed(16, 8, 0, 3, 2, 9, 5, 4, 6));
        assertArrayEquals(new int[]{3, 2, 7, 5}, comparison.differenceBox());
        assertTrue(comparison.describe().contains("3,2 7x5"), comparison.describe());

        // A copy: the comparison is not changed
        comparison.differenceBox()[0] = 99;
        assertEquals(3, comparison.differenceBox()[0]);
    }

    @Test
    void theDifferenceImageShowsTheDifferentPixelsInRed() {
        ImageComparison comparison = ImageComparison.compare(gray(6, 3), changed(6, 3, 0x00FF00, 4, 1));
        CapturedImage difference = comparison.differenceImage();
        assertEquals(6, difference.width());
        assertEquals(3, difference.height());
        assertEquals(0xFF0000, difference.rgb(4, 1));
        // The expected image darkened elsewhere
        assertEquals(0x202020, difference.rgb(0, 0));
        assertEquals(0x202020, difference.rgb(5, 2));
    }

    @Test
    void aToleranceWiderThanStrictNeedsItsReason() {
        ImageTolerance wider = ImageTolerance.STRICT.withChannelDifference(3);
        assertTrue(wider.isWiderThanStrict());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ImageComparison.compare(gray(2, 2), gray(2, 2), wider));
        assertTrue(e.getMessage().contains("withReason"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ImageComparison.compare(gray(2, 2), gray(2, 2),
                ImageTolerance.STRICT.withDifferentPixels(1)));

        // With its reason, or narrower than STRICT: accepted
        assertTrue(ImageComparison.compare(gray(2, 2), gray(2, 2), wider.withReason("llvmpipe rounds")).matches());
        assertTrue(ImageComparison.compare(gray(2, 2), gray(2, 2), ImageTolerance.STRICT.withChannelDifference(0))
                .matches());
        assertFalse(ImageTolerance.STRICT.isWiderThanStrict());
        assertThrows(IllegalArgumentException.class, () -> ImageTolerance.STRICT.withReason(" "));
        assertThrows(IllegalArgumentException.class, () -> ImageTolerance.STRICT.withDifferentFraction(1.5));
        assertThrows(IllegalArgumentException.class, () -> ImageTolerance.STRICT.withChannelDifference(256));
    }
}
