package io.github.mightycode.mightylibdemo.resources;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.MultiSourceDataType;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.Texture;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A picture made of layers drawn over each other (resources/layered/name/*.png, sorted by name): a data read from
 * several files (MultiSourceDataType). The picture is a TextureData owned by this data, outside Resources (as the atlas
 * of a TileMap); its GL element is a texture showing the fallback of this type (blue stripes) while it can't be used.
 */
public class LayeredTextureData extends MultiSourceDataType {
    private static final String ELEMENT_PREFIX = "layered:";

    private final LayeredImage image;
    // Built by the loader from the layers (worker), turned into pixels by internLoad
    private volatile BufferedImage composed;

    public LayeredTextureData(String name, List<String> layers) {
        super(TYPE_SET_UP.THREAD_CONTEXT, name, layers);
        image = new LayeredImage(ELEMENT_PREFIX + name);
    }

    void setComposed(BufferedImage composed) {
        this.composed = composed;
    }

    @Override
    protected boolean internLoad() {
        BufferedImage picture = composed;
        if (picture == null)
            return false;

        composed = null;
        image.attachTo(resources());
        return image.build(picture);
    }

    @Override
    protected void internCancelPreload() {
        composed = null;
    }

    @Override
    protected void internUnload() {
        image.release();
    }

    // The picture built from the layers (tests)
    LayeredImage image() {
        return image;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    // The texture of the picture, shared under the name of the picture, sent once this data is loaded
    @Override
    public Class<? extends GLElement> glElementType() {
        return Texture.class;
    }

    @Override
    public String glElementName() {
        return image.getDataName();
    }

    @Override
    public GLElement createGLElement() {
        return new LayeredTexture(resources(), image, this);
    }

    // The texture of the picture: while it can't be used, the fallback of LayeredTextureData instead of the checkerboard
    static final class LayeredTexture extends Texture {
        LayeredTexture(Resources resources, LayeredImage image, LayeredTextureData owner) {
            super(resources, image);
            // A failed data must not keep a loading screen waiting
            addResourceDependency(owner);
        }

        @Override
        protected Class<? extends DataType> fallbackDataType() {
            return LayeredTextureData.class;
        }
    }
}
