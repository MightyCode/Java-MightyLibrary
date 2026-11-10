package io.github.mightycode.mightylib.main.procedures;

import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.enginecommand.CommandRegistry;

public interface IProjectLoading {
    void init(Resources resources);

    /**
     * The commands of the game, kept for the whole run (scene changes included). Called once, after init.
     * Temporary entry point: to be absorbed by the registration of the extensions (see later.md).
     */
    default void registerCommands(CommandRegistry registry) {}

    void contextLoading(WindowContext context);
}
