package io.github.mightycode.mightylib.resources;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public abstract class ResourceLoader {
    protected static final String[] IGNORE_FOLDERS = {
            "batchs"
    };

    // The Resources registering the data of this loader, set by Resources.init (a loader serves one Resources)
    private volatile Resources owner;

    public ResourceLoader(){
    }

    final void attachTo(Resources resources) {
        if (owner == resources)
            return;

        if (owner != null)
            throw new IllegalStateException("The loader " + getResourceNameType() + " already serves another Resources");

        owner = resources;
    }

    // The Resources this loader serves: fails until Resources.init attached it
    protected final Resources resources() {
        Resources resources = owner;
        if (resources == null)
            throw new IllegalStateException("The loader " + getResourceNameType() + " is not attached to a Resources");

        return resources;
    }

    public abstract Class<? extends DataType> getType();

    public abstract String getResourceNameType();

    public abstract void create(Map<String, DataType> data);

    /**
     * Data of the files present now (the resource watcher looks for new files). Same as create by default; a loader
     * whose files are listed by a description can also add the files missing from it, with default settings.
     */
    public void discover(Map<String, DataType> data) {
        create(data);
    }

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
