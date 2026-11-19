package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.main.WorkersTestAccess;
import io.github.mightycode.mightylib.resources.config.ConfigData;
import io.github.mightycode.mightylib.resources.config.ConfigDataLoader;
import io.github.mightycode.mightylib.resources.internal.BatchLoader;
import io.github.mightycode.mightylib.resources.internal.BatchResources;
import io.github.mightycode.mightylib.resources.source.FilteredSource;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigData: defaults of the engine and the game merged deeply, the player's layer above them (configs/ of its data
 * folder), written atomically, read again when a file changes.
 */
class ConfigDataLayersTest {
    private static final String NAME = "test/display";

    @TempDir
    Path engine;
    @TempDir
    Path game;
    @TempDir
    Path player;

    private Resources resources;
    private ResourceWatcher watcher;

    @BeforeEach
    void setUp() throws IOException {
        MainThreadTestAccess.setCurrentAsMain();
        WorkersTestAccess.ensureThreads(2);

        write(engine, "{\"gamma\": 1, \"video\": {\"width\": 800, \"vsync\": true}, \"name\": \"engine\"}", 1_000_000);
        write(game, "{\"video\": {\"width\": 1024}, \"name\": \"game\"}", 1_000_000);
        write(player, "{\"gamma\": 1.5}", 1_000_000);
        Files.createDirectories(player.resolve("saves"));
        Files.createDirectories(game.resolve("batchs"));
        Files.write(game.resolve("batchs").resolve("configs.batch"),
                ("{\"batchs\":[{\"type\":\"ConfigData\",\"files\":[\"" + NAME + "\"],\"regex\":[]}]}")
                        .getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        if (watcher != null)
            watcher.stop();
    }

    private static void write(Path root, String content, long modified) throws IOException {
        Path file = root.resolve(ConfigData.PathOf(NAME));
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        Files.setLastModifiedTime(file, FileTime.fromMillis(modified));
    }

    private ConfigData load() throws InterruptedException {
        ResourceSources sources = ResourceSources.builder()
                .add("mightylib", new FolderSource(engine))
                .add(ResourceSources.GAME_OWNER, new FolderSource(game))
                .add(ResourceSources.PLAYER_OWNER, new FilteredSource(new FolderSource(player), ConfigDataLoader.FOLDER))
                .build();
        resources = new Resources(new Resources.BatchResourcesMethod("unused"), 2, sources,
                Arrays.asList(new ConfigDataLoader(), new BatchLoader()));
        resources.init();
        resources.loadAllOfType(BatchResources.class);
        resources.loadBatch("configs");
        pumpUntilIdle();

        ConfigData config = resources.getResource(ConfigData.class, NAME);
        assertTrue(config.isLoaded());
        return config;
    }

    private void pump(long milliseconds) throws InterruptedException {
        long end = System.currentTimeMillis() + milliseconds;
        while (System.currentTimeMillis() < end) {
            resources.process(2);
            MainThreadTestAccess.processAll();
            Thread.sleep(2);
        }
    }

    private void pumpUntilIdle() throws InterruptedException {
        long end = System.currentTimeMillis() + 10_000;
        do {
            resources.process(2);
            MainThreadTestAccess.processAll();
            Thread.sleep(2);
        } while (!resources.finishedCurrentLoading() && System.currentTimeMillis() < end);
    }

    private JSONObject playerFile() throws IOException {
        return new JSONObject(Files.readString(player.resolve(ConfigData.PathOf(NAME))));
    }

    @Test
    void layersAreMergedDeeplyAndThePlayerWins() throws InterruptedException {
        ConfigData config = load();

        assertEquals(1.5f, config.getFloat("gamma", 0f));
        assertEquals(1024, config.getInt("video.width", 0));
        assertTrue(config.getBoolean("video.vsync", false));
        assertEquals("game", config.getString("name", null));
        assertEquals(7, config.getInt("missing", 7));
        assertEquals(7, config.getInt("video.missing.deep", 7));
        // Wrong type: the default
        assertEquals(3, config.getInt("name", 3));

        assertEquals(1, config.playerValues().length());
        assertEquals(1f, config.defaultValues().getFloat("gamma"));
        assertEquals(3, config.layers().size());
        assertEquals(player.resolve(ConfigData.PathOf(NAME)).toAbsolutePath().normalize(), config.playerFile());
    }

    @Test
    void setWritesOnlyThePlayersLayerAtomically() throws Exception {
        ConfigData config = load();

        config.set("video.vsync", false).get(5, TimeUnit.SECONDS);
        assertFalse(config.getBoolean("video.vsync", true));
        assertEquals(1024, config.getInt("video.width", 0));

        JSONObject written = playerFile();
        assertEquals(config.playerValues().toString(), written.toString());
        assertFalse(written.getJSONObject("video").has("width"));
        try (Stream<Path> files = Files.list(player.resolve("configs/test"))) {
            assertEquals(1, files.count(), "no temporary file left");
        }

        // The default of the sources comes back
        config.remove("gamma").get(5, TimeUnit.SECONDS);
        assertEquals(1f, config.getFloat("gamma", 0f));
        assertFalse(playerFile().has("gamma"));

        assertThrows(IllegalArgumentException.class, () -> config.set("gamma", new Object()));
        assertThrows(IllegalArgumentException.class, () -> config.set("a..b", 1));
    }

    // Several changes in a row: the file holds the last one
    @Test
    void theLastWriteWins() throws Exception {
        ConfigData config = load();
        for (int i = 0; i < 20; ++i)
            config.set("count", i);
        config.set("count", 99).get(5, TimeUnit.SECONDS);

        assertEquals(99, playerFile().getInt("count"));
    }

    @Test
    void aLayerThatCantBeReadIsIgnored() throws Exception {
        write(game, "{ not json", 2_000_000);
        ConfigData config = load();

        assertEquals(1, config.invalidLayers().size());
        assertTrue(config.invalidLayers().get(0).contains(ConfigData.PathOf(NAME)));
        assertEquals(800, config.getInt("video.width", 0));
        assertEquals(1.5f, config.getFloat("gamma", 0f));
    }

    // The player's file is kept as it is: nothing is written over it
    @Test
    void anInvalidPlayersLayerIsNotOverwritten() throws Exception {
        write(player, "{ broken", 2_000_000);
        ConfigData config = load();

        config.set("gamma", 2).get(5, TimeUnit.SECONDS);
        assertEquals(2f, config.getFloat("gamma", 0f));
        assertEquals("{ broken", Files.readString(player.resolve(ConfigData.PathOf(NAME))));
    }

    @Test
    void aChangedFileIsReadAgain() throws Exception {
        ConfigData config = load();
        int version = config.getVersion();

        watcher = resources.watcher();
        watcher.configure(ResourceWatcher.Mode.POLLING, 50);
        watcher.watchType(ConfigData.class);
        pump(1500);

        write(player, "{\"gamma\": 2.5}", 3_000_000);
        long end = System.currentTimeMillis() + 10_000;
        while (config.getVersion() == version && System.currentTimeMillis() < end)
            pump(50);
        pumpUntilIdle();

        assertEquals(2.5f, config.getFloat("gamma", 0f));
        assertEquals(1024, config.getInt("video.width", 0));
    }
}
