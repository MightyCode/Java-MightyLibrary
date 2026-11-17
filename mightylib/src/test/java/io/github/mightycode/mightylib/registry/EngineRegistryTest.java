package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FakeLoader;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.IResourceSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.resources.source.SourceEntry;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

// The registration phase: order, collisions, replace, namespaces, freeze
class EngineRegistryTest {
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

    private static IPlugin engine(Consumer<EngineRegistry> register) {
        return plugin(Identifier.ENGINE_NAMESPACE, register);
    }

    private static final IPlugin EMPTY_ENGINE = engine(registry -> {});

    private static BaseCommand command(Identifier id) {
        return new BaseCommand(id, ECommandLevel.USER, "A command of the tests") {
            @Override
            protected void syntax(CommandNode root) {
                root.executes(context -> CommandResult.ok("done"));
            }
        };
    }

    @Test
    void engineFirstThenPluginsInTheirOrder() {
        List<String> calls = new ArrayList<>();
        FakeLoader engineLoader = new FakeLoader();
        OtherLoader gameLoader = new OtherLoader();

        Registrations registrations = EngineRegistry.Collect(
                engine(registry -> {
                    calls.add(registry.namespace());
                    registry.resources().add(engineLoader);
                }),
                List.of(plugin("game", registry -> {
                    calls.add(registry.namespace());
                    registry.resources().add(gameLoader);
                    registry.commands().register(command(registry.id("menu")));
                }), plugin("mod", registry -> calls.add(registry.namespace()))));

        assertEquals(List.of("mightylib", "game", "mod"), calls);
        assertEquals(List.of("mightylib", "game", "mod"), registrations.plugins());
        assertEquals(List.of(engineLoader, gameLoader), registrations.loaders());
        assertEquals(1, registrations.commands("game").size());
        assertTrue(registrations.commands("mod").isEmpty());
        assertTrue(registrations.isFrozen());
    }

    @Test
    void collisionNamesBothPlugins() {
        IllegalStateException loaders = assertThrows(IllegalStateException.class, () -> EngineRegistry.Collect(
                engine(registry -> registry.resources().add(new FakeLoader())),
                List.of(plugin("game", registry -> registry.resources().add(new FakeLoader())))));
        assertTrue(loaders.getMessage().contains("mightylib") && loaders.getMessage().contains("game"),
                loaders.getMessage());

        IllegalStateException commands = assertThrows(IllegalStateException.class, () -> EngineRegistry.Collect(
                EMPTY_ENGINE,
                List.of(plugin("game", registry -> {
                    registry.commands().register(command(registry.id("menu")));
                    registry.commands().register(command(registry.id("menu")));
                }))));
        assertTrue(commands.getMessage().contains("game:menu"), commands.getMessage());
    }

    // A later plugin replaces an entry of the engine or of another plugin: it owns it, at the same place
    @Test
    void replaceTakesTheEntryOfAPreviousPlugin() {
        BaseCommand engineHelp = command(Identifier.of("mightylib", "help"));
        BaseCommand gameHelp = command(Identifier.of("mightylib", "help"));
        FakeLoader gameLoader = new FakeLoader();

        Registrations registrations = EngineRegistry.Collect(
                engine(registry -> {
                    registry.commands().register(engineHelp);
                    registry.resources().add(new FakeLoader()).add(new OtherLoader());
                }),
                List.of(plugin("game", registry -> {
                    registry.commands().replace(gameHelp);
                    registry.resources().replace(gameLoader);
                })));

        assertTrue(registrations.commands("mightylib").isEmpty());
        assertEquals(List.of(gameHelp), registrations.commands("game"));
        assertSame(gameLoader, registrations.loaders().get(0));
        assertEquals(2, registrations.loaders().size());
    }

    @Test
    void replaceOfAnAbsentEntryFails() {
        assertThrows(IllegalStateException.class, () -> EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("game", registry -> registry.commands().replace(command(registry.id("menu")))))));
        assertThrows(IllegalStateException.class, () -> EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("game", registry -> registry.resources().replace(new FakeLoader())))));
    }

    // A plugin registers the identifiers of its namespace only
    @Test
    void otherNamespaceIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("game", registry -> registry.commands().register(command(Identifier.of("mod", "x")))))));
        assertThrows(IllegalArgumentException.class, () -> EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("game", registry -> registry.commands().register(
                        command(Identifier.of("mightylib", "help")))))));
    }

    @Test
    void namespacesOfThePlugins() {
        assertThrows(IllegalStateException.class, () -> EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("game", registry -> {}), plugin("game", registry -> {}))));
        assertThrows(IllegalArgumentException.class, () -> EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("mightylib", registry -> {}))));
        assertThrows(IllegalArgumentException.class, () -> EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("Bad Name", registry -> {}))));
        assertThrows(IllegalArgumentException.class, () -> EngineRegistry.Collect(plugin("game", registry -> {}),
                List.of()));
    }

    // A registry kept by a plugin can't be used after the phase
    @Test
    void registryIsFrozenAfterThePhase() {
        EngineRegistry[] kept = new EngineRegistry[1];
        Registrations registrations = EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("game", registry -> kept[0] = registry)));

        assertTrue(registrations.isFrozen());
        assertThrows(IllegalStateException.class, () -> kept[0].resources());
        assertThrows(IllegalStateException.class, () -> kept[0].commands());
        assertThrows(UnsupportedOperationException.class, () -> registrations.loaders().add(new FakeLoader()));
    }

    @Test
    void sourcesOfResourcesEngineThenGameThenPlugins() {
        FolderSource engineSource = new FolderSource("engine");
        FolderSource game = new FolderSource("game");
        FolderSource first = new FolderSource("first");
        FolderSource second = new FolderSource("second");
        FolderSource mod = new FolderSource("mod");

        Registrations registrations = EngineRegistry.Collect(engine(registry -> registry.resources().source(engineSource)),
                List.of(plugin("game", registry -> registry.resources().source(first).source(second)),
                        plugin("other", registry -> {}),
                        plugin("mod", registry -> registry.resources().source(mod))));

        assertEquals(List.of(first, second), registrations.sources("game"));
        assertTrue(registrations.sources("other").isEmpty());

        ResourceSources stack = registrations.resourceSources(game);
        assertEquals(List.of(engineSource, game, first, second, mod), stack.sources());
        assertEquals("mightylib", stack.owner(engineSource));
        assertEquals(ResourceSources.GAME_OWNER, stack.owner(game));
        assertEquals("game", stack.owner(second));
        assertEquals("mod", stack.owner(mod));
    }

    // An empty source that records its closing
    private static final class ClosingSource implements IResourceSource {
        int closed;

        @Override
        public String name() {
            return "closing";
        }

        @Override
        public boolean exists(String path) {
            return false;
        }

        @Override
        public boolean isFolder(String path) {
            return false;
        }

        @Override
        public InputStream open(String path) throws IOException {
            throw new NoSuchFileException(path);
        }

        @Override
        public long size(String path) {
            return -1;
        }

        @Override
        public List<SourceEntry> list(String folder) {
            return List.of();
        }

        @Override
        public Optional<Path> localFile(String path) {
            return Optional.empty();
        }

        @Override
        public void close() {
            ++closed;
        }
    }

    @Test
    void sourcesClosedWhenTheStackCanNotBeBuilt() {
        ClosingSource twice = new ClosingSource();
        ClosingSource other = new ClosingSource();
        ClosingSource game = new ClosingSource();
        Registrations registrations = EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("game", registry -> registry.resources().source(other).source(twice)),
                        plugin("mod", registry -> registry.resources().source(twice))));

        assertThrows(IllegalArgumentException.class, () -> registrations.resourceSources(game));
        assertTrue(other.closed > 0);
        assertTrue(twice.closed > 0);
        assertEquals(1, game.closed);
    }

    @Test
    void classpathSourceOfAPluginUnderItsNamespace() {
        Registrations registrations = EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("game", registry -> registry.resources().classpath().folder("folder"))));

        List<IResourceSource> sources = registrations.sources("game");
        assertEquals("classpath:assets/game/", sources.get(0).name());
        assertEquals("folder:folder/", sources.get(1).name());
    }

    @Test
    void sourcesFrozenAfterThePhase() {
        ResourcesSection[] kept = new ResourcesSection[1];
        Registrations registrations = EngineRegistry.Collect(EMPTY_ENGINE,
                List.of(plugin("game", registry -> kept[0] = registry.resources())));
        assertThrows(IllegalStateException.class, () -> kept[0].folder("late"));
        assertThrows(IllegalStateException.class, () -> kept[0].classpath());
        assertThrows(UnsupportedOperationException.class,
                () -> registrations.sources("game").add(new FolderSource("late")));
    }

    // A second type of data, different from FakeData
    private static class OtherLoader extends FakeLoader {
        @Override
        public Class<? extends DataType> getType() {
            return OtherData.class;
        }

        @Override
        public String getResourceNameType() {
            return "Other";
        }
    }

    private abstract static class OtherData extends DataType {
        OtherData() {
            super(TYPE_SET_UP.IMMEDIATELY_IN_CURRENT_CONTEXT, "other");
        }
    }
}
