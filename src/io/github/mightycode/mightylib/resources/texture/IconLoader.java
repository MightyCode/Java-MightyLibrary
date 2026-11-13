package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class IconLoader extends ResourceLoader {

    @Override
    public Class<? extends DataType> getType() {
        return Icon.class;
    }

    @Override
    public String getResourceNameType() {
        return "Icon";
    }

    @Override
    public void create(Map<String, DataType> data){
        exploreResourcesFile(data, resources().folder());
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new Icon(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending != null && ending.equals(".ico")) return getFileName(path);

        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof Icon))
            return;

        Icon icon = (Icon) dataType;

        BufferedImage image;
        try (FileInputStream input = new FileInputStream(icon.path())) {
            image = ImageIO.read(input);
        } catch (IOException e) {
            throw new ResourceException("can't read the icon " + icon.path(), e);
        }

        // No reader for this format
        if (image == null)
            throw new ResourceException("unsupported image format: " + icon.path());

        icon.setBufferedImage(image);
    }
}
