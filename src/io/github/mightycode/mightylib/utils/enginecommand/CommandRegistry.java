package io.github.mightycode.mightylib.utils.enginecommand;

/**
 * Where an owner adds its commands (IProjectLoading.registerCommands for the game). The commands belong to the owner
 * the registry was given for.
 */
public interface CommandRegistry {
    /**
     * @throws IllegalArgumentException if a command with the same identifier already exists
     */
    void register(BaseCommand command);
}
