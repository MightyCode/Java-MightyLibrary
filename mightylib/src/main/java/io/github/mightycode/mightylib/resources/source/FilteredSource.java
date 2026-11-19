package io.github.mightycode.mightylib.resources.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A source limited to one folder of another: only the paths under that folder exist (the root lists only the folder
 * itself). Used for the layer of the player (the folder of its data, of which only "configs" is a resource), or by a
 * project that shares a folder with something else.
 */
public final class FilteredSource implements IResourceSource {
    private final IResourceSource inner;
    private final String folder;
    private final String name;

    /**
     * @param folder logical path of the only folder exposed (ResourceSources.normalize), not the root
     */
    public FilteredSource(IResourceSource inner, String folder) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.folder = ResourceSources.normalize(folder);
        if (this.folder.isEmpty())
            throw new IllegalArgumentException("A filtered source exposes a folder, not the root");

        name = inner.name() + " (" + this.folder + "/)";
    }

    public IResourceSource inner() {
        return inner;
    }

    // The folder exposed
    public String folder() {
        return folder;
    }

    // The folder, what it holds, and its parents (to list down to it)
    private boolean visible(String path) {
        return path.equals(folder) || path.startsWith(folder + "/") || isParent(path);
    }

    private boolean isParent(String path) {
        return path.isEmpty() || folder.startsWith(path + "/");
    }

    // Under the folder (files can't be anywhere else)
    private boolean inside(String path) {
        return path.startsWith(folder + "/");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean exists(String path) {
        return visible(path) && inner.exists(path);
    }

    @Override
    public boolean isFolder(String path) {
        return visible(path) && inner.isFolder(path);
    }

    @Override
    public InputStream open(String path) throws IOException {
        if (!inside(path))
            throw new NoSuchFileException(path + " (" + name + ")");

        return inner.open(path);
    }

    @Override
    public long size(String path) {
        return inside(path) ? inner.size(path) : -1;
    }

    @Override
    public List<SourceEntry> list(String parent) {
        if (!visible(parent))
            return List.of();

        List<SourceEntry> entries = new ArrayList<>();
        for (SourceEntry entry : inner.list(parent)) {
            if (visible(entry.path()))
                entries.add(new SourceEntry(this, parent, entry.name(), entry.isFolder()));
        }

        return entries;
    }

    // The folder and its parents too: the resource watcher follows the folders of the sources on the disk
    @Override
    public Optional<Path> localFile(String path) {
        return visible(path) ? inner.localFile(path) : Optional.empty();
    }

    @Override
    public void close() {
        inner.close();
    }
}
