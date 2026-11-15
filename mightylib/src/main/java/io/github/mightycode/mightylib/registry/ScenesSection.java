package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.SceneRegistry;

import java.util.function.Supplier;

/**
 * The scenes of a plugin by name (EngineRegistry.scenes): what the command scene and the menus offer. Optional: a
 * scene that is not registered can still be created by the code. Registered with its class (to name the current
 * scene), a factory (Level::new) and a description.
 */
public final class ScenesSection {
    static final class SceneEntry {
        final Identifier identifier;
        final Class<? extends Scene> type;
        final Supplier<? extends Scene> factory;
        final String description;

        private SceneEntry(Identifier identifier, Class<? extends Scene> type, Supplier<? extends Scene> factory,
                           String description) {
            SceneRegistry.CheckEntry(identifier, type, factory, description);
            this.identifier = identifier;
            this.type = type;
            this.factory = factory;
            this.description = description;
        }
    }

    private final Registrations registrations;
    private final EngineRegistry registry;

    ScenesSection(Registrations registrations, EngineRegistry registry) {
        this.registrations = registrations;
        this.registry = registry;
    }

    /**
     * @throws IllegalArgumentException if the identifier is outside the namespace of the plugin, or the description is
     *                                  empty
     * @throws IllegalStateException    if a scene has this identifier
     */
    public <T extends Scene> ScenesSection register(Identifier identifier, Class<T> type, Supplier<? extends T> factory,
                                                   String description) {
        registrations.checkOpen();
        SceneEntry entry = new SceneEntry(identifier, type, factory, description);
        registry.checkNamespace(identifier);
        registrations.scenes.register(registry.namespace(), identifier, entry);
        return this;
    }

    /**
     * Replaces the scene of a previous plugin (the engine included) with this identifier.
     * @throws IllegalStateException if no scene has this identifier
     */
    public <T extends Scene> ScenesSection replace(Identifier identifier, Class<T> type, Supplier<? extends T> factory,
                                                  String description) {
        registrations.checkOpen();
        SceneEntry entry = new SceneEntry(identifier, type, factory, description);
        registrations.scenes.replace(registry.namespace(), identifier, entry);
        return this;
    }
}
