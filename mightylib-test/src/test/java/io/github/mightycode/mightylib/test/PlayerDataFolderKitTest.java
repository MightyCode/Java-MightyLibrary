package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.resources.config.ConfigData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

// The data of the player of a test engine: a temporary folder per engine, deleted at close; a folder given is kept
class PlayerDataFolderKitTest {
    private static final String NAME = "kit";

    private static void configFile(Path game) throws IOException {
        Path file = game.resolve(ConfigData.PathOf(NAME));
        Files.createDirectories(file.getParent());
        Files.write(file, "{\"volume\": 1}".getBytes(StandardCharsets.UTF_8));
    }

    // Writes a value of the player and waits for the file
    private static void write(TestEngine engine, int volume)
            throws InterruptedException, ExecutionException, TimeoutException {
        ConfigData config = engine.resources().getResource(ConfigData.class, NAME);
        assertNotNull(config);
        assertTrue(config.isLoaded());
        config.set("volume", volume).get(10, TimeUnit.SECONDS);
    }

    @Test
    void theWritesGoToATemporaryFolderDeletedAtClose(@TempDir Path game) throws Exception {
        configFile(game);
        TestEngine engine = MightyTest.headless(HeadlessTestEngineTest.config(game)).start().loadUntilDone();
        Path folder = engine.playerDataFolder();
        try {
            assertTrue(folder.getFileName().toString().startsWith(TestEngine.PLAYER_DATA_PREFIX), folder.toString());
            assertEquals(folder.toString(), engine.config().getPlayerDataFolder());

            write(engine, 2);
            Path written = folder.resolve(ConfigData.PathOf(NAME));
            assertTrue(Files.exists(written), written.toString());
            assertTrue(new String(Files.readAllBytes(written), StandardCharsets.UTF_8).contains("2"));
            // Never in the data folder of the module (working folder of the tests)
            assertFalse(Files.exists(Paths.get("data").resolve(ConfigData.PathOf(NAME))));
        } finally {
            engine.close();
        }

        assertFalse(Files.exists(folder), folder + " is still there");
        // Still answered once closed
        assertEquals(folder, engine.playerDataFolder());
    }

    @Test
    void aFolderGivenByTheConfigurationIsUsedAndKept(@TempDir Path game, @TempDir Path player) throws Exception {
        configFile(game);
        try (TestEngine engine = MightyTest.headless(HeadlessTestEngineTest.config(game)
                .setPlayerDataFolder(player.toString())).start().loadUntilDone()) {
            assertEquals(player, engine.playerDataFolder());
            write(engine, 3);
        }

        Path written = player.resolve(ConfigData.PathOf(NAME));
        assertTrue(Files.exists(written), written + " was deleted");
        assertTrue(new String(Files.readAllBytes(written), StandardCharsets.UTF_8).contains("3"));
    }

    @Test
    void twoEnginesHaveTwoFolders(@TempDir Path game) {
        Path first;
        Path second;
        try (TestEngine engine = MightyTest.headless(HeadlessTestEngineTest.config(game)).start()) {
            first = engine.playerDataFolder();
        }
        try (TestEngine engine = MightyTest.headless(HeadlessTestEngineTest.config(game)).start()) {
            second = engine.playerDataFolder();
            assertTrue(Files.isDirectory(second));
        }

        assertNotEquals(first, second);
        assertFalse(Files.exists(first));
        assertFalse(Files.exists(second));
    }

    @Test
    void theFolderIsChosenAtStart(@TempDir Path game) {
        TestEngine engine = MightyTest.headless(HeadlessTestEngineTest.config(game));
        assertThrows(IllegalStateException.class, engine::playerDataFolder);
        engine.close();
    }
}
