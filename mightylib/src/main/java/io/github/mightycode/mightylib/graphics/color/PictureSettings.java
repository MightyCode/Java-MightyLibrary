package io.github.mightycode.mightylib.graphics.color;

import java.util.Locale;

/**
 * Settings of the final pass of a view, applied to the encoded (sRGB) values: contrast (x - 0.5) * contrast + 0.5,
 * then brightness x * brightness, then gamma pow(x, 1 / gamma). DEFAULT (1, 1, 1) changes nothing. Each value is in
 * [MIN, MAX]. Immutable.
 */
public final class PictureSettings {
    public static final float MIN = 0.25f;
    public static final float MAX = 4f;
    public static final PictureSettings DEFAULT = new PictureSettings(1f, 1f, 1f);

    private final float gamma;
    private final float brightness;
    private final float contrast;

    private PictureSettings(float gamma, float brightness, float contrast) {
        this.gamma = gamma;
        this.brightness = brightness;
        this.contrast = contrast;
    }

    /**
     * @throws IllegalArgumentException if a value is out of [MIN, MAX] (or not a number)
     */
    public static PictureSettings of(float gamma, float brightness, float contrast) {
        Check("gamma", gamma);
        Check("brightness", brightness);
        Check("contrast", contrast);
        return new PictureSettings(gamma, brightness, contrast);
    }

    public static boolean IsValid(float value) {
        return value >= MIN && value <= MAX;
    }

    private static void Check(String name, float value) {
        if (!IsValid(value))
            throw new IllegalArgumentException("The " + name + " of the picture is in [" + MIN + ", " + MAX + "], not "
                    + value);
    }

    public float gamma() {
        return gamma;
    }

    public float brightness() {
        return brightness;
    }

    public float contrast() {
        return contrast;
    }

    public PictureSettings withGamma(float gamma) {
        return of(gamma, brightness, contrast);
    }

    public PictureSettings withBrightness(float brightness) {
        return of(gamma, brightness, contrast);
    }

    public PictureSettings withContrast(float contrast) {
        return of(gamma, brightness, contrast);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof PictureSettings))
            return false;

        PictureSettings settings = (PictureSettings) other;
        return Float.compare(gamma, settings.gamma) == 0 && Float.compare(brightness, settings.brightness) == 0
                && Float.compare(contrast, settings.contrast) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(gamma);
        result = 31 * result + Float.hashCode(brightness);
        return 31 * result + Float.hashCode(contrast);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "gamma %.2f, brightness %.2f, contrast %.2f", gamma, brightness, contrast);
    }
}
