package io.github.mightycode.mightylibdemo.resources;

import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.test.MightyTest;
import io.github.mightycode.mightylib.test.TestEngine;
import io.github.mightycode.mightylibdemo.main.DemoPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The layered pictures of the demo: found by folder, layers sorted by name and stacked, a bad layer named.
 * A headless engine of the test kit with the plugin of the demo, on a folder of the test as the folder of the game.
 */
class LayeredTextureLoaderTest {
    private static final int RED = 0xFFFF0000;
    private static final int BLUE = 0xFF0000FF;

    @TempDir
    Path folder;

    private Path layered;
    private TestEngine engine;

    @BeforeEach
    void setUp() {
        layered = folder.resolve(LayeredTextureLoader.FOLDER);
    }

    @AfterEach
    void stop() {
        if (engine != null)
            engine.close();
    }

    // A layer of the given size filled with the color, transparent elsewhere
    private void layer(String data, String file, int width, int height, int filledWidth, int color) throws IOException {
        Path dataFolder = layered.resolve(data);
        Files.createDirectories(dataFolder);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < filledWidth; ++x)
                image.setRGB(x, y, color);
        }
        ImageIO.write(image, "png", dataFolder.resolve(file).toFile());
    }

    private String path(String data, String file) {
        return LayeredTextureLoader.FOLDER + "/" + data + "/" + file;
    }

    // The engine started on the folder, its resources loaded; a failing layer gives the fallback, as in the game
    private Resources load() {
        engine = MightyTest.headless(new EngineConfig()
                        .setResourcesFolder(folder.toString())
                        .setResourcesLoadingMethod(new Resources.AllResourcesMethod())
                        .addPlugin(new DemoPlugin()))
                .allowResourceFailures()
                .start()
                .loadUntilDone();
        return engine.resources();
    }

    @Test
    void eachFolderIsADataWithItsLayersSortedByName() throws IOException {
        // Written in the opposite order: the name gives the order
        layer("hero", "2_top.png", 2, 2, 1, RED);
        layer("hero", "1_bottom.png", 4, 2, 4, BLUE);
        Files.write(layered.resolve("hero").resolve("notes.txt"), "not a layer".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(layered.resolve("empty"));

        Resources resources = load();
        LayeredTextureData hero = resources.getResource(LayeredTextureData.class, "hero");

        assertEquals(List.of(path("hero", "1_bottom.png"), path("hero", "2_top.png")), hero.sourcePaths());
        assertFalse(resources.isExistingResource(LayeredTextureData.class, "empty"));
        assertTrue(hero.isLoaded());
        assertTrue(hero.isSourceRead(0) && hero.isSourceRead(1));

        // The size of the biggest layer, the top layer over the bottom one
        assertEquals(4, hero.getWidth());
        assertEquals(2, hero.getHeight());
        assertEquals(RED, hero.image().getPixel(0, 0));
        assertEquals(BLUE, hero.image().getPixel(1, 0));
        assertEquals(BLUE, hero.image().getPixel(3, 1));
    }

    @Test
    void anUnreadableLayerIsNamed() throws IOException {
        layer("hero", "1_body.png", 2, 2, 2, BLUE);
        Files.createDirectories(layered.resolve("hero"));
        Files.write(layered.resolve("hero").resolve("2_hat.png"), "not a picture".getBytes(StandardCharsets.UTF_8));

        PrintStream previous = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        Resources resources;
        try {
            resources = load();
        } finally {
            System.setErr(previous);
        }

        LayeredTextureData hero = resources.getResource(LayeredTextureData.class, "hero");
        assertTrue(hero.hasFailed());
        assertTrue(hero.isSourceRead(0));
        assertFalse(hero.isSourceRead(1));
        assertEquals(List.of(path("hero", "2_hat.png")), hero.unreadSources());
        String errors = captured.toString(StandardCharsets.UTF_8);
        assertTrue(errors.contains("not read: " + path("hero", "2_hat.png")), errors);
    }

    // Its own fallback, drawn by the code
    @Test
    void theFallbackIsBlueStripes() {
        Resources resources = load();
        LayeredTextureData fallback = resources.getFallback(LayeredTextureData.class);

        assertInstanceOf(FallbackLayeredTextureData.class, fallback);
        assertTrue(fallback.isLoaded());
        assertFalse(fallback.hasSource());
        assertEquals(32, fallback.getWidth());
        assertNotEquals(fallback.image().getPixel(0, 0), fallback.image().getPixel(4, 0));
        assertSame(fallback, resources.getResourceOrFallback(LayeredTextureData.class, "unknown"));
    }
}
