package io.github.mightycode.mightylib.resources.source;

import io.github.mightycode.mightylib.utils.Logger;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The stack of the sources of the resources, from the lowest to the highest priority: the source of the engine, the
 * folder of the game (EngineConfig.setResourcesFolder), then the sources of the plugins in their order. A path is
 * served by the highest source that has it; the lists merged by entry read every layer (layers).
 *
 * <p>Immutable, built by the engine once the registry is frozen: read from any thread.
 */
public final class ResourceSources {
    // Owner of the folder of the game (not a namespace: no plugin has it)
    public static final String GAME_OWNER = "(game)";
    // Owner of the layer of the player (its configurations), above every other source
    public static final String PLAYER_OWNER = "(player)";

    private final List<IResourceSource> sources;
    // The owner of each source, at the same index
    private final List<String> owners;

    private ResourceSources(List<IResourceSource> sources, List<String> owners) {
        this.sources = List.copyOf(sources);
        this.owners = List.copyOf(owners);
        for (int i = 0; i < sources.size(); ++i) {
            if (indexOf(sources.get(i)) != i)
                throw new IllegalArgumentException("The source " + sources.get(i).name() + " is twice in the stack");
        }
    }

    // By identity: two sources may be equal without being the same root; -1 if absent
    private int indexOf(IResourceSource source) {
        for (int i = 0; i < sources.size(); ++i) {
            if (sources.get(i) == source)
                return i;
        }

        return -1;
    }

    // The given sources, from the lowest to the highest priority, without owner
    public static ResourceSources of(IResourceSource... sources) {
        Builder builder = builder();
        for (IResourceSource source : sources)
            builder.add("", source);

        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Adds the sources from the lowest to the highest priority
    public static final class Builder {
        private final List<IResourceSource> sources = new ArrayList<>();
        private final List<String> owners = new ArrayList<>();

        private Builder() {}

        // owner: the namespace of the plugin that declared the source, GAME_OWNER for the folder of the game
        public Builder add(String owner, IResourceSource source) {
            owners.add(Objects.requireNonNull(owner, "owner"));
            sources.add(Objects.requireNonNull(source, "source"));
            return this;
        }

        public ResourceSources build() {
            return new ResourceSources(sources, owners);
        }
    }

    /**
     * The logical form of a path: relative, separated by '/', without "." nor empty part nor final '/'; "" is the root.
     * @throws IllegalArgumentException if the path is absolute or contains ".." (a source never reads outside of its
     *                                  root)
     */
    public static String normalize(String path) {
        Objects.requireNonNull(path, "path");
        String slashes = path.replace('\\', '/');
        if (slashes.startsWith("/") || (slashes.length() >= 2 && slashes.charAt(1) == ':'))
            throw new IllegalArgumentException("Absolute resource path ->" + path
                    + "<-: the paths are relative to the sources");

        StringBuilder logical = new StringBuilder(slashes.length());
        for (String part : slashes.split("/")) {
            if (part.isEmpty() || ".".equals(part))
                continue;
            if ("..".equals(part))
                throw new IllegalArgumentException("Resource path ->" + path + "<- with \"..\": a source never reads"
                        + " outside of its root");

            if (logical.length() > 0)
                logical.append('/');
            logical.append(part);
        }

        return logical.toString();
    }

    // From the lowest to the highest priority; unmodifiable
    public List<IResourceSource> sources() {
        return sources;
    }

    // The namespace of the plugin that declared it, GAME_OWNER for the folder of the game, "" without owner
    public String owner(IResourceSource source) {
        int index = indexOf(source);
        if (index < 0)
            throw new IllegalArgumentException("The source " + source.name() + " is not in the stack");

        return owners.get(index);
    }

    // The highest source that has a file or a folder at this path
    public Optional<IResourceSource> resolve(String path) {
        String logical = normalize(path);
        for (int i = sources.size() - 1; i >= 0; --i) {
            if (sources.get(i).exists(logical))
                return Optional.of(sources.get(i));
        }

        return Optional.empty();
    }

    public boolean exists(String path) {
        return resolve(path).isPresent();
    }

    // The highest source that has a file at this path: a folder of a higher source doesn't hide it
    private Optional<IResourceSource> resolveFile(String logical) {
        for (int i = sources.size() - 1; i >= 0; --i) {
            IResourceSource source = sources.get(i);
            if (source.exists(logical) && !source.isFolder(logical))
                return Optional.of(source);
        }

        return Optional.empty();
    }

    // A source has a folder at this path
    public boolean isFolder(String path) {
        String logical = normalize(path);
        for (IResourceSource source : sources) {
            if (source.isFolder(logical))
                return true;
        }

        return false;
    }

    /**
     * The file of the highest source that has it; the caller closes the stream.
     * @throws NoSuchFileException if no source has it
     */
    public InputStream open(String path) throws IOException {
        String logical = normalize(path);
        Optional<IResourceSource> source = resolveFile(logical);
        if (source.isEmpty())
            throw new NoSuchFileException(logical + " (in no source)");

        return source.get().open(logical);
    }

    public byte[] readBytes(String path) throws IOException {
        try (InputStream stream = open(path)) {
            return stream.readAllBytes();
        }
    }

    // UTF-8
    public String readString(String path) throws IOException {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * The file in a native buffer (MemoryUtil.memAlloc, for STB and OpenAL), ready to be read: the caller frees it with
     * MemoryUtil.memFree.
     */
    public ByteBuffer readNative(String path) throws IOException {
        String logical = normalize(path);
        Optional<IResourceSource> source = resolveFile(logical);
        if (source.isEmpty())
            throw new NoSuchFileException(logical + " (in no source)");

        Optional<Path> file = source.get().localFile(logical);
        if (file.isPresent() && Files.isRegularFile(file.get()))
            return readNativeFile(file.get());

        byte[] bytes;
        try (InputStream stream = source.get().open(logical)) {
            bytes = stream.readAllBytes();
        }
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        buffer.put(bytes).flip();
        return buffer;
    }

    // Read straight into the native buffer, without heap copy
    private static ByteBuffer readNativeFile(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size > Integer.MAX_VALUE)
                throw new IOException("File too big for a buffer : " + file);

            ByteBuffer buffer = MemoryUtil.memAlloc((int) size);
            try {
                while (buffer.hasRemaining()) {
                    if (channel.read(buffer) < 0)
                        break;
                }
            } catch (IOException e) {
                MemoryUtil.memFree(buffer);
                throw e;
            }

            buffer.flip();
            return buffer;
        }
    }

    // Size in bytes of the file of the highest source that has it, -1 if none
    public long size(String path) {
        String logical = normalize(path);
        return resolveFile(logical).map(source -> source.size(logical)).orElse(-1L);
    }

    // The children of the folder in every source, sorted by name; for a name in several sources, the highest one
    public List<SourceEntry> list(String folder) {
        String logical = normalize(folder);
        Map<String, SourceEntry> byName = new TreeMap<>();
        for (IResourceSource source : sources) {
            for (SourceEntry entry : source.list(logical))
                byName.put(entry.name(), entry);
        }

        return List.copyOf(byName.values());
    }

    // The sources that have a file or a folder at this path, from the lowest to the highest (lists merged by entry)
    public List<IResourceSource> layers(String path) {
        String logical = normalize(path);
        List<IResourceSource> layers = new ArrayList<>();
        for (IResourceSource source : sources) {
            if (source.exists(logical))
                layers.add(source);
        }

        return layers;
    }

    // The file on the disk of the highest source that has it as a file; empty if none, or if it is not on the disk
    public Optional<Path> localFile(String path) {
        String logical = normalize(path);
        return resolveFile(logical).flatMap(source -> source.localFile(logical));
    }

    /**
     * The files on the disk this path maps to in every source on the disk, existing or not, from the lowest to the
     * highest: what the resource watcher follows (a file created in a higher source replaces the data).
     */
    public List<Path> localCandidates(String path) {
        String logical = normalize(path);
        List<Path> candidates = new ArrayList<>();
        for (IResourceSource source : sources)
            source.localFile(logical).ifPresent(candidates::add);

        return candidates;
    }

    // Closes every source, the highest first; a failure doesn't stop the others
    public void close() {
        for (int i = sources.size() - 1; i >= 0; --i) {
            try {
                sources.get(i).close();
            } catch (RuntimeException e) {
                Logger.Warn(Logger.CATEGORY_RESOURCES, "Can't close the source " + sources.get(i).name() + " : " + e.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        List<String> names = new ArrayList<>();
        for (IResourceSource source : sources)
            names.add(source.name());

        return "ResourceSources" + names;
    }
}
