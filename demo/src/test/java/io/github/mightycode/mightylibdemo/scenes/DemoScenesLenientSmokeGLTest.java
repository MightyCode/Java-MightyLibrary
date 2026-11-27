package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.test.MightySceneTest;
import io.github.mightycode.mightylib.test.SceneRunner;
import io.github.mightycode.mightylibdemo.main.DemoTestConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

// The smoke test of the scenes whose resources fail on purpose (SmokeCases, lenient): their fallbacks are warnings
@MightySceneTest(config = DemoTestConfig.class, strictResources = false)
class DemoScenesLenientSmokeGLTest {
    static List<String> scenes() {
        return SmokeRun.Scenes(true);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenes")
    void loadsTwiceWithoutLeakNorError(String scene, SceneRunner runner) {
        SmokeRun.Run(runner, scene);
    }
}
