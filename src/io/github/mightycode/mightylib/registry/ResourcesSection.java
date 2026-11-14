package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.resources.ResourceLoader;

import java.util.Collection;
import java.util.Objects;

/**
 * The loaders of the types of data (EngineRegistry.resources): one loader per type of data, whatever the namespace.
 * The engine registers the mandatory ones; the game adds those it uses (LibraryLoaders.all() for every optional loader
 * of the library) and its own.
 */
public final class ResourcesSection {
    private final Registrations registrations;
    private final String plugin;

    ResourcesSection(Registrations registrations, String plugin) {
        this.registrations = registrations;
        this.plugin = plugin;
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
