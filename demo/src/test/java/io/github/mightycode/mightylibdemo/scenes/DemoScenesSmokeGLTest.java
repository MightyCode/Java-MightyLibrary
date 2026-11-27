package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.test.MightySceneTest;
import io.github.mightycode.mightylib.test.SceneRunner;
import io.github.mightycode.mightylibdemo.main.DemoTestConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

/**
 * Every scene of DemoScenes is loaded twice (scene, empty scene, scene) and run for some steps: no exception, no leak
 * and no error of the Logger channel. The particular cases are in SmokeCases, with their reason; the scenes whose
 * resources fail on purpose run in DemoScenesLenientSmokeGLTest.
 */
@MightySceneTest(config = DemoTestConfig.class)
class DemoScenesSmokeGLTest {
    static List<String> scenes() {
        return SmokeRun.Scenes(false);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenes")
    void loadsTwiceWithoutLeakNorError(String scene, SceneRunner runner) {
        SmokeRun.Run(runner, scene);
    }
}
