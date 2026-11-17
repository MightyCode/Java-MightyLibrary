package io.github.mightycode.mightylibdemo.resources;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.source.SourceEntry;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The layered pictures of the demo: one per folder of resources/layered, its layers the png files of the folder sorted
 * by name (1_body.png under 2_clothes.png), found without description.
 */
public class LayeredTextureLoader extends ResourceLoader {
    public static final String FOLDER = "layered";
    private static final String EXTENSION = ".png";

    @Override
    public Class<? extends DataType> getType() {
        return LayeredTextureData.class;
    }

    @Override
    public String getResourceNameType() {
        return "LayeredTextureData";
    }

    @Override
    public void create(Map<String, DataType> data) {
        // The folders and their layers in every source of the resources, sorted by name
        for (SourceEntry folder : resources().sources().list(FOLDER)) {
            if (!folder.isFolder())
                continue;

            List<String> layers = new ArrayList<>();
            for (SourceEntry file : resources().sources().list(folder.path())) {
                if (!file.isFolder() && file.name().toLowerCase(Locale.ROOT).endsWith(EXTENSION))
                    layers.add(file.path());
            }
            if (layers.isEmpty())
                continue;

            data.put(folder.name(), new LayeredTextureData(folder.name(), layers));
        }
    }

    @Override
    public DataType createFallback() {
        return new FallbackLayeredTextureData();
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
    }

    @Override
    public String filterFile(String path) {
        return null;
    }

    // Each layer is read then drawn over the previous ones, on a picture of the size of the biggest layer
    @Override
    protected void initWithFile(DataType dataType) {
        LayeredTextureData data = (LayeredTextureData) dataType;
        List<BufferedImage> layers = new ArrayList<>();
        int width = 0, height = 0;
        for (int i = 0; i < data.sourceCount(); ++i) {
            BufferedImage layer = read(data.sourcePath(i));
            layers.add(layer);
            width = Math.max(width, layer.getWidth());
            height = Math.max(height, layer.getHeight());
            data.markSourceRead(i);
        }

        BufferedImage composed = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = composed.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcOver);
            for (BufferedImage layer : layers)
                graphics.drawImage(layer, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        data.setComposed(composed);
    }

    private BufferedImage read(String path) {
        try (InputStream input = resources().sources().open(path)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null)
                throw new ResourceException("not an image: " + path);
            return image;
        } catch (IOException e) {
            throw new ResourceException("can't read the layer " + path, e);
        }
    }
}
