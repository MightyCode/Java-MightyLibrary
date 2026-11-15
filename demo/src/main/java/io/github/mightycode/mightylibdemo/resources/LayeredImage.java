package io.github.mightycode.mightylibdemo.resources;

import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.resources.texture.TextureData;

import java.awt.image.BufferedImage;

/**
 * The picture of a LayeredTextureData, built by it outside Resources (no file, no pipeline): each build is a new
 * version, sent again by its texture.
 */
final class LayeredImage extends TextureData {
    LayeredImage(String name) {
        super(TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT, name, null);
        setDefaultAspectTexture(TextureParameters.PIXEL_ART_PARAMETERS);
    }

    boolean build(BufferedImage picture) {
        unload();
        setBufferedImage(picture);
        setPreloaded();
        load();
        return isLoaded();
    }

    void release() {
        unload();
    }
}
