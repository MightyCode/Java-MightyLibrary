package io.github.mightycode.mightylib.utils.enginecommand;

/**
 * Where an owner adds its commands: a plugin (EngineRegistry.commands), a view (Commands.registryFor). The commands
 * belong to the owner the registry was given for.
 */
public interface CommandRegistry {
    /**
     * @throws RuntimeException if a command with the same identifier already exists (IllegalArgumentException for a
     *                          view, IllegalStateException for a plugin: the message names both plugins)
     */
    void register(BaseCommand command);
}
