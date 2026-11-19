package io.github.mightycode.mightylib.graphics.color;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.main.WorkersTestAccess;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.config.ConfigData;
import io.github.mightycode.mightylib.resources.config.ConfigDataLoader;
import io.github.mightycode.mightylib.resources.source.FilteredSource;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The settings of the picture read from the configuration mightylib/display, the player's written to its layer
class ColorServiceConfigTest {
    @TempDir
    Path game;
    @TempDir
    Path player;

    private Resources resources;
    private ColorService service;

    @BeforeEach
    void setUp() throws IOException {
        MainThreadTestAccess.setCurrentAsMain();
        WorkersTestAccess.ensureThreads(2);
        write(game, "{\"gamma\": 1.2, \"brightness\": 1, \"contrast\": 9}");
    }

    private static void write(Path root, String content) throws IOException {
        Path file = root.resolve(ConfigData.PathOf(IColorService.DISPLAY_CONFIG));
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private void start() throws InterruptedException {
        ResourceSources sources = ResourceSources.builder()
                .add(ResourceSources.GAME_OWNER, new FolderSource(game))
                .add(ResourceSources.PLAYER_OWNER, new FilteredSource(new FolderSource(player), ConfigDataLoader.FOLDER))
                .build();
        resources = new Resources(new Resources.AllResourcesMethod(), 2, sources, List.of(new ConfigDataLoader()));
        service = new ColorService(ERenderColorSpace.GAMMA, resources);

        // Nothing before the data exist
        service.update();
        assertNull(service.config());

        resources.init();
        long end = System.currentTimeMillis() + 10_000;
        do {
            service.update();
            resources.process(2);
            MainThreadTestAccess.processAll();
            Thread.sleep(2);
        } while ((service.config() == null || !service.config().isLoaded()) && System.currentTimeMillis() < end);
        service.update();
    }

    @Test
    void readFromTheConfigurationAndOutOfBoundsIgnored() throws Exception {
        write(player, "{\"brightness\": 0.8}");
        start();

        assertEquals(PictureSettings.of(1.2f, 0.8f, 1f), service.picture());
    }

    // Only what differs from the settings of the game goes to the player's layer
    @Test
    void thePlayersSettingsAreSaved() throws Exception {
        start();

        service.setPicture(service.picture().withBrightness(1.5f));
        waitWritten();
        JSONObject written = playerFile();
        assertEquals(1.5f, written.getFloat("brightness"));
        assertFalse(written.has("gamma"), written.toString());
        assertFalse(written.has("contrast"), written.toString());

        // Back to the value of the game: the key leaves the player's layer
        service.setPicture(service.picture().withBrightness(1f).withGamma(2f));
        waitWritten();
        written = playerFile();
        assertFalse(written.has("brightness"), written.toString());
        assertEquals(2f, written.getFloat("gamma"));

        service.resetPicture();
        waitWritten();
        assertEquals(0, playerFile().length());
        assertEquals(PictureSettings.of(1.2f, 1f, 1f), service.picture());
    }

    @Test
    void changeOfTheFileIsAppliedAtTheNextUpdate() throws Exception {
        start();
        write(player, "{\"contrast\": 2}");
        resources.reload(service.config());
        long end = System.currentTimeMillis() + 10_000;
        while (service.picture().contrast() != 2f && System.currentTimeMillis() < end) {
            resources.process(2);
            MainThreadTestAccess.processAll();
            service.update();
            Thread.sleep(2);
        }

        assertEquals(2f, service.picture().contrast());
    }

    private JSONObject playerFile() throws IOException {
        return new JSONObject(Files.readString(player.resolve(ConfigData.PathOf(IColorService.DISPLAY_CONFIG))));
    }

    // The writes run on the workers, in order: the last one is done once a new empty one is
    private void waitWritten() throws Exception {
        service.config().set("sync", 0).get();
        service.config().remove("sync").get();
    }
}
