package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.inputs.IInputManagerFactory;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.scenes.ISceneManagerFactory;
import io.github.mightycode.mightylib.services.IEngineService;
import io.github.mightycode.mightylib.services.IServiceFactory;
import io.github.mightycode.mightylib.services.ServiceDeclaration;
import io.github.mightycode.mightylib.services.ServiceKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

// The services registered by the plugins: namespaces, collisions, replace, order, freeze
class ServicesSectionTest {
    private interface ISound extends IEngineService {}

    private interface IOther extends IEngineService {}

    private static final ServiceKey<ISound> SOUND = ServiceKey.of(Identifier.of("mightylib", "sound"), ISound.class);
    private static final IServiceFactory<ISound> OPENAL = engine -> new ISound() {};
    private static final IServiceFactory<ISound> SILENT = engine -> new ISound() {};

    private static IPlugin plugin(String namespace, Consumer<EngineRegistry> register) {
        return new IPlugin() {
            @Override
            public String namespace() {
                return namespace;
            }

            @Override
            public void register(EngineRegistry registry) {
                register.accept(registry);
            }
        };
    }

    private static IPlugin engineWithSound() {
        return plugin(Identifier.ENGINE_NAMESPACE, registry -> registry.services().register(SOUND, OPENAL)
                .fallback(SILENT));
    }

    private static ServiceKey<IOther> gameKey(String path) {
        return ServiceKey.of(Identifier.of("game", path), IOther.class);
    }

    private static List<String> order(Registrations registrations) {
        List<String> keys = new ArrayList<>();
        for (ServiceDeclaration<?> declaration : registrations.services())
            keys.add(declaration.key() + "@" + declaration.plugin());
        return keys;
    }

    @Test
    void theServicesOfEveryPluginAreSortedAtTheFreeze() {
        Registrations registrations = EngineRegistry.Collect(engineWithSound(), List.of(plugin("game", registry -> {
            registry.services().register(gameKey("counter"), engine -> new IOther() {})
                    .requires(SOUND).after(gameKey("hud"));
            registry.services().register(gameKey("hud"), engine -> new IOther() {});
        })));

        assertEquals(List.of("mightylib:sound@mightylib", "game:hud@game", "game:counter@game"),
                order(registrations));
        ServiceDeclaration<?> sound = registrations.services().get(0);
        assertSame(OPENAL, sound.factory());
        assertSame(SILENT, sound.fallbackFactory());
        assertNull(sound.replaces());
    }

    @Test
    void aKeyOutsideTheNamespaceOfThePluginIsRefused() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> EngineRegistry.Collect(engineWithSound(), List.of(plugin("game",
                        registry -> registry.services().register(ServiceKey.of(Identifier.of("mod", "x"),
                                IOther.class), engine -> new IOther() {})))));
        assertTrue(error.getMessage().contains("mod:x"));
    }

    @Test
    void aDuplicateNamesBothPlugins() {
        ServiceKey<IOther> shared = gameKey("shared");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> EngineRegistry.Collect(engineWithSound(), List.of(plugin("game", registry -> {
                    registry.services().register(shared, engine -> new IOther() {});
                    registry.services().register(shared, engine -> new IOther() {});
                }))));
        assertEquals("Service game:shared registered by game and game (a later plugin can replace it)",
                error.getMessage());
    }

    @Test
    void replaceKeepsThePlaceAndTakesOnlyTheDeclarationsOfTheReplacement() {
        IServiceFactory<ISound> mine = engine -> new ISound() {};
        Registrations registrations = EngineRegistry.Collect(
                plugin(Identifier.ENGINE_NAMESPACE, registry -> {
                    registry.services().register(ServiceKey.of(Identifier.of("mightylib", "first"),
                            IOther.class), engine -> new IOther() {});
                    registry.services().register(SOUND, OPENAL).fallback(SILENT)
                            .after(ServiceKey.of(Identifier.of("mightylib", "first"), IOther.class));
                }),
                List.of(plugin("game", registry -> registry.services().replace(SOUND, mine))));

        assertEquals(List.of("mightylib:first@mightylib", "mightylib:sound@game"), order(registrations));
        ServiceDeclaration<?> sound = registrations.services().get(1);
        assertSame(mine, sound.factory());
        assertNull(sound.fallbackFactory());
        assertTrue(sound.afterKeys().isEmpty());
        assertEquals("mightylib", sound.replaces());
    }

    @Test
    void replacingAnAbsentServiceOrChangingItsContractIsAnError() {
        assertThrows(IllegalStateException.class, () -> EngineRegistry.Collect(engineWithSound(),
                List.of(plugin("game", registry -> registry.services().replace(gameKey("none"),
                        engine -> new IOther() {})))));

        ServiceKey<IOther> wrongContract = ServiceKey.of(SOUND.id(), IOther.class);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> EngineRegistry.Collect(engineWithSound(), List.of(plugin("game",
                        registry -> registry.services().replace(wrongContract, engine -> new IOther() {})))));
        assertTrue(error.getMessage().contains(ISound.class.getName()));
    }

    @Test
    void aMissingRequirementFailsTheRegistration() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> EngineRegistry.Collect(plugin(Identifier.ENGINE_NAMESPACE, registry -> {}),
                        List.of(plugin("game", registry -> registry.services()
                                .register(gameKey("counter"), engine -> new IOther() {}).requires(SOUND)))));
        assertTrue(error.getMessage().contains("requires mightylib:sound"));
    }

    @Test
    void theLastPluginGivesTheSceneManagerFactory() {
        ISceneManagerFactory engineFactory = (engine, context, main, loading) -> null;
        ISceneManagerFactory gameFactory = (engine, context, main, loading) -> null;

        Registrations engineOnly = EngineRegistry.Collect(plugin(Identifier.ENGINE_NAMESPACE,
                registry -> registry.services().sceneManager(engineFactory)), List.of());
        assertSame(engineFactory, engineOnly.sceneManagerFactory());
        assertEquals("mightylib", engineOnly.sceneManagerFactoryPlugin());

        Registrations replaced = EngineRegistry.Collect(plugin(Identifier.ENGINE_NAMESPACE,
                        registry -> registry.services().sceneManager(engineFactory)),
                List.of(plugin("game", registry -> registry.services().sceneManager(gameFactory)),
                        plugin("mod", registry -> {})));
        assertSame(gameFactory, replaced.sceneManagerFactory());
        assertEquals("game", replaced.sceneManagerFactoryPlugin());
    }

    @Test
    void theLastPluginGivesTheInputManagerFactory() {
        IInputManagerFactory engineFactory = InputManager::new;
        IInputManagerFactory modFactory = (keyboard, mouse, gamepads, registry) -> null;

        Registrations registrations = EngineRegistry.Collect(plugin(Identifier.ENGINE_NAMESPACE,
                        registry -> registry.services().inputManager(engineFactory)),
                List.of(plugin("game", registry -> {}),
                        plugin("mod", registry -> registry.services().inputManager(modFactory))));
        assertSame(modFactory, registrations.inputManagerFactory());
        assertEquals("mod", registrations.inputManagerFactoryPlugin());

        // The engine's creates the managers the input tests use
        assertNotNull(engineFactory.create(null, null, null, registrations.inputs()));
    }

    @Test
    void theSectionAndTheDeclarationsAreFrozenAfterTheRegistration() {
        List<EngineRegistry> kept = new ArrayList<>();
        List<ServiceDeclaration<?>> declarations = new ArrayList<>();
        EngineRegistry.Collect(engineWithSound(), List.of(plugin("game", registry -> {
            kept.add(registry);
            declarations.add(registry.services().register(gameKey("a"), engine -> new IOther() {}));
        })));

        assertThrows(IllegalStateException.class, () -> kept.get(0).services());
        assertThrows(IllegalStateException.class, () -> declarations.get(0).requires(SOUND));
    }
}
