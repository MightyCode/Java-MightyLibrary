package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.test.MightySceneTest;
import io.github.mightycode.mightylib.test.SceneRunner;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.DemoTestConfig;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example of a test with GL (tag gl, hidden window): Scene2D loaded, frames, a simulated action. After the test the
 * kit goes back to its empty scene and fails if Scene2D left GL objects or resources behind.
 */
@MightySceneTest(config = DemoTestConfig.class)
class Scene2DTest {
    @Test
    void theCameraMovesWithTheAction(SceneRunner runner) {
        runner.start("Scene2D").untilLoaded();
        Scene2D scene = assertInstanceOf(Scene2D.class, runner.scene());
        runner.frames(5);

        Vector2f before = scene.camera2D().getCamPosCopy();
        runner.press(ActionId.MOVE_RIGHT_2D, 10).frames(12);
        Vector2f after = scene.camera2D().getCamPosCopy();

        assertNotEquals(before, after);
    }

    @Test
    void escapeGoesBackToTheMenu(SceneRunner runner) {
        runner.start("Scene2D").untilLoaded();
        runner.press(ActionId.ESCAPE, 1).frames(2).untilLoaded();
        assertInstanceOf(MenuScene.class, runner.scene());
    }
}
