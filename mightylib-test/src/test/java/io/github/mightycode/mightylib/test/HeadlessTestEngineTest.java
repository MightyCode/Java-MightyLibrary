package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.main.KitEngineAccess;
import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandCondition;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

// The headless engine of the kit: started as a game, driven by the test, its process-wide state reset when closed
class HeadlessTestEngineTest {
    private static final float TICK = 1f / 50f;

    // A game of the tests: one command that answers the current time
    static final class Game implements IPlugin {
        @Override
        public String namespace() {
            return "kitgame";
        }

        @Override
        public void register(EngineRegistry registry) {
            registry.commands().register(new BaseCommand(Identifier.of("kitgame", "time"), ECommandLevel.USER,
                    "Answers the time of the game") {
                @Override
                protected void syntax(CommandNode root) {
                    root.executes(context -> CommandResult.ok(Float.toString(GameTime.currentTime())));
                }
            });
        }
    }

    static EngineConfig config(Path gameFolder) {
        return new EngineConfig()
                .setResourcesFolder(gameFolder.toString())
                .setTps(50)
                .setResourcesLoadingMethod(new Resources.AllResourcesMethod())
                .addPlugin(new Game());
    }

    private static void image(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", file.toFile());
    }

    @Test
    void theFolderOfTheTestIsAboveTheGame(@TempDir Path game, @TempDir Path test) throws IOException {
        image(test.resolve("textures/kit_dot.png"));
        Files.write(test.resolve("textures/textures.json"),
                "{\"textures\": {\"kit_dot\": {\"file\": \"kit_dot.png\", \"qualityType\": \"realistic\"}}}".getBytes(StandardCharsets.UTF_8));

        try (TestEngine engine = MightyTest.headless(config(game)).addSource(test).start().loadUntilDone()) {
            TextureData dot = engine.resources().getResource(TextureData.class, "kit_dot");
            assertNotNull(dot);
            assertTrue(dot.isLoaded());
            assertNotNull(engine.glElementRequests());
            assertTrue(KitEngineAccess.isStrictResources(engine.engine()));
        }
    }

    @Test
    void theCommandsOfTheGameRun(@TempDir Path game) {
        try (TestEngine engine = MightyTest.headless(config(game)).start()) {
            CommandResult result = engine.execute("kitgame:time");
            assertTrue(result.isSuccess(), result.toString());
            assertEquals(Float.toString(engine.time()), result.getMessage());
        }
    }

    @Test
    void theTimeOnlyMovesWithTheTest(@TempDir Path game) {
        try (TestEngine engine = MightyTest.headless(config(game)).start()) {
            float start = engine.time();
            engine.step();
            assertEquals(TICK, GameTime.DeltaTime(), 1e-6);

            engine.advance(0.5f).step();
            assertEquals(0.5f + TICK, GameTime.DeltaTime(), 1e-5);
            assertEquals(start + 0.5f + 2 * TICK, engine.time(), 1e-5);
            assertEquals(engine.time(), GameTime.currentTime(), 1e-5);
            assertEquals(TICK, GameTime.FixedDeltaTime(), 1e-6);
        }
    }

    @Test
    void oneEngineAtATimeAndTheStateIsResetWhenClosed(@TempDir Path game) throws Exception {
        TestEngine first = MightyTest.headless(config(game)).start();
        assertThrows(IllegalStateException.class, () -> MightyTest.headless(config(game)).start());
        first.close();
        first.close();

        // No main thread anymore: any thread is accepted, as before any engine
        AtomicReference<Boolean> otherThreadIsMain = new AtomicReference<>();
        Thread other = new Thread(() -> otherThreadIsMain.set(MainThread.isMainThread()));
        other.start();
        other.join();
        assertTrue(otherThreadIsMain.get());
        assertEquals(1f / 60f, GameTime.FixedDeltaTime(), 1e-6);

        try (TestEngine second = MightyTest.headless(config(game)).start()) {
            assertTrue(second.step(3).execute("kitgame:time").isSuccess());
        }
    }

    // Without window, a command that needs one (or GL) answers so: none fails on what the engine lacks
    @Test
    void noCommandFailsWithoutWindow(@TempDir Path game) {
        try (TestEngine engine = MightyTest.headless(config(game)).start()) {
            List<String> failures = new ArrayList<>();
            for (BaseCommand command : engine.engine().commands().getCommands()) {
                // exit stops the engine, wait waits for ticks the test does not run
                String name = command.getName();
                if (name.equals("exit") || name.equals("wait"))
                    continue;
                CommandResult result = engine.execute(name);
                if (result.getMessage() != null && result.getMessage().contains(" failed : "))
                    failures.add(result.getMessage());
            }
            assertEquals(List.of(), failures);

            CommandResult gl = engine.execute("gl");
            assertFalse(gl.isSuccess());
            assertTrue(gl.getMessage().contains(CommandCondition.NO_WINDOW), gl.getMessage());
            assertTrue(engine.execute("stats").isSuccess());
            assertTrue(engine.execute("services").isSuccess());
        }
    }

    // The kit changes the configuration it starts from: a second engine on it is refused with a clear message
    @Test
    void aConfigurationStartsOneEngine(@TempDir Path game, @TempDir Path test) {
        EngineConfig config = config(game);
        MightyTest.headless(config).addSource(test).start().close();
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> MightyTest.headless(config).addSource(test).start());
        assertTrue(error.getMessage().contains("already started"), error.getMessage());

        // The refused engine took no lock: the next one starts
        MightyTest.headless(config(game)).start().close();
    }

    @Test
    void aStepNeedsAStartedEngine(@TempDir Path game) {
        TestEngine engine = MightyTest.headless(config(game));
        assertThrows(IllegalStateException.class, engine::step);
        engine.start();
        assertThrows(IllegalStateException.class, () -> engine.addSource(game));
        engine.close();
        assertThrows(IllegalStateException.class, engine::step);
    }
}
