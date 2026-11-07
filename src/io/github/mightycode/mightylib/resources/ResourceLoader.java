package io.github.mightycode.mightylib.resources;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public abstract class ResourceLoader {
    protected static final String[] IGNORE_FOLDERS = {
            "batchs"
    };

    public ResourceLoader(){
    }

    public abstract Class<? extends DataType> getType();

    public abstract String getResourceNameType();

    public abstract void create(Map<String, DataType> data);

    /**
     * Data used in place of a data of this type that failed, is still loading or has an unknown name
     * (Resources.usable, Resources.getResourceOrFallback). Created by the code, named FALLBACK_NAME, it must never fail.
     * Null (default): no fallback, for complex data that can't be replaced safely (maps, JSON...): users get null.
     */
    public DataType createFallback() {
        return null;
    }

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

    protected abstract void initWithFile(DataType dataType);

    // Preload step run by Resources (maybe on a worker): the state is set afterward, on the main thread
    final void read(DataType dataType) {
        if (dataType instanceof SingleSourceDataType) {
            SingleSourceDataType singleSourceDataType = (SingleSourceDataType) dataType;
            if (singleSourceDataType.validPath())
                initWithFile(singleSourceDataType);

        } else if (dataType instanceof MultiSourceDataType) {
            MultiSourceDataType multiSourceDataType = (MultiSourceDataType) dataType;
            if (multiSourceDataType.validPaths())
                initWithFile(multiSourceDataType);
        } else {
            initWithFile(dataType);
        }
    }
}
