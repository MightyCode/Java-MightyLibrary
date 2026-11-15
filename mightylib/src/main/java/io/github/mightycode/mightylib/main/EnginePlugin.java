package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.console.Console;
import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.inputs.EngineActions;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.inputType.EInputType;
import io.github.mightycode.mightylib.inputs.inputType.InputSimple;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.resources.LibraryLoaders;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.services.EngineServices;
import io.github.mightycode.mightylib.services.IServiceFactory;
import io.github.mightycode.mightylib.sounds.ISoundService;
import io.github.mightycode.mightylib.sounds.OpenALSoundService;
import io.github.mightycode.mightylib.sounds.SilentSoundService;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.general.ListGeneralCommand;

import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT;

/**
 * The content of the engine, registered like the one of any plugin, before them: mandatory loaders, general commands,
 * actions of the console with their keys, services (sound, console).
 * A later plugin replaces an entry with replace.
 */
final class EnginePlugin implements IPlugin {
    private final EngineConfig config;
    // Created with the window, after the registration: read by the loader of the shaders when it works
    private final Supplier<ShaderManager> shaderManager;
    // The player has an access to the commands (EngineConfig.getCommandAccess, resolved once)
    private final boolean console;

    EnginePlugin(EngineConfig config, Supplier<ShaderManager> shaderManager, boolean console) {
        this.config = config;
        this.shaderManager = shaderManager;
        this.console = console;
    }

    @Override
    public String namespace() {
        return Identifier.ENGINE_NAMESPACE;
    }

    @Override
    public void register(EngineRegistry registry) {
        registry.resources().addAll(LibraryLoaders.mandatory(shaderManager));

        // "cheats on|off": the build allows the cheats and the game wants the command
        ECommandLevel maxAccess = config.getMaxCommandAccess();
        ListGeneralCommand.addCommands(registry.commands(),
                maxAccess != null && config.isCheatCommand() && maxAccess.includes(ECommandLevel.CHEAT));

        registry.inputs()
                .action(EngineActions.TOGGLE_CONSOLE, new InputSimple(GLFW_KEY_GRAVE_ACCENT, EInputType.Keyboard))
                .action(EngineActions.DETACH_CONSOLE, new InputSimple(GLFW_KEY_F2, EInputType.Keyboard))
                .action(EngineActions.RELOAD_TEXTURE, new InputSimple(GLFW_KEY_F5, EInputType.Keyboard));

        registry.services().sceneManager(SceneManager::new).inputManager(InputManager::new);

        registry.services().register(EngineServices.SOUND,
                        engine -> OpenALSoundService.Open(engine.resources(), GainTreeFile(engine)))
                .fallback(SilentSound());

        if (console) {
            registry.services().register(EngineServices.CONSOLE, engine -> {
                Console created = new Console(engine, engine.consoleLog(), engine.commands(), config.getConsoleFont());
                created.setLogsShown(config.isConsoleLogsShown());
                created.setDataShown(config.isConsoleDataShown());
                return created;
            });
        }
    }

    // The silent sound: fallback of the OpenAL one, forced by -Dmightylib.sound=silent
    static IServiceFactory<ISoundService> SilentSound() {
        return engine -> new SilentSoundService(GainTreeFile(engine));
    }

    // Null without gain tree
    private static String GainTreeFile(Engine engine) {
        String path = engine.config().getGainTreePath();
        return path == null ? null : engine.resources().folder() + path;
    }
}
