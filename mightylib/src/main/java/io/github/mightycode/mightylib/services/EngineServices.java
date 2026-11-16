package io.github.mightycode.mightylib.services;

import io.github.mightycode.mightylib.console.IConsoleService;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.sounds.ISoundService;

// The keys of the services registered by the engine (a plugin replaces them with registry.services().replace)
public final class EngineServices {
    private EngineServices() {}

    // Always registered: OpenAL, silent as fallback
    public static final ServiceKey<ISoundService> SOUND =
            ServiceKey.of(Identifier.of(Identifier.ENGINE_NAMESPACE, "sound"), ISoundService.class);

    // Only when the player has access to the commands (EngineConfig.setCommandAccess): test it with hasService
    public static final ServiceKey<IConsoleService> CONSOLE =
            ServiceKey.of(Identifier.of(Identifier.ENGINE_NAMESPACE, "console"), IConsoleService.class);
}
