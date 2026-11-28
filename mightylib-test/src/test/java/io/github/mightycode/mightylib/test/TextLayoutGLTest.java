package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static io.github.mightycode.mightylib.test.BatchPictures.inScene;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The box of a Text (size, left top corner) from its lines, its font size, its padding and its reference, and the place
 * of its characters by its alignment: the font of the engine, measured with the text's own measures (measureWidth,
 * lineHeight).
 */
@MightySceneTest(config = SceneRunnerTest.Config.class)
class TextLayoutGLTest {
    private static final float EPSILON = 1e-3f;

    // Texts made by the test in the scene, unloaded after it
    private static void withTexts(SceneRunner runner, Consumer<List<Text>> test) {
        inScene(runner, scene -> {
            List<Text> texts = new ArrayList<>();
            try (Views.Scope ignored = Views.enter(scene)) {
                try {
                    test.accept(texts);
                } finally {
                    texts.forEach(Text::unload);
                }
            }
        });
    }

    private static Text text(List<Text> texts, String content, float fontSize) {
        Text text = new Text();
        texts.add(text);
        return text.setFontSize(fontSize).setReference(EDirection.LeftUp).setPosition(new Vector2f(100f, 50f))
                .setText(content);
    }

    private static void assertVector(float x, float y, Vector2f actual, String what) {
        assertEquals(x, actual.x, EPSILON, what + " x");
        assertEquals(y, actual.y, EPSILON, what + " y");
    }

    @Test
    void theBoxIsTheWidestLineByTheLines(SceneRunner runner) {
        withTexts(runner, texts -> {
            Text text = text(texts, "Hello", 20);
            assertTrue(text.measureWidth("Hello") > 0);
            assertVector(text.measureWidth("Hello"), text.lineHeight(), text.size(), "one line");
            assertVector(100f, 50f, text.leftUpPosition(), "left up");

            text.setText("Hello\nHi everyone\nA");
            assertVector(text.measureWidth("Hi everyone"), 3 * text.lineHeight(), text.size(), "three lines");
        });
    }

    // The measures follow the font size: twice the size, twice the box
    @Test
    void theBoxFollowsTheFontSize(SceneRunner runner) {
        withTexts(runner, texts -> {
            Text text = text(texts, "Scale me", 15);
            Vector2f small = new Vector2f(text.size());
            text.setFontSize(30);
            assertVector(small.x * 2, small.y * 2, text.size(), "font size 30");
            assertEquals(30f, text.getFontSize());
        });
    }

    // The padding makes a bigger box, the text inside: the first character starts after the left and top padding
    @Test
    void thePaddingIsInTheBox(SceneRunner runner) {
        withTexts(runner, texts -> {
            Text text = text(texts, "Pad", 20);
            Vector2f bare = new Vector2f(text.size());
            Vector4f firstBare = text.getPositionOfChar(0);

            text.setPadding(4f, 6f, 8f, 10f);
            assertVector(bare.x + 12f, bare.y + 16f, text.size(), "padded");
            assertEquals(new Vector4f(4f, 6f, 8f, 10f), text.getPadding());
            Vector4f first = text.getPositionOfChar(0);
            assertEquals(firstBare.x + 4f, first.x, EPSILON);
            assertEquals(firstBare.z + 6f, first.z, EPSILON);
        });
    }

    // The reference is the point of the box at the position: the box moves by a part of its size
    @Test
    void theReferenceIsThePointOfTheBoxAtThePosition(SceneRunner runner) {
        withTexts(runner, texts -> {
            Text text = text(texts, "Anchored", 24);
            Vector2f size = new Vector2f(text.size());
            Object[][] cases = {
                    {EDirection.LeftUp, 0f, 0f}, {EDirection.Up, 0.5f, 0f}, {EDirection.RightUp, 1f, 0f},
                    {EDirection.Left, 0f, 0.5f}, {EDirection.None, 0.5f, 0.5f}, {EDirection.Right, 1f, 0.5f},
                    {EDirection.LeftDown, 0f, 1f}, {EDirection.Down, 0.5f, 1f}, {EDirection.RightDown, 1f, 1f},
            };
            for (Object[] testCase : cases) {
                text.setReference((EDirection) testCase[0]);
                assertVector(100f - size.x * (float) testCase[1], 50f - size.y * (float) testCase[2],
                        text.leftUpPosition(), testCase[0].toString());
                assertEquals(testCase[0], text.reference());
            }

            // The box follows the position
            text.setReference(EDirection.None);
            text.setPosition(new Vector2f(300f, 200f));
            assertVector(300f - size.x / 2, 200f - size.y / 2, text.leftUpPosition(), "moved");
        });
    }

    // The first character of the short line: at the left, centered in the widest line, or at its right
    @Test
    void theAlignmentPlacesTheShortLinesInTheWidestOne(SceneRunner runner) {
        withTexts(runner, texts -> {
            String content = "A wide first line\nShort";
            Text text = text(texts, content, 20);
            int shortStart = "A wide first line".length();
            float gap = text.measureWidth("A wide first line") - text.measureWidth("Short");
            float left = text.getPositionOfChar(shortStart).x;

            text.setAlignment(ETextAlignment.Center);
            assertEquals(left + gap / 2, text.getPositionOfChar(shortStart).x, EPSILON, "center");
            text.setAlignment(ETextAlignment.Right);
            assertEquals(left + gap, text.getPositionOfChar(shortStart).x, EPSILON, "right");
            assertEquals(ETextAlignment.Right, text.getAlignment());
            // The widest line does not move
            text.setAlignment(ETextAlignment.Left);
            float wideLeft = text.getPositionOfChar(0).x;
            text.setAlignment(ETextAlignment.Right);
            assertEquals(wideLeft, text.getPositionOfChar(0).x, EPSILON, "widest line");
        });
    }

    // Wrapped at the spaces: no line wider than the maximum, the text given kept, every character still placed
    @Test
    void aMaximumWidthWrapsTheTextInsideIt(SceneRunner runner) {
        withTexts(runner, texts -> {
            String content = "one two three four five six seven eight nine ten";
            Text text = text(texts, content, 20);
            float oneLine = text.size().y;
            float maxWidth = text.measureWidth("one two three ");

            text.setMaxWidth(maxWidth);
            assertTrue(text.size().x <= maxWidth + EPSILON, text.size().x + " wider than " + maxWidth);
            assertTrue(text.size().y >= 3 * oneLine - EPSILON, "several lines: " + text.size().y);
            assertEquals(content, text.text());
            Vector4f last = text.getPositionOfChar(content.length() - 1);
            assertTrue(last.z > text.getPositionOfChar(0).z, "the last character on a line below the first");

            text.setMaxWidth(0);
            assertEquals(oneLine, text.size().y, EPSILON, "no wrapping");
        });
    }

    // A text emptied draws nothing and its box is the padding alone at its place (it kept the box of the previous
    // text, which stayed under the mouse for a GUI)
    @Test
    void anEmptiedTextHasTheBoxOfItsPaddingOnly(SceneRunner runner) {
        withTexts(runner, texts -> {
            Text text = text(texts, "Something", 20).setPadding(2f, 3f, 4f, 5f).setReference(EDirection.None);
            text.setText("");
            assertVector(6f, 8f, text.size(), "empty");
            assertVector(97f, 46f, text.leftUpPosition(), "empty, centered on the position");

            text.setPosition(new Vector2f(10f, 20f));
            assertVector(7f, 16f, text.leftUpPosition(), "empty, moved");

            text.setText("Back");
            assertVector(text.measureWidth("Back") + 6f, text.lineHeight() + 8f, text.size(), "filled again");
        });
    }
}
