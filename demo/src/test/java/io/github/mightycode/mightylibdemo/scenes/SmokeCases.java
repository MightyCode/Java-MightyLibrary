package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.test.SceneRunner;
import io.github.mightycode.mightylib.utils.ELogLevel;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.json.JSONArray;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The scenes of the demo the smoke test (DemoScenesSmokeGLTest) treats in a particular way, each with its reason. A
 * scene without entry is loaded twice with the default steps, strict resources, no input and no problem announced.
 */
final class SmokeCases {
    static final int DEFAULT_STEPS = 30;

    static final class Case {
        final Class<? extends Scene> type;
        final String reason;
        int steps = DEFAULT_STEPS;
        String[] args = {};
        boolean lenient;
        boolean demoAssets;
        // Not null: the scene is skipped with this reason (the bug is written in docs/later.md)
        String knownBug;
        final List<ELogLevel> expectedLevels = new ArrayList<>();
        final List<String> expectedPatterns = new ArrayList<>();
        // After the steps of each loading (simulated inputs, then frames)
        Consumer<SceneRunner> afterSteps;

        private Case(Class<? extends Scene> type, String reason) {
            this.type = type;
            this.reason = reason;
        }

        Case steps(int steps) {
            this.steps = steps;
            return this;
        }

        Case args(String... args) {
            this.args = args.clone();
            return this;
        }

        // Resources that fail on purpose: a lenient engine, the failures are warnings
        Case lenient() {
            lenient = true;
            return this;
        }

        // Needs the demonstration pack (python tools/demo_assets.py)
        Case demoAssets() {
            demoAssets = true;
            return this;
        }

        Case expect(ELogLevel level, String regex) {
            expectedLevels.add(level);
            expectedPatterns.add(regex);
            return this;
        }

        Case afterSteps(Consumer<SceneRunner> inputs) {
            afterSteps = inputs;
            return this;
        }

        Case knownBug(String bug) {
            knownBug = bug;
            return this;
        }
    }

    private static Case scene(Class<? extends Scene> type, String reason) {
        return new Case(type, reason);
    }

    static final List<Case> ALL = Collections.unmodifiableList(List.of(
            scene(FallbackScene.class, "Its resources fail on purpose to show their fallbacks").lenient(),
            scene(LayeredTexture2D.class, "Shows the fallback of an unknown layered picture and of an unknown texture")
                    .lenient(),
            scene(MultiWindowScene.class, "Its windows open on keys: the tool window (T), a second context (C) and the"
                    + " modal dialog (M), closed with the scene (windows and contexts are in the leak check)")
                    .afterSteps(runner -> {
                        runner.key(GLFW.GLFW_KEY_T, 1).frames(2).key(GLFW.GLFW_KEY_C, 1).frames(2)
                                .key(GLFW.GLFW_KEY_M, 1).frames(5);
                        JSONArray contexts = runner.execute("windows").getData().getJSONArray("contexts");
                        assertEquals(2, contexts.length(), contexts::toString);
                        assertEquals(3, contexts.getJSONObject(0).getJSONArray("windows").length(), contexts::toString);
                    }),
            scene(Scene3D.class, "Draws the backpack model of the demonstration pack (python tools/demo_assets.py)")
                    .demoAssets(),
            scene(PreloadScene.class, "Enter switches to the preloaded Scene2D: the change of a preloaded scene")
                    .afterSteps(runner -> runner.press(ActionId.ENTER, 1).frames(2).untilLoaded().frames(5))
    ));

    private SmokeCases() {}

    /** The case of the scene, or the default one (reason empty). */
    static Case of(Class<? extends Scene> type) {
        for (Case smokeCase : ALL) {
            if (smokeCase.type == type)
                return smokeCase;
        }
        return new Case(type, "");
    }
}
