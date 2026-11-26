package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.main.WorkersTestAccess;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.LoaderTestAccess;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.internal.BatchLoader;
import io.github.mightycode.mightylib.resources.texture.TextureDataLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A Resources of the tests of the animations, without engine: the textures, the animations and their fallbacks.
 * A sheet "hero" of 32 x 16 pixels; "walk": 3 frames of 0.25, 0.5 and 1 s (exact in binary); "run": 2 frames of
 * 0.5 s. Only the fallbacks are loaded at first; load() loads the animations and the sheet.
 */
final class AnimationResources {
    static final String WALK = "walk";
    static final String RUN = "run";
    static final int SHEET_WIDTH = 32;
    static final int SHEET_HEIGHT = 16;
    // Times of the frames of walk
    static final float FIRST = 0.25f;
    static final float SECOND = 0.5f;
    static final float THIRD = 1f;
    static final float WALK_TIME = FIRST + SECOND + THIRD;

    private static final long TIMEOUT_MILLISECONDS = 10_000;

    private final Resources resources;

    private AnimationResources(Resources resources) {
        this.resources = resources;
    }

    static AnimationResources create(Path folder) throws IOException {
        MainThreadTestAccess.setCurrentAsMain();
        WorkersTestAccess.ensureThreads(2);

        Files.createDirectories(folder.resolve("textures"));
        ImageIO.write(new BufferedImage(SHEET_WIDTH, SHEET_HEIGHT, BufferedImage.TYPE_INT_ARGB), "png",
                folder.resolve("textures/hero.png").toFile());
        write(folder.resolve("textures/textures.json"),
                "{\"textures\": {\"hero\": {\"file\": \"hero.png\", \"qualityType\": \"pixelart\"}}}");
        write(folder.resolve("animations/" + WALK + ".anim"),
                "hero\n3\n0,0,8,8,1,2,0.25\n8,0,8,8,3,4,0.5\n16,0,8,16,5,6,1\n");
        write(folder.resolve("animations/" + RUN + ".anim"), "hero\n2\n0,8,8,8,0,0,0.5\n8,8,8,8,0,0,0.5\n");
        // The first batch and that of the engine are empty: only the fallbacks at first
        write(folder.resolve("batchs/start.batch"), "{\"batchs\":[]}");
        write(folder.resolve("batchs/" + EngineResources.ENGINE_BATCH + ".batch"), "{\"batchs\":[]}");
        write(folder.resolve("batchs/animations.batch"), "{\"batchs\":["
                + "{\"type\":\"TextureData\",\"files\":[\"hero\"],\"regex\":[]},"
                + "{\"type\":\"AnimationData\",\"files\":[\"" + WALK + "\",\"" + RUN + "\"],\"regex\":[]}]}");

        // The texture loader first: its fallback is the texture of the fallback animation
        Resources resources = new Resources(new Resources.BatchResourcesMethod("start"), 2,
                LoaderTestAccess.sources(folder), List.of(new TextureDataLoader(), new AnimationDataLoader(),
                new BatchLoader()));
        resources.init();
        resources.load();
        return new AnimationResources(resources);
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    Resources resources() {
        return resources;
    }

    AnimationData data(String name) {
        return resources.getResource(AnimationData.class, name);
    }

    // Loads the animations and their sheet, plays the main loop until done
    AnimationResources load() throws InterruptedException {
        resources.loadBatch("animations");
        long end = System.currentTimeMillis() + TIMEOUT_MILLISECONDS;
        while (!resources.finishedCurrentLoading() || !data(WALK).isLoaded()) {
            if (System.currentTimeMillis() > end)
                fail("Animations still loading after " + TIMEOUT_MILLISECONDS + " ms");
            resources.process(2);
            MainThreadTestAccess.processAll();
            Thread.sleep(1);
        }
        MainThreadTestAccess.processAll();
        return this;
    }

    // A loaded animation of this name, played once or looping
    Animation animation(String name, boolean looping) {
        Animation animation = new Animation(resources);
        animation.init(data(name), looping);
        return animation;
    }
}
