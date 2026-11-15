package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandRegistry;

import java.util.Objects;

/**
 * The commands of a plugin (EngineRegistry.commands), kept for the whole run: the plugin owns them. Their identifier is
 * in the namespace of the plugin. The commands of the scenes and the views stay dynamic (Commands.registryFor).
 */
public final class CommandsSection implements CommandRegistry {
    private final Registrations registrations;
    private final EngineRegistry registry;

    CommandsSection(Registrations registrations, EngineRegistry registry) {
        this.registrations = registrations;
        this.registry = registry;
    }

    /**
     * @throws IllegalArgumentException if the identifier is outside the namespace of the plugin
     * @throws IllegalStateException    if a command with the same identifier is registered
     */
    @Override
    public void register(BaseCommand command) {
        registrations.checkOpen();
        Objects.requireNonNull(command, "command");
        registry.checkNamespace(command.getId());
        registrations.commands.register(registry.namespace(), command.getId(), command);
    }

    /**
     * Replaces the command with the same identifier registered by a previous plugin (any namespace, the engine
     * included): this plugin owns the new one.
     * @throws IllegalStateException if no command has this identifier
     */
    public void replace(BaseCommand command) {
        registrations.checkOpen();
        Objects.requireNonNull(command, "command");
        registrations.commands.replace(registry.namespace(), command.getId(), command);
    }
}
