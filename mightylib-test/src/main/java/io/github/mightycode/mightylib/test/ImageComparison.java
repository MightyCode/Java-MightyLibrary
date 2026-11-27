package io.github.mightycode.mightylib.test;

import java.util.Locale;

/**
 * The comparison of an image to the expected one, channel by channel (R, G, B) within an {@link ImageTolerance}:
 * the different pixels, their part, the largest difference, the box around them, and an image of the differences
 * (the expected image darkened, the different pixels in red). Images of different sizes never match.
 */
public final class ImageComparison {
    private static final int DIFFERENT = 0xFF0000;

    private final CapturedImage expected;
    private final CapturedImage actual;
    private final ImageTolerance tolerance;
    private final boolean sameSize;
    private final int differentPixels;
    private final int maximumDifference;
    // x, y, width, height of the different pixels, null for none
    private final int[] box;
    private final CapturedImage differenceImage;

    private ImageComparison(CapturedImage expected, CapturedImage actual, ImageTolerance tolerance, boolean sameSize,
                            int differentPixels, int maximumDifference, int[] box, CapturedImage differenceImage) {
        this.expected = expected;
        this.actual = actual;
        this.tolerance = tolerance;
        this.sameSize = sameSize;
        this.differentPixels = differentPixels;
        this.maximumDifference = maximumDifference;
        this.box = box;
        this.differenceImage = differenceImage;
    }

    /** Within STRICT. */
    public static ImageComparison compare(CapturedImage expected, CapturedImage actual) {
        return compare(expected, actual, ImageTolerance.STRICT);
    }

    /** @throws IllegalArgumentException if the tolerance is wider than STRICT without its reason */
    public static ImageComparison compare(CapturedImage expected, CapturedImage actual, ImageTolerance tolerance) {
        tolerance.requireReason();
        if (expected.width() != actual.width() || expected.height() != actual.height())
            return new ImageComparison(expected, actual, tolerance, false, -1, -1, null, null);

        int width = expected.width(), height = expected.height();
        int[] difference = new int[width * height];
        int different = 0, maximum = 0;
        int left = width, top = height, right = -1, bottom = -1;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int a = expected.rgb(x, y), b = actual.rgb(x, y);
                int largest = 0;
                for (int shift = 0; shift <= 16; shift += 8)
                    largest = Math.max(largest, Math.abs((a >> shift & 0xFF) - (b >> shift & 0xFF)));
                maximum = Math.max(maximum, largest);

                if (largest > tolerance.channelDifference()) {
                    ++different;
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                    difference[x + y * width] = DIFFERENT;
                } else {
                    difference[x + y * width] = darken(a);
                }
            }
        }

        int[] box = different == 0 ? null : new int[]{left, top, right - left + 1, bottom - top + 1};
        return new ImageComparison(expected, actual, tolerance, true, different, maximum, box,
                CapturedImage.of(width, height, difference));
    }

    // A third of each channel: the red of the differences stands out
    private static int darken(int rgb) {
        return (rgb >> 16 & 0xFF) / 3 << 16 | (rgb >> 8 & 0xFF) / 3 << 8 | (rgb & 0xFF) / 3;
    }

    /** Same size and no more different pixels than the tolerance allows. */
    public boolean matches() {
        return sameSize && differentPixels <= tolerance.allowedPixels(expected.width() * expected.height());
    }

    public boolean isSameSize() {
        return sameSize;
    }

    /** The pixels with a channel beyond the tolerance, -1 for images of different sizes. */
    public int differentPixels() {
        return differentPixels;
    }

    /** The part of the pixels that differ, in [0, 1] (1 for images of different sizes). */
    public double differentFraction() {
        if (!sameSize)
            return 1;
        return (double) differentPixels / (expected.width() * expected.height());
    }

    /** The largest difference of a channel, within the tolerance or not (-1 for images of different sizes). */
    public int maximumDifference() {
        return maximumDifference;
    }

    /** {x, y, width, height} around the different pixels (top left origin), null when none differs. */
    public int[] differenceBox() {
        return box == null ? null : box.clone();
    }

    /** The expected image darkened, the different pixels in red; null for images of different sizes. */
    public CapturedImage differenceImage() {
        return differenceImage;
    }

    public CapturedImage expected() {
        return expected;
    }

    public CapturedImage actual() {
        return actual;
    }

    public ImageTolerance tolerance() {
        return tolerance;
    }

    /** The figures of the comparison, for a message. */
    public String describe() {
        if (!sameSize)
            return "the image is " + actual.width() + "x" + actual.height() + ", " + expected.width() + "x"
                    + expected.height() + " expected";

        String text = String.format(Locale.ROOT, "%d different pixels (%.4f%%, %d allowed), largest difference %d,"
                        + " tolerance %s", differentPixels, differentFraction() * 100,
                tolerance.allowedPixels(expected.width() * expected.height()), maximumDifference, tolerance);
        if (box != null)
            text += ", differences in " + box[0] + "," + box[1] + " " + box[2] + "x" + box[3];
        return text;
    }

    /** @throws AssertionError with the figures if the images do not match */
    public void assertMatches(String what) {
        if (!matches())
            throw new AssertionError(what + ": " + describe());
    }

    @Override
    public String toString() {
        return "ImageComparison " + (matches() ? "matching" : "different") + ": " + describe();
    }
}
