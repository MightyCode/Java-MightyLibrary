package io.github.mightycode.mightylib.main.procedures;

import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.utils.enginecommand.CommandRegistry;

import java.util.List;

public interface IProjectLoading {
    /**
     * The loaders of the types of data of the project, added after the mandatory ones. Read once, before the
     * resources are created: the list can't change afterwards. One loader per type of data.
     * Temporary entry point: to be absorbed by the registration of the extensions (see later.md).
     */
    default List<ResourceLoader> loaders() {
        return List.of();
    }

    /**
     * The commands of the game, kept for the whole run (scene changes included). Called once, after the resources are
     * created. Temporary entry point: to be absorbed by the registration of the extensions (see later.md).
     */
    default void registerCommands(CommandRegistry registry) {}

    void contextLoading(WindowContext context);
}
