package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.scenes.Scene;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// The table of the particular cases of the smoke test, without GL
class SmokeCasesTest {
    @Test
    void everyCaseIsASceneOfTheDemoWithItsReason() {
        Set<Class<? extends Scene>> scenes = new HashSet<>();
        for (DemoScenes.Entry<?> entry : DemoScenes.ALL)
            scenes.add(entry.type());

        Set<Class<? extends Scene>> seen = new HashSet<>();
        for (SmokeCases.Case smokeCase : SmokeCases.ALL) {
            String name = smokeCase.type.getSimpleName();
            assertTrue(scenes.contains(smokeCase.type), name + " is not in DemoScenes.ALL");
            assertTrue(seen.add(smokeCase.type), name + " has two cases");
            assertFalse(smokeCase.reason.trim().isEmpty(), name + " has no reason");
            assertTrue(smokeCase.steps > 0, name);
            assertTrue(smokeCase.knownBug == null || !smokeCase.knownBug.trim().isEmpty(), name);
            assertEquals(smokeCase.expectedLevels.size(), smokeCase.expectedPatterns.size(), name);
        }
    }

    @Test
    void aSceneWithoutCaseHasTheDefaultOne() {
        SmokeCases.Case smokeCase = SmokeCases.of(Scene2D.class);
        assertEquals(SmokeCases.DEFAULT_STEPS, smokeCase.steps);
        assertFalse(smokeCase.lenient);
        assertFalse(smokeCase.demoAssets);
        assertNull(smokeCase.knownBug);
        assertEquals(0, smokeCase.args.length);
    }

    // Each scene of the demo runs in exactly one of the two smoke classes
    @Test
    void strictAndLenientScenesCoverTheDemo() {
        Set<String> all = new HashSet<>(SmokeRun.Scenes(false));
        for (String lenient : SmokeRun.Scenes(true))
            assertTrue(all.add(lenient), lenient);
        assertEquals(DemoScenes.ALL.size(), all.size());
    }

    @Test
    void theManifestOfTheDemonstrationPackIsRead() {
        assertTrue(Files.isRegularFile(DemoAssets.MANIFEST));
        assertNotNull(DemoAssets.Missing());
    }
}
