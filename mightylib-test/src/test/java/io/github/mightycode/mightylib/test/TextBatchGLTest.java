package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.mightycode.mightylib.test.BatchPictures.*;
import static org.junit.jupiter.api.Assertions.*;

// Text in the 2D batch: the same picture as drawn alone, one draw call for texts of one font whatever their color
@MightySceneTest(config = SceneRunnerTest.Config.class)
class TextBatchGLTest {
    @Test
    void theBatchedTextsAreTheTextsDrawnAlone(SceneRunner runner) {
        inScene(runner, scene -> {
            List<Text> texts = new ArrayList<>();
            try (Views.Scope ignored = Views.enter(scene)) {
                EDirection[] references = {EDirection.LeftUp, EDirection.None, EDirection.RightDown};
                for (int i = 0; i < 6; ++i) {
                    Text text = new Text();
                    text.setFontSize(14 + i * 6);
                    text.setColor(new Color4f(0.15f * i, 1f - 0.12f * i, 0.3f, 1f - 0.1f * i));
                    text.setReference(references[i % references.length]);
                    text.setText("Batched text " + i + " : AaBbQq 0123 éàç");
                    text.setPosition(new Vector2f(30 + i * 40, 40 + i * 60));
                    text.setRotationZ(i == 4 ? 0.4f : 0f);
                    texts.add(text);
                }

                Text paragraph = new Text();
                paragraph.setFontSize(18);
                paragraph.setAlignment(ETextAlignment.Center);
                paragraph.setMaxWidth(420);
                paragraph.setText("A long paragraph wrapped at the spaces, centered, so that every line of it is a"
                        + " part of the same draw of the batch and keeps its place.");
                paragraph.setPosition(new Vector2f(700, 420));
                texts.add(paragraph);
            }
            runner.frames(5);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                capture.assertEveryModeDrawsTheSame(scene, () -> texts.forEach(Text::display));
                assertEquals(texts.size(), scene.batch2D().lastFrameDraws());
                assertEquals(1, scene.batch2D().lastFrameDrawCalls());

                texts.forEach(Text::unload);
            }
        });
    }
}
