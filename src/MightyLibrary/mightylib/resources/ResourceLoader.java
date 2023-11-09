package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.resources.sound.SoundData;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public abstract class ResourceLoader {

    public ResourceLoader(){ }

    public abstract Class<?> getType();

    public abstract String getResourceNameType();

    public abstract void create(Map<String, DataType> data);

    public abstract void fileDetected(Map<String, DataType> data, String currentPath, String name);

    public abstract String filterFile(String path);

    public final String getFileName(String path){
        return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
    }

    public final String getFileExtension(String path){
        return path.substring(path.lastIndexOf("."));
    }

    public final void exploreResourcesFile(Map<String, DataType> data, String currentPath){
        File file = new File(currentPath);

        if (file.isFile()){
            String result = filterFile(currentPath);
            if (result != null){
                fileDetected(data, currentPath, result);
            }
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                exploreResourcesFile(data, currentPath + "/" + childPath);
            }
        }
    }

    public abstract void load(DataType dataType);

    public abstract void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath);
}
