package io.github.mightycode.mightylib.resources.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// A folder on the disk (relative to the working directory or absolute); a missing folder is an empty source
public final class FolderSource implements IResourceSource {
    private final String name;
    private final PathTree tree;

    public FolderSource(String folder) {
        this(Paths.get(Objects.requireNonNull(folder, "folder")));
    }

    public FolderSource(Path folder) {
        Objects.requireNonNull(folder, "folder");
        String shown = folder.toString().replace('\\', '/');
        name = "folder:" + (shown.endsWith("/") ? shown : shown + "/");
        tree = new PathTree(this, folder.toAbsolutePath().normalize());
    }

    // Absolute and normalized
    public Path folder() {
        return tree.root();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean exists(String path) {
        return tree.exists(path);
    }

    @Override
    public boolean isFolder(String path) {
        return tree.isFolder(path);
    }

    @Override
    public InputStream open(String path) throws IOException {
        return tree.open(path);
    }

    @Override
    public long size(String path) {
        return tree.size(path);
    }

    @Override
    public List<SourceEntry> list(String folder) {
        return tree.list(folder);
    }

    @Override
    public Optional<Path> localFile(String path) {
        return tree.localFile(path);
    }

    @Override
    public String toString() {
        return name;
    }
}
