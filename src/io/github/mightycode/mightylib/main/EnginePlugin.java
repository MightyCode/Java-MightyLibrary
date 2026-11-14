package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.resources.LibraryLoaders;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.general.ListGeneralCommand;

/**
 * The content of the engine, registered like the one of any plugin, before them: mandatory loaders, general commands.
 * A later plugin replaces an entry with replace.
 */
final class EnginePlugin implements IPlugin {
    private final EngineConfig config;

    EnginePlugin(EngineConfig config) {
        this.config = config;
    }

    @Override
    public String namespace() {
        return Identifier.ENGINE_NAMESPACE;
    }

    @Override
    public void register(EngineRegistry registry) {
        registry.resources().addAll(LibraryLoaders.mandatory());

        // "cheats on|off": the build allows the cheats and the game wants the command
        ECommandLevel maxAccess = config.getMaxCommandAccess();
        ListGeneralCommand.addCommands(registry.commands(),
                maxAccess != null && config.isCheatCommand() && maxAccess.includes(ECommandLevel.CHEAT));
    }
}
