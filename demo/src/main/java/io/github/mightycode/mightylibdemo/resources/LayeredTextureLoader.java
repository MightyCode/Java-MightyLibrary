package io.github.mightycode.mightylibdemo.resources;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
        File[] folders = new File(resources().folder() + FOLDER).listFiles(File::isDirectory);
        if (folders == null)
            return;

        for (File folder : folders) {
            File[] files = folder.listFiles((directory, name) -> name.toLowerCase().endsWith(EXTENSION));
            if (files == null || files.length == 0)
                continue;

            Arrays.sort(files, Comparator.comparing(File::getName));
            List<String> layers = new ArrayList<>();
            for (File file : files)
                layers.add(resources().folder() + FOLDER + "/" + folder.getName() + "/" + file.getName());

            data.put(folder.getName(), new LayeredTextureData(folder.getName(), layers));
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

    private static BufferedImage read(String path) {
        try {
            BufferedImage image = ImageIO.read(new File(path));
            if (image == null)
                throw new ResourceException("not an image: " + path);
            return image;
        } catch (IOException e) {
            throw new ResourceException("can't read the layer " + path, e);
        }
    }
}
