package MightyLibrary.mightylib.resources.texture;

import java.awt.image.BufferedImage;

/**
 * 1x1 texture of a single color, created in memory (no file): default maps of a material without texture.
 */
public class ColorTextureData extends TextureData {
    private final int argb;

    public ColorTextureData(int argb) {
        super(NameOf(argb), null);
        this.argb = argb;
    }

    public static String NameOf(int argb) {
        return String.format("color/%08x", argb);
    }

    public static int ToArgb(float r, float g, float b, float a) {
        return (ToByte(a) << 24) | (ToByte(r) << 16) | (ToByte(g) << 8) | ToByte(b);
    }

    private static int ToByte(float value) {
        return Math.round(Math.max(0f, Math.min(1f, value)) * 255f);
    }

    @Override
    protected boolean internLoad() {
        // Rebuilt at each load: the image is dropped once converted
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, argb);
        setBufferedImage(image);

        return super.internLoad();
    }
}
