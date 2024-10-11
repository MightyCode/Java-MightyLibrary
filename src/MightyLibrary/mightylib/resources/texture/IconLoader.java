package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
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
        exploreResourcesFile(data, "resources");
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

        try {
            BufferedImage image = ImageIO.read(new FileInputStream(icon.path()));
            icon.setBufferedImage(image);

        } catch (Exception e) {
            System.err.println("Can't find the path for :");
            System.err.println(icon.path() + "\n");
            e.printStackTrace();
        }
    }
}
