package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.source.ClasspathSource;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.IResourceSource;

import java.util.Collection;
import java.util.Objects;

/**
 * The loaders of the types of data (EngineRegistry.resources): one loader per type of data, whatever the namespace.
 * The engine registers the mandatory ones; the game adds those it uses (LibraryLoaders.all() for every optional loader
 * of the library) and its own.
 * And the sources of the files (ResourceSources): the engine's, then the folder of the game, then those of the plugins
 * in their order.
 */
public final class ResourcesSection {
    // Prefix of the classpath source of a plugin, followed by its namespace (unique: one jar holds it)
    private static final String CLASSPATH_PREFIX = "assets/";

    private final Registrations registrations;
    private final String plugin;
    // The one of the class of the plugin: a plugin in a jar reads its own jar
    private final ClassLoader classLoader;

    ResourcesSection(Registrations registrations, String plugin, ClassLoader classLoader) {
        this.registrations = registrations;
        this.plugin = plugin;
        this.classLoader = classLoader;
    }

    /**
     * Adds a source of resources above the folder of the game and the sources of the previous plugins (and of the
     * previous calls): a file it has replaces the one of the sources below, a list (textures.json...) is merged by
     * entry.
     */
    public ResourcesSection source(IResourceSource source) {
        registrations.checkOpen();
        registrations.addSource(plugin, Objects.requireNonNull(source, "source"));
        return this;
    }

    // The files of the classpath under "assets/<namespace of the plugin>/", read with the class loader of the plugin
    public ResourcesSection classpath() {
        registrations.checkOpen();
        return source(new ClasspathSource(classLoader, CLASSPATH_PREFIX + plugin + "/"));
    }

    // A folder on the disk (relative to the working directory or absolute)
    public ResourcesSection folder(String folder) {
        registrations.checkOpen();
        return source(new FolderSource(folder));
    }

    /**
     * @throws IllegalStateException if a loader of the same type of data is registered (the message names both
     *                               plugins)
     */
    public ResourcesSection add(ResourceLoader loader) {
        registrations.checkOpen();
        Objects.requireNonNull(loader, "loader");
        registrations.loaders.register(plugin, loader.getType(), loader);
        return this;
    }

    public ResourcesSection addAll(Collection<? extends ResourceLoader> loaders) {
        for (ResourceLoader loader : loaders)
            add(loader);

        return this;
    }

    /**
     * Replaces the loader of the same type of data registered by a previous plugin (the engine included).
     * @throws IllegalStateException if no loader of this type is registered
     */
    public ResourcesSection replace(ResourceLoader loader) {
        registrations.checkOpen();
        Objects.requireNonNull(loader, "loader");
        registrations.loaders.replace(plugin, loader.getType(), loader);
        return this;
    }
}
