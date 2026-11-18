package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.main.DisplaySettings;
import io.github.mightycode.mightylib.main.EWindowMode;
import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.scenes.Scene;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

// The engine with a hidden window: a scene of the tests loaded, frames, a simulated key, no leak
@MightySceneTest(config = SceneRunnerTest.Config.class)
class SceneRunnerTest {
    // Counts the frames where SPACE was pressed
    public static final class KeyScene extends Scene {
        int pressed;

        @Override
        public void update() {
            super.update();
            if (context().getInputManager().getKeyboardManager().keyPressed(GLFW.GLFW_KEY_SPACE))
                ++pressed;
        }
    }

    static final class Plugin implements IPlugin {
        @Override
        public String namespace() {
            return "kitscenes";
        }

        @Override
        public void register(EngineRegistry registry) {
            registry.scenes().register(Identifier.of("kitscenes", "key_scene"), KeyScene.class, KeyScene::new,
                    "Counts the presses of SPACE");
        }
    }

    static final class Config implements Supplier<EngineConfig> {
        @Override
        public EngineConfig get() {
            // The display of a game in borderless fullscreen with vsync: the kit makes it a windowed one without vsync
            return new EngineConfig()
                    .setResourcesFolder(Paths.get("target", "no-game-folder").toString())
                    .setDisplay(new DisplaySettings(EWindowMode.BORDERLESS).setVsync(true))
                    .addPlugin(new Plugin());
        }
    }

    @Test
    void theWindowIsWindowedWithoutVsync(TestEngine engine) {
        DisplaySettings display = engine.engine().contexts().requireMainWindow().getWindow().getDisplay();
        assertEquals(EWindowMode.WINDOWED, display.getMode());
        assertFalse(display.isVsync());
    }

    @Test
    void aSceneIsLoadedAndReadsASimulatedKey(SceneRunner runner) {
        runner.start("key_scene").untilLoaded();
        KeyScene scene = assertInstanceOf(KeyScene.class, runner.scene());

        runner.frames(2);
        assertEquals(0, scene.pressed);
        runner.key(GLFW.GLFW_KEY_SPACE, 1).frames(3);
        assertEquals(1, scene.pressed);
    }

    @Test
    void theEngineStartsOnTheEmptyScene(SceneRunner runner, TestEngine engine) {
        assertInstanceOf(EmptyTestScene.class, runner.scene());
        assertSame(engine, runner.engine());
        assertTrue(engine.execute("gl").isSuccess());
    }

    @Test
    void aLeakIsNamed(SceneRunner runner) {
        Map<String, Integer> before = runner.leakSnapshot();
        Map<String, Integer> changed = new java.util.TreeMap<>(before);
        changed.merge("gl.Texture", 1, Integer::sum);

        AssertionError error = assertThrows(AssertionError.class, () -> runner.assertNoLeak(changed));
        assertTrue(error.getMessage().contains("gl.Texture"), error.getMessage());
    }
}
