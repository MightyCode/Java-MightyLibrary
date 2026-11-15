package io.github.mightycode.mightylibdemo.resources;

import io.github.mightycode.mightylib.resources.Resources;

import java.awt.image.BufferedImage;
import java.util.Collections;

/**
 * Fallback of the layered pictures: blue stripes drawn by the code (no layer to read), with its own texture.
 * Shows that a type of the game gets its own fallback element instead of the checkerboard of the textures.
 */
public class FallbackLayeredTextureData extends LayeredTextureData {
    private static final int SIZE = 32;
    private static final int STRIPE = 4;

    public FallbackLayeredTextureData() {
        super(Resources.FALLBACK_NAME, Collections.emptyList());
    }

    @Override
    protected boolean internLoad() {
        BufferedImage stripes = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; ++y) {
            for (int x = 0; x < SIZE; ++x)
                stripes.setRGB(x, y, ((x + y) / STRIPE) % 2 == 0 ? 0xFF2050D0 : 0xFF90B0F0);
        }

        setComposed(stripes);
        return super.internLoad();
    }
}
