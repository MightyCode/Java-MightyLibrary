package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.resources.Resources;

import java.awt.image.BufferedImage;

/**
 * Fallback of the textures: magenta and black checkerboard created in memory, it can't fail.
 * Used in place of a texture that failed, is still loading or has an unknown name.
 */
public class FallbackTextureData extends TextureData {
    private static final int SIZE = 16;
    private static final int CELL = 8;
    private static final int MAGENTA = 0xFFFF00FF;
    private static final int BLACK = 0xFF000000;

    public FallbackTextureData() {
        super(Resources.FALLBACK_NAME, null);
        defaultAspectTexture = TextureParameters.PIXEL_ART_PARAMETERS;
    }

    @Override
    protected boolean internLoad() {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; ++y) {
            for (int x = 0; x < SIZE; ++x)
                image.setRGB(x, y, ((x / CELL + y / CELL) % 2 == 0) ? MAGENTA : BLACK);
        }
        setBufferedImage(image);

        return super.internLoad();
    }
}
