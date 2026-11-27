package io.github.mightycode.mightylib.test;

import java.util.Locale;

/**
 * What two images may differ by: a pixel is different when one of its channels R, G or B differs by more than the
 * channel difference; the comparison fails when more pixels than allowed are different (a number or a part of the
 * image). Immutable: each with* gives a new tolerance.
 * <p>
 * {@link #STRICT} (2 levels, no pixel) is the default. A tolerance wider than STRICT needs its reason
 * ({@link #withReason}): the comparison refuses it otherwise.
 */
public final class ImageTolerance {
    public static final int STRICT_CHANNEL_DIFFERENCE = 2;

    /** ±2 levels per channel, no different pixel. */
    public static final ImageTolerance STRICT = new ImageTolerance(STRICT_CHANNEL_DIFFERENCE, 0, -1, null);

    private final int channelDifference;
    // A number of pixels, or -1 when the part is given
    private final int differentPixels;
    // A part of the pixels in [0, 1], or -1 when the number is given
    private final double differentFraction;
    private final String reason;

    private ImageTolerance(int channelDifference, int differentPixels, double differentFraction, String reason) {
        this.channelDifference = channelDifference;
        this.differentPixels = differentPixels;
        this.differentFraction = differentFraction;
        this.reason = reason;
    }

    /** A pixel is different when one of its channels differs by more than this (0 to 255). */
    public ImageTolerance withChannelDifference(int levels) {
        if (levels < 0 || levels > 255)
            throw new IllegalArgumentException("Channel difference out of [0, 255]: " + levels);
        return new ImageTolerance(levels, differentPixels, differentFraction, reason);
    }

    /** At most this number of different pixels (replaces a part). */
    public ImageTolerance withDifferentPixels(int pixels) {
        if (pixels < 0)
            throw new IllegalArgumentException("Negative number of pixels: " + pixels);
        return new ImageTolerance(channelDifference, pixels, -1, reason);
    }

    /** At most this part of the pixels different, in [0, 1] (replaces a number). */
    public ImageTolerance withDifferentFraction(double fraction) {
        if (!(fraction >= 0 && fraction <= 1))
            throw new IllegalArgumentException("Part of the pixels out of [0, 1]: " + fraction);
        return new ImageTolerance(channelDifference, -1, fraction, reason);
    }

    /** Why this test allows more than STRICT (driver, rounding...): required to widen it. */
    public ImageTolerance withReason(String why) {
        if (why == null || why.trim().isEmpty())
            throw new IllegalArgumentException("A reason is a text");
        return new ImageTolerance(channelDifference, differentPixels, differentFraction, why.trim());
    }

    public int channelDifference() {
        return channelDifference;
    }

    /** The different pixels allowed in an image of this many pixels. */
    public int allowedPixels(int totalPixels) {
        if (differentPixels >= 0)
            return differentPixels;
        return (int) Math.floor(differentFraction * totalPixels);
    }

    /** The reason given, null for none. */
    public String reason() {
        return reason;
    }

    /** More levels per channel or more pixels than STRICT. */
    public boolean isWiderThanStrict() {
        return channelDifference > STRICT_CHANNEL_DIFFERENCE || differentPixels > 0 || differentFraction > 0;
    }

    // The comparison refuses a widened tolerance without its reason
    void requireReason() {
        if (isWiderThanStrict() && reason == null)
            throw new IllegalArgumentException("The tolerance " + this + " is wider than STRICT: give its reason"
                    + " (withReason)");
    }

    @Override
    public String toString() {
        String pixels = differentPixels >= 0 ? differentPixels + " pixels"
                : String.format(Locale.ROOT, "%.4f%% of the pixels", differentFraction * 100);
        return "+-" + channelDifference + " levels, " + pixels + (reason == null ? "" : " (" + reason + ")");
    }
}
