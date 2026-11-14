package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything the plugins registered (EngineRegistry.Collect), frozen once the last plugin is done: the engine reads it
 * to create its systems. Pure data: no GL, no engine.
 */
public final class Registrations {
    private final List<String> plugins = new ArrayList<>();
    final Entries<Class<? extends DataType>, ResourceLoader> loaders =
            new Entries<>("Loader of", Class::getSimpleName);
    final Entries<Identifier, BaseCommand> commands = new Entries<>("Command", Identifier::toString);

    private boolean frozen;

    Registrations() {}

    void addPlugin(String namespace) {
        plugins.add(namespace);
    }

    void freeze() {
        frozen = true;
    }

    void checkOpen() {
        if (frozen)
            throw new IllegalStateException("The registry is frozen: register from IPlugin.register only");
    }

    public boolean isFrozen() {
        return frozen;
    }

    // Namespaces of the plugins, in registration order: the engine first
    public List<String> plugins() {
        return Collections.unmodifiableList(plugins);
    }

    // The loaders of every plugin, in registration order
    public List<ResourceLoader> loaders() {
        return Collections.unmodifiableList(loaders.values());
    }

    // The commands the plugin registered or replaced: it owns them
    public List<BaseCommand> commands(String plugin) {
        return Collections.unmodifiableList(commands.valuesOf(plugin));
    }
}
