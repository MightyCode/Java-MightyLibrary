package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.mightycode.mightylib.test.BatchPictures.*;
import static org.junit.jupiter.api.Assertions.*;

// Animation2DRenderer in the 2D batch: the same picture as drawn alone, one draw call for its frames
@MightySceneTest(config = SceneRunnerTest.Config.class)
class Animation2DBatchGLTest {
    // Animations without animator: the quad of their whole texture, flipped, shifted and turned
    @Test
    void theBatchedAnimationsAreTheAnimationsDrawnAlone(SceneRunner runner) {
        inScene(runner, scene -> {
            List<Animation2DRenderer> renderers = new ArrayList<>();
            Pattern pattern;
            EDirection[] shifts = {EDirection.None, EDirection.LeftUp, EDirection.RightDown, EDirection.Up};
            try (Views.Scope ignored = Views.enter(scene)) {
                pattern = new Pattern();
                for (int i = 0; i < 8; ++i) {
                    Animation2DRenderer animation = new Animation2DRenderer(EngineResources.TEXTURE_2D);
                    animation.setMainTextureChannel(pattern);
                    animation.setShiftRotation(shifts[i % shifts.length]);
                    animation.setHorizontalFlip(i % 2 == 1);
                    animation.setVerticalFlip(i % 4 >= 2);
                    animation.setScale(new Vector3f(80 + i * 11, 60 + i * 9, 1));
                    animation.setPosition(new Vector2f(100 + i * 110, 150 + (i % 3) * 150));
                    animation.setRotationZ(i * 0.5f - 1.5f);
                    renderers.add(animation);
                }
            }
            runner.frames(5);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                capture.assertEveryModeDrawsTheSame(scene, () -> renderers.forEach(Animation2DRenderer::display));
                assertEquals(renderers.size(), scene.batch2D().lastFrameDraws());
                assertEquals(1, scene.batch2D().lastFrameDrawCalls());

                renderers.forEach(Animation2DRenderer::unload);
                pattern.delete();
            }
        });
    }
}
