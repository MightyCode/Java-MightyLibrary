package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources.EGraphicResources;
import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.scenes.Scene;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static io.github.mightycode.mightylib.test.BatchPictures.inScene;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// A GL texture kept by the code after its CPU data was unloaded with its batch: deleted when the code releases it
@MightySceneTest(config = TextureUnloadGLTest.Config.class, sources = "src/test/resources/texture-unload")
class TextureUnloadGLTest {
    private static final String BATCH = "unloadpainting";
    private static final String PAINTING = "unloadedPainting";

    static final class Config implements Supplier<EngineConfig> {
        @Override
        public EngineConfig get() {
            return new SceneRunnerTest.Config().get()
                    .setResourcesLoadingMethod(new Resources.BatchResourcesMethod("unloadstart"));
        }
    }

    private static int glTextures() {
        return GenDeleteResources.Count(EGraphicResources.Texture);
    }

    @Test
    void aTextureKeptAfterItsDataIsUnloadedIsDeletedWhenReleased(SceneRunner runner) {
        inScene(runner, scene -> {
            int before = glTextures();
            Resources resources = resourcesOf(scene);
            GLResources glResources = glResourcesOf(scene);

            // The batch reads the data and sends its texture
            run(scene, () -> resources.loadBatch(BATCH));
            runner.engine().loadUntilDone();
            TextureData data = get(scene, () -> resources.getResource(TextureData.class, PAINTING));
            assertTrue(data.isLoaded());
            assertEquals(before + 1, glTextures(), "the texture of the batch");

            // The code shares it, then the batch is unloaded: the data goes, the texture stays for the code
            Texture kept = get(scene, () -> glResources.addElementOrReturnIfPresent(Texture.class, new Texture(data),
                    data.glElementName()));
            assertTrue(kept.isLoaded());
            run(scene, () -> resources.unloadBatch(BATCH));
            runner.engine().loadUntilDone();
            assertFalse(data.isLoaded(), "the data is unloaded with its batch");
            assertSame(kept, get(scene, () -> glResources.getResource(Texture.class, data.glElementName())));
            assertEquals(before + 1, glTextures(), "kept by the code");

            // Released by the code: its GL texture is deleted although its data is already unloaded
            run(scene, () -> glResources.releaseResource(kept));
            assertFalse(kept.isLoaded());
            assertEquals(before, glTextures(), "the GL texture is deleted");
        });
    }

    private static Resources resourcesOf(Scene scene) {
        return get(scene, () -> Views.engine().resources());
    }

    private static GLResources glResourcesOf(Scene scene) {
        return get(scene, () -> Views.engine().glResources());
    }

    private static void run(Scene scene, Runnable action) {
        try (Views.Scope ignored = Views.enter(scene)) {
            action.run();
        }
    }

    private static <T> T get(Scene scene, Supplier<T> value) {
        try (Views.Scope ignored = Views.enter(scene)) {
            return value.get();
        }
    }
}
