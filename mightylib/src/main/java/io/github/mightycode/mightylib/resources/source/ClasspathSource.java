package io.github.mightycode.mightylib.resources.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The files under a prefix of the classpath (ex. "assets/mightylib/"): a folder (in the IDE, target/classes) or a jar,
 * opened as a zip file system to be listed. A prefix absent from the classpath is an empty source.
 *
 * <p>The prefix must be unique, held by one jar only: a classpath folder can't be listed across several jars. The
 * engine and the plugins use "assets/&lt;namespace&gt;/" (EngineRegistry.resources().classpath()), unique since a
 * namespace is. The jar must contain the entries of its folders (Maven and Gradle write them).
 *
 * <p>A jar file system already opened (by another source) is reused, and closed by the one that opened it.
 */
public final class ClasspathSource implements IResourceSource {
    private final String prefix;
    private final PathTree tree;
    // Opened by this source, closed by close; null otherwise
    private final FileSystem ownedFileSystem;

    public ClasspathSource(ClassLoader classLoader, String prefix) {
        Objects.requireNonNull(classLoader, "classLoader");
        String logical = ResourceSources.normalize(Objects.requireNonNull(prefix, "prefix"));
        if (logical.isEmpty())
            throw new IllegalArgumentException("A classpath source needs a prefix (ex. assets/<namespace>/)");

        this.prefix = logical + "/";

        URL url = classLoader.getResource(logical);
        if (url == null)
            url = classLoader.getResource(this.prefix);

        FileSystem opened = null;
        Path root = null;
        if (url != null) {
            switch (url.getProtocol()) {
                case "file":
                    root = Paths.get(toUri(url));
                    break;
                case "jar":
                    String spec = url.toString();
                    int separator = spec.indexOf("!/");
                    if (separator < 0)
                        throw new IllegalStateException("Unreadable jar URL " + spec + " (" + name() + ")");

                    URI jar = URI.create(spec.substring(0, separator));
                    FileSystem fileSystem;
                    try {
                        fileSystem = FileSystems.newFileSystem(jar, Map.of());
                        opened = fileSystem;
                    } catch (FileSystemAlreadyExistsException e) {
                        fileSystem = FileSystems.getFileSystem(jar);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Can't open " + jar + " (" + name() + ")", e);
                    }
                    root = fileSystem.getPath("/" + logical);
                    break;
                default:
                    throw new IllegalStateException("Classpath source " + name() + " in an unsupported location: "
                            + url);
            }
        }

        ownedFileSystem = opened;
        tree = new PathTree(this, root);
    }

    private URI toUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unreadable URL " + url + " (" + name() + ")", e);
        }
    }

    // Normalized, ending with '/'
    public String prefix() {
        return prefix;
    }

    // The prefix was found on the classpath
    public boolean isPresent() {
        return !tree.isEmpty();
    }

    @Override
    public String name() {
        return "classpath:" + prefix;
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

    // Empty in a jar
    @Override
    public Optional<Path> localFile(String path) {
        return tree.localFile(path);
    }

    @Override
    public void close() {
        if (ownedFileSystem == null)
            return;

        try {
            ownedFileSystem.close();
        } catch (IOException e) {
            System.err.println("--Can't close " + name() + " : " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return name();
    }
}
