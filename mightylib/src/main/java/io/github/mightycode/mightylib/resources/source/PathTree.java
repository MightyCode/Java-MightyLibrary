package io.github.mightycode.mightylib.resources.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// The files under a root of a file system (the disk, or a jar opened as a zip file system); null root = no file
final class PathTree {
    private final IResourceSource owner;
    private final Path root;

    PathTree(IResourceSource owner, Path root) {
        this.owner = owner;
        this.root = root;
    }

    boolean isEmpty() {
        return root == null;
    }

    Path root() {
        return root;
    }

    // Null without root
    private Path resolve(String path) {
        String logical = ResourceSources.normalize(path);
        return root == null ? null : root.resolve(logical);
    }

    boolean exists(String path) {
        Path file = resolve(path);
        return file != null && Files.exists(file);
    }

    boolean isFolder(String path) {
        Path file = resolve(path);
        return file != null && Files.isDirectory(file);
    }

    InputStream open(String path) throws IOException {
        Path file = resolve(path);
        if (file == null || !Files.isRegularFile(file))
            throw new NoSuchFileException(ResourceSources.normalize(path) + " (" + owner.name() + ")");

        return Files.newInputStream(file);
    }

    long size(String path) {
        Path file = resolve(path);
        if (file == null || !Files.isRegularFile(file))
            return -1;

        try {
            return Files.size(file);
        } catch (IOException e) {
            return -1;
        }
    }

    List<SourceEntry> list(String folder) {
        String logical = ResourceSources.normalize(folder);
        Path directory = resolve(logical);
        if (directory == null || !Files.isDirectory(directory))
            return List.of();

        List<SourceEntry> entries = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            for (Path child : children) {
                Path fileName = child.getFileName();
                if (fileName == null)
                    continue;

                // The zip file system ends the names of its folders with '/'
                String name = fileName.toString();
                if (name.endsWith("/"))
                    name = name.substring(0, name.length() - 1);

                entries.add(new SourceEntry(owner, logical, name, Files.isDirectory(child)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Can't list " + logical + " (" + owner.name() + ")", e);
        }

        entries.sort(Comparator.comparing(SourceEntry::name));
        return entries;
    }

    // Only on the default file system (the disk)
    Optional<Path> localFile(String path) {
        Path file = resolve(path);
        if (file == null || file.getFileSystem() != FileSystems.getDefault())
            return Optional.empty();

        return Optional.of(file);
    }
}
