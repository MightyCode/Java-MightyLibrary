package io.github.mightycode.mightylib.registry;

/**
 * What a game, a tool or a mod adds to the engine (EngineConfig.addPlugin): loaders, commands... registered once, at
 * the start of the engine, before the window exists. The registry is frozen afterwards.
 * The engine registers its own content the same way, first, with the namespace "mightylib".
 */
public interface IPlugin {
    /**
     * The namespace of everything the plugin registers ([a-z0-9_.-], see Identifier): one plugin per namespace,
     * "mightylib" is the engine's.
     */
    String namespace();

    /**
     * Called once, on the main thread, in the order of EngineConfig.addPlugin, after the plugin of the engine. Only
     * registers (no GL, no engine yet): the registry can't be used after this call.
     */
    void register(EngineRegistry registry);
}
