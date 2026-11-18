package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FakeLoader;
import io.github.mightycode.mightylib.resources.internal.IGLElementSink;
import io.github.mightycode.mightylib.services.IEngineService;
import io.github.mightycode.mightylib.services.ServiceKey;
import io.github.mightycode.mightylib.sounds.SilentSoundService;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.TestCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The core of the engine, without window nor GL: plugins, resources, commands and services
class HeadlessEngineTest {
    private static final ServiceKey<CountingService> COUNTER =
            ServiceKey.of(Identifier.of("test", "counter"), CountingService.class);

    private static final class CountingService implements IEngineService {
        int updates;

        @Override
        public void update() {
            ++updates;
        }
    }

    // Keeps the data whose GL element is asked, sends nothing
    private static final class RecordingSink implements IGLElementSink {
        final List<DataType> acquired = new ArrayList<>();

        @Override
        public GLElement acquire(DataType data, boolean urgent) {
            acquired.add(data);
            return null;
        }

        @Override
        public void release(GLElement element, boolean urgent) {
        }
    }

    private final FakeLoader loader = new FakeLoader();

    private final IPlugin plugin = new IPlugin() {
        @Override
        public String namespace() {
            return "test";
        }

        @Override
        public void register(EngineRegistry registry) {
            registry.commands().register(TestCommands.Syntax("hello", ECommandLevel.USER, "Answers hello",
                    root -> root.executes(context -> CommandResult.ok("hello"))));
            registry.resources().add(loader);
            registry.services().register(COUNTER, engine -> new CountingService());
        }
    };

    private Engine engine;
    private Views.Scope scope;

    @AfterEach
    void stop() {
        if (engine != null)
            assertEquals(ListError.NO_ERROR, engine.shutdown(ListError.NO_ERROR));
        if (scope != null)
            scope.close();
    }

    private Engine start(Path resources) {
        System.setProperty(EngineConfig.SOUND_PROPERTY, "silent");
        try {
            engine = new Engine(new EngineConfig()
                    .setResourcesFolder(resources.toString())
                    .addPlugin(plugin));
            scope = Views.enter(engine);
            assertEquals(ListError.NO_ERROR, engine.setupHeadless(new RecordingSink()));
            return engine;
        } finally {
            System.clearProperty(EngineConfig.SOUND_PROPERTY);
        }
    }

    @Test
    void theCoreStartsWithoutWindowNorGL(@TempDir Path resources) {
        Engine started = start(resources);

        assertTrue(started.isHeadless());
        assertNull(started.sceneManager());
        assertNull(started.glResources());
        assertNull(started.contexts().getMainWindow());
        assertNotNull(started.scenes());
        // The console shows itself in a window: none
        assertNull(started.console());
    }

    @Test
    void theCommandsLoadersAndServicesOfThePluginsExist(@TempDir Path resources) {
        Engine started = start(resources);

        CommandResult result = started.commands().execute("test:hello");
        assertTrue(result.isSuccess(), result.toString());
        assertEquals("hello", result.getMessage());

        assertTrue(started.registrations().loaders().contains(loader));
        assertTrue(started.hasService(COUNTER));
        assertInstanceOf(SilentSoundService.class, started.sound());
    }

    @Test
    void ticksAndFramesRunWithoutWindow(@TempDir Path resources) {
        Engine started = start(resources);

        MainLoop.tick(started);
        MainLoop.frame(started, System.nanoTime());
        MainLoop.tick(started);

        assertEquals(2, started.service(COUNTER).updates);
    }
}
