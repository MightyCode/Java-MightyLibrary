package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.resources.sound.SoundData;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public abstract class ResourceLoader {
    protected static final String[] IGNORE_FOLDERS = {
            "batchs"
    };

    public ResourceLoader(){ }

    public abstract Class<? extends DataType> getType();

    public abstract String getResourceNameType();

    public abstract void create(Map<String, DataType> data);

    public abstract void fileDetected(Map<String, DataType> data, String currentPath, String name);

    public abstract String filterFile(String path);

    public final String getFileName(String path){
        return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
    }

    public final String getFileExtension(String path){
        int index = path.lastIndexOf(".");

        if (index == -1)
            return null;

        return path.substring(index);
    }

    public final void exploreResourcesFile(Map<String, DataType> data, String currentPath){
        exploreResourcesFile(data, currentPath, IGNORE_FOLDERS);
    }

    protected final boolean isIgnoredFolder(String[] ignoreFolders, String folderName){
        for (String ignoreFolder : ignoreFolders){
            if (ignoreFolder.equals(folderName))
                return true;
        }

        return false;
    }

    public final void exploreResourcesFile(Map<String, DataType> data, String currentPath,
                                           String[] ignoreFolders){
        File file = new File(currentPath);

        if (file.isFile()){
            String result = filterFile(currentPath);
            if (result != null){
                fileDetected(data, currentPath, result);
            }
        } else if (file.isDirectory()) {
            if (isIgnoredFolder(ignoreFolders, file.getName()))
                return;

            for (String childPath : Objects.requireNonNull(file.list()))
                exploreResourcesFile(data, currentPath + "/" + childPath);
        }
    }

    public abstract void load(DataType dataType);

    public abstract void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath);
}
