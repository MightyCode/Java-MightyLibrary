package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.ELogLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A scene whose resource fails while it loads, as in the game (resources not strict): the failure is a warning, the
 * loading ends, the scene is launched with the fallback of the resource, and releases it when it is left.
 */
@MightySceneTest(config = SceneRunnerTest.Config.class, strictResources = false)
class SceneLoadingFailureGLTest {
    private static final String BROKEN = "lifecycleBrokenTexture";

    static final class BrokenResourceScene extends Scene {
        boolean failedAtLaunch;
        boolean released;

        @Override
        public void init(String[] args) {
            // Read by its loader, it fails: a missing file
            resources().addResourceAndSetUp(TextureData.class,
                    new TextureData(BROKEN, "textures/missing/lifecycleBroken.png"));
        }

        @Override
        public void launch(String[] args) {
            failedAtLaunch = resources().getResource(TextureData.class, BROKEN).hasFailed();
        }

        @Override
        public void release() {
            resources().deleteResource(TextureData.class, BROKEN);
            released = true;
        }
    }

    @Test
    void aFailedResourceIsAWarningAndTheSceneIsLaunched(SceneRunner runner) {
        runner.engine().expectProblem(ELogLevel.WARNING, "lifecycleBroken");
        BrokenResourceScene scene = new BrokenResourceScene();
        runner.start(() -> scene).untilLoaded();
        runner.frames(2);

        assertTrue(scene.isLaunched());
        assertTrue(scene.failedAtLaunch, "the resource failed before launch");

        runner.toEmptyScene().untilLoaded();
        assertTrue(scene.released);
        assertFalse(runner.engine().resources().isExistingResource(TextureData.class, BROKEN));
    }
}
