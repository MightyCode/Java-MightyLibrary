package io.github.mightycode.mightylib.resources.source;

import java.util.Objects;

// A child of a folder of a source (IResourceSource.list, ResourceSources.list): immutable
public final class SourceEntry {
    private final IResourceSource source;
    private final String path;
    private final String name;
    private final boolean folder;

    public SourceEntry(IResourceSource source, String parent, String name, boolean folder) {
        this.source = Objects.requireNonNull(source, "source");
        this.name = Objects.requireNonNull(name, "name");
        this.path = parent.isEmpty() ? name : parent + "/" + name;
        this.folder = folder;
    }

    // The source that has it (for ResourceSources.list, the highest one)
    public IResourceSource source() {
        return source;
    }

    // Logical path: the folder listed, then the name
    public String path() {
        return path;
    }

    // Name of the file or folder, without its parents
    public String name() {
        return name;
    }

    public boolean isFolder() {
        return folder;
    }

    @Override
    public String toString() {
        return path + (folder ? "/" : "") + " (" + source.name() + ")";
    }
}
