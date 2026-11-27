package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.test.CapturedImage;
import io.github.mightycode.mightylib.test.ImageComparison;
import io.github.mightycode.mightylib.test.ImageTolerance;
import io.github.mightycode.mightylib.test.MightySceneTest;
import io.github.mightycode.mightylib.test.ReferenceImages;
import io.github.mightycode.mightylib.test.SceneRunner;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylibdemo.main.ReferenceTestConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The final images of the reference scenes against the references of the driver of the machine
 * (src/test/resources/references/<key>/, ReferenceImages): several views of each, named Scene-view. A rendering
 * changed on purpose generates them again (tools/references.py --update, --from-ci for the series of the CI).
 */
@MightySceneTest(config = ReferenceTestConfig.class)
class ReferenceImagesGLTest {
    // The views of a scene: a name, then the commands that set it (camera, picture...); compared within STRICT, or a
    // tolerance of the view with its reason
    private static final class View {
        final String name;
        final String[] commands;
        final ImageTolerance tolerance;

        View(String name, String... commands) {
            this(name, ImageTolerance.STRICT, commands);
        }

        View(String name, ImageTolerance tolerance, String... commands) {
            this.name = name;
            this.tolerance = tolerance;
            this.commands = commands;
        }
    }

    private static final View DEFAULT = new View("default");
    private static final View ZOOM = new View("zoom", "camera main2D zoom 2");
    private static final View ROTATION = new View("rotation", "camera main2D rotation 20");
    /*
     * The picture settings of the player: llvmpipe rounds gamma, brightness and contrast otherwise on an Intel and on
     * an AMD processor (15 pixels up to 4 levels apart, the only view that differs beyond STRICT): one series of the
     * CI for both, whatever the processor of its runner (RendererKey)
     */
    private static final View PICTURE = new View("picture", ImageTolerance.STRICT.withDifferentPixels(40)
            .withReason("llvmpipe rounds the picture settings otherwise on Intel and AMD processors (15 pixels up to 4"
                    + " levels apart)"),
            "picture gamma 1.4", "picture brightness 1.1", "picture contrast 1.2");

    // Every view of the scene compared, the failures together; a machine without series skips the test
    private static void assertViews(SceneRunner runner, String scene, View... views) {
        List<String> failures = new ArrayList<>();
        for (View view : views) {
            runner.start(scene).untilLoaded();
            for (String command : view.commands) {
                CommandResult result = runner.execute(command);
                assertTrue(result.isSuccess(), command + ": " + result);
            }

            CapturedImage image = runner.capture();
            assertEquals(ReferenceTestConfig.WIDTH, image.width());
            assertEquals(ReferenceTestConfig.HEIGHT, image.height());
            try {
                ReferenceImages.assertMatches(image, scene + "-" + view.name, view.tolerance);
            } catch (AssertionError e) {
                failures.add(e.getMessage());
            }
        }
        if (!failures.isEmpty())
            fail(String.join("\n", failures));
    }

    @Test
    void sprites(SceneRunner runner) {
        assertViews(runner, "ReferenceSprites2D", DEFAULT, ZOOM, ROTATION);
    }

    @Test
    void shapesAndTexts(SceneRunner runner) {
        assertViews(runner, "ReferenceShapesText2D", DEFAULT, ZOOM, ROTATION);
    }

    @Test
    void colorSpaces(SceneRunner runner) {
        try {
            assertViews(runner, "ReferenceColorSpace2D", DEFAULT, ZOOM, PICTURE);
        } finally {
            // The engine is kept for the next test: the picture of the player back to its defaults
            runner.execute("picture reset");
        }
    }

    @Test
    void lit3D(SceneRunner runner) {
        assertViews(runner, "Reference3D", DEFAULT,
                new View("moved", "camera main3D move 5 5 6", "camera main3D lookat 0 2 0"));
    }

    // A reference is only worth something if the scene gives the same image each time
    @Test
    void aReferenceSceneGivesTheSameImageEachTime(SceneRunner runner) {
        for (String scene : List.of("ReferenceSprites2D", "ReferenceShapesText2D", "ReferenceColorSpace2D",
                "Reference3D")) {
            CapturedImage first = runner.start(scene).untilLoaded().capture();
            runner.frames(3);
            CapturedImage later = runner.capture();
            CapturedImage again = runner.start(scene).untilLoaded().capture();

            ImageTolerance exact = ImageTolerance.STRICT.withChannelDifference(0);
            ImageComparison.compare(first, later, exact).assertMatches(scene + " a few ticks later");
            ImageComparison.compare(first, again, exact).assertMatches(scene + " started again");
        }
    }
}
