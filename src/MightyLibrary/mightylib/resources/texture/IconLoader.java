package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Objects;

public class IconLoader extends ResourceLoader {

    @Override
    public Class<?> getType() {
        return Icon.class;
    }

    @Override
    public String getResourceNameType() {
        return "Icon";
    }

    @Override
    public void create(Map<String, DataType> data){
        create(data, "resources/icon");
    }

    private void create(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            data.put(name, new Icon(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                create(data, path + "/" + childPath);
            }
        }
    }

    @Override
    public void load(DataType dataType) {
        if (!(dataType instanceof Icon))
            return;

        Icon icon = (Icon) dataType;

        try {
            BufferedImage image = ImageIO.read(new FileInputStream(icon.getPath()));
            icon.createIcon(image);

        } catch (Exception e) {
            System.err.println("Can't find the path for :");
            System.err.println(icon.getPath() + "\n");
            e.printStackTrace();
        }
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {

    }
}
