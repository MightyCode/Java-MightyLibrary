package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.source.IResourceSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.resources.source.SourceEntry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finds the data of one type (DataType) in the files and reads them (preload, maybe on a worker). Registered by a
 * plugin (registry), attached to one Resources. Hooks: getType, getResourceNameType, create (the data of the files,
 * usually through exploreResourcesFile, filterFile and fileDetected), discover, createFallback and initWithFile.
 */
public abstract class ResourceLoader {
    // Folders skipped by exploreResourcesFile(data, path), at any depth
    // (the configurations are read by ConfigDataLoader only)
    private static final List<String> DEFAULT_IGNORED_FOLDERS = List.of("batchs", "configs");

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

    // Hook: the class of the data this loader creates (one loader per type)
    public abstract Class<? extends DataType> getType();

    // Hook: the name of the type in the logs and the commands ("Texture", "Sound"...)
    public abstract String getResourceNameType();

    /**
     * Hook called by Resources.init: put the data of this type in the map, by name, without reading them (usually
     * exploreResourcesFile on the resource folder). The data are read later by initWithFile.
     */
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

    // Hook called by exploreResourcesFile for each file kept by filterFile: put its data in the map under this name
    public abstract void fileDetected(Map<String, DataType> data, String currentPath, String name);

    // Hook called by exploreResourcesFile for each file: the name of its data, null when this loader ignores it
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

    // Every file under the logical folder currentPath ("" for the root) or this file, the batch folders skipped
    public final void exploreResourcesFile(Map<String, DataType> data, String currentPath){
        exploreResourcesFile(data, currentPath, DEFAULT_IGNORED_FOLDERS);
    }

    /**
     * Every file under the logical folder currentPath ("" for the root) or this file, in every source of the
     * resources, given to filterFile then to fileDetected when kept, with its logical path. The folders with one of
     * these names are skipped, at any depth (currentPath itself included).
     *
     * <p>A path in several sources is given once (the highest source serves it). Two paths giving the same name in
     * two sources: the data of the highest source is kept, one line "--... overrides ...", an exception in strict
     * mode. In one source, the loader decides (the last detected usually wins).
     */
    public final void exploreResourcesFile(Map<String, DataType> data, String currentPath,
                                           Collection<String> ignoredFolders){
        Resources resources = resources();
        new Exploration(resources.sources(), data, ignoredFolders, resources.isStrict())
                .explore(ResourceSources.normalize(currentPath));
    }

    // One call of exploreResourcesFile: the source of each name detected, for the collisions between sources
    private final class Exploration {
        private final ResourceSources sources;
        private final Map<String, DataType> data;
        private final Collection<String> ignoredFolders;
        private final boolean strict;
        private final Map<String, SourceEntry> detected = new HashMap<>();

        Exploration(ResourceSources sources, Map<String, DataType> data, Collection<String> ignoredFolders,
                    boolean strict) {
            this.sources = sources;
            this.data = data;
            this.ignoredFolders = ignoredFolders;
            this.strict = strict;
        }

        void explore(String path) {
            if (sources.isFolder(path)) {
                if (!path.isEmpty() && ignoredFolders.contains(path.substring(path.lastIndexOf('/') + 1)))
                    return;

                for (SourceEntry entry : sources.list(path)) {
                    if (entry.isFolder())
                        explore(entry.path());
                    else
                        detect(entry);
                }
            } else {
                Optional<IResourceSource> source = sources.resolve(path);
                if (source.isPresent()) {
                    int slash = path.lastIndexOf('/');
                    detect(new SourceEntry(source.get(), slash < 0 ? "" : path.substring(0, slash),
                            path.substring(slash + 1), false));
                }
            }
        }

        private void detect(SourceEntry entry) {
            String name = filterFile(entry.path());
            if (name == null)
                return;

            SourceEntry previous = detected.get(name);
            if (previous == null || previous.source() == entry.source()) {
                detected.put(name, entry);
                fileDetected(data, entry.path(), name);
                return;
            }

            // Same name in two sources: the highest one wins, whatever the order of the exploration. Decided before
            // fileDetected, so that the loader never sees the file that loses
            SourceEntry winner = entry, loser = previous;
            if (sources.sources().indexOf(previous.source()) > sources.sources().indexOf(entry.source())) {
                winner = previous;
                loser = entry;
            }

            String message = getResourceNameType() + " " + name + ": " + winner.path() + " ("
                    + winner.source().name() + ") overrides " + loser.path() + " (" + loser.source().name() + ")";
            if (strict)
                throw new IllegalStateException(message);

            System.out.println("--" + message);
            if (winner == entry) {
                detected.put(name, entry);
                fileDetected(data, entry.path(), name);
            }
        }
    }


    /**
     * Hook called to read a data (preload, maybe on a worker, never with GL): read its file(s) and fill it, or
     * throw a ResourceException. A MultiSourceDataType marks each file it read (markSourceRead).
     */
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
                readSources(multiSourceDataType);
        } else {
            initWithFile(dataType);
        }
    }

    // Every file must be marked read by the loader: a failure names those it did not read
    private void readSources(MultiSourceDataType data) {
        data.resetSourcesRead();
        try {
            initWithFile(data);
        } catch (ResourceException e) {
            List<String> unread = data.unreadSources();
            if (unread.isEmpty())
                throw e;
            throw new ResourceException(e.getMessage() + " (not read: " + String.join(", ", unread) + ")");
        }

        List<String> unread = data.unreadSources();
        if (!unread.isEmpty())
            throw new ResourceException("files not read: " + String.join(", ", unread));
    }
}
