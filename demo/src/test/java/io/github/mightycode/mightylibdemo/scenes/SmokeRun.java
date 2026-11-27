package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.test.SceneRunner;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

// The run of the smoke test shared by its strict and lenient classes
final class SmokeRun {
    private SmokeRun() {}

    static List<String> Scenes(boolean lenient) {
        return DemoScenes.ALL.stream()
                .filter(entry -> SmokeCases.of(entry.type()).lenient == lenient)
                .map(entry -> DemoScenes.RelativeName(entry.type()))
                .collect(Collectors.toList());
    }

    private static Class<? extends Scene> TypeOf(String name) {
        for (DemoScenes.Entry<?> entry : DemoScenes.ALL) {
            if (DemoScenes.RelativeName(entry.type()).equals(name))
                return entry.type();
        }
        throw new IllegalArgumentException("No scene " + name + " in DemoScenes");
    }

    /**
     * The scene, then the empty scene, then the scene again, each loaded and run for the steps of its case; the
     * extension of the kit goes back to the empty scene and checks the leaks and the errors.
     */
    static void Run(SceneRunner runner, String name) {
        SmokeCases.Case smokeCase = SmokeCases.of(TypeOf(name));
        assumeTrue(smokeCase.knownBug == null, () -> "Known bug: " + smokeCase.knownBug);
        if (smokeCase.demoAssets)
            DemoAssets.assumePresent();

        for (int i = 0; i < smokeCase.expectedLevels.size(); ++i)
            runner.engine().expectProblem(smokeCase.expectedLevels.get(i), smokeCase.expectedPatterns.get(i));

        long start = System.nanoTime();
        for (int round = 0; round < 2; ++round) {
            if (round > 0)
                runner.toEmptyScene();
            runner.start(name, smokeCase.args).untilLoaded();
            runner.frames(smokeCase.steps);
            if (smokeCase.afterSteps != null)
                smokeCase.afterSteps.accept(runner);
        }
        System.out.println("--Smoke " + name + ": " + (System.nanoTime() - start) / 1_000_000 + " ms");
    }
}
