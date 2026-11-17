package io.github.mightycode.mightylib.resources.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A root of resource files: a folder on the disk (FolderSource), a prefix of the classpath (ClasspathSource), or any
 * other place a project implements. The engine stacks them in ResourceSources: the highest source that has a file
 * serves it.
 *
 * <p>Paths are logical: relative to the root, separated by '/', without ".." nor absolute part
 * (ResourceSources.normalize, an IllegalArgumentException otherwise); "" is the root. A source never reads outside of
 * its root.
 *
 * <p>Thread-safe and without modifiable state after its construction: read by the workers of the resources and by the
 * resource watcher.
 */
public interface IResourceSource {
    // For the debug: "folder:resources/", "classpath:assets/mightylib/"
    String name();

    // A file or a folder at this path
    boolean exists(String path);

    boolean isFolder(String path);

    /**
     * The content of the file; the caller closes the stream.
     * @throws java.nio.file.NoSuchFileException if there is no file at this path
     */
    InputStream open(String path) throws IOException;

    // Size in bytes of the file, -1 if there is no file at this path
    long size(String path);

    // The direct children of the folder, sorted by name; empty if there is no folder at this path
    List<SourceEntry> list(String folder);

    /**
     * The file on the disk this path maps to, existing or not; empty if the source is not on the disk (classpath in a
     * jar). Read by the resource watcher (only the sources on the disk are watched) and by the readers that need a
     * file (Assimp).
     */
    Optional<Path> localFile(String path);

    // Releases what the source opened (a jar opened as a file system); called once, at the shutdown of the engine
    default void close() {}
}
