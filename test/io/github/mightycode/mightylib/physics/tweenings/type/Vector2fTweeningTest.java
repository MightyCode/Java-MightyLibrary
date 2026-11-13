package io.github.mightycode.mightylib.physics.tweenings.type;

import io.github.mightycode.mightylib.main.utils.GameTimeControl;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningBehaviour;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningOption;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningType;
import org.joml.Vector2f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// A frame lasts half a second: a tweening of two seconds ends at the fourth update
class Vector2fTweeningTest {
    private static final float EPS = 1e-4f;

    @BeforeEach
    void halfASecondPerFrame() {
        GameTimeControl.setDeltaTime(0.5f);
    }

    @AfterEach
    void noTimeAnymore() {
        GameTimeControl.reset();
    }

    private static void assertVector(float x, float y, Vector2f actual) {
        assertEquals(x, actual.x, EPS, "x");
        assertEquals(y, actual.y, EPS, "y");
    }

    private static Vector2fTweening twoSeconds(ETweeningOption option) {
        Vector2fTweening tweening = new Vector2fTweening();
        tweening.setTweeningOption(option);
        return tweening.initTwoValue(2f, new Vector2f(0, 100), new Vector2f(10, 0));
    }

    @Test
    void theValueIsTheBeginningOneBeforeTheFirstUpdate() {
        assertVector(0, 0, new Vector2fTweening().value());
        assertVector(0, 100, twoSeconds(ETweeningOption.Direct).value());
    }

    @Test
    void bothAxesAreTweenedTogether() {
        Vector2fTweening tweening = twoSeconds(ETweeningOption.Direct);

        tweening.update();
        assertVector(2.5f, 75f, tweening.value());
        tweening.update();
        assertVector(5f, 50f, tweening.value());
        tweening.update();
        tweening.update();
        assertVector(10f, 0f, tweening.value());
        assertTrue(tweening.finished());
    }

    @Test
    void initWithARangeAddsItToTheBeginning() {
        Vector2fTweening tweening = new Vector2fTweening();
        tweening.initRangeValue(2f, new Vector2f(1, 1), new Vector2f(4, -2));

        assertVector(5f, -1f, tweening.goalValue());
    }

    @Test
    void goalValueIsTheEndAndACopy() {
        Vector2fTweening tweening = twoSeconds(ETweeningOption.Direct);

        tweening.goalValue().set(999, 999);

        assertVector(10, 0, tweening.goalValue());
    }

    @Test
    void valueIsACopy() {
        Vector2fTweening tweening = twoSeconds(ETweeningOption.Direct);
        tweening.update();

        tweening.value().set(999, 999);

        assertVector(2.5f, 75f, tweening.value());
    }

    @Test
    void theVectorsGivenToInitAreCopied() {
        Vector2f begin = new Vector2f(0, 0), end = new Vector2f(10, 10);
        Vector2fTweening tweening = new Vector2fTweening().initTwoValue(2f, begin, end);

        begin.set(500, 500);
        end.set(900, 900);
        tweening.update();
        tweening.update();

        assertVector(5, 5, tweening.value());
    }

    @Test
    void theCurveIsTheOneThatWasChosen() {
        Vector2fTweening tweening = new Vector2fTweening();
        tweening.setTweeningValues(ETweeningType.Quadratic, ETweeningBehaviour.In);
        tweening.initTwoValue(2f, new Vector2f(0, 0), new Vector2f(10, 20));

        tweening.update();
        tweening.update();

        assertVector(2.5f, 5f, tweening.value());
    }

    @Test
    void loopStartsAgain() {
        Vector2fTweening tweening = twoSeconds(ETweeningOption.Loop);

        for (int i = 0; i < 4; ++i)
            tweening.update();

        assertVector(0, 100, tweening.value());
        assertFalse(tweening.finished());
    }

    @Test
    void theFirstMirroredFrameStartsFromTheEnd() {
        Vector2fTweening tweening = twoSeconds(ETweeningOption.DirectMirrored);

        for (int i = 0; i < 4; ++i)
            tweening.update();

        assertVector(10, 0, tweening.value());
        assertFalse(tweening.finished());
    }

    @Test
    void mirroredTweeningComesBackToTheBeginning() {
        Vector2fTweening tweening = twoSeconds(ETweeningOption.DirectMirrored);
        for (int i = 0; i < 4; ++i)
            tweening.update();

        tweening.update();
        assertVector(7.5f, 25f, tweening.value());
        tweening.update();
        assertVector(5f, 50f, tweening.value());
        tweening.update();
        tweening.update();
        assertVector(0f, 100f, tweening.value());
        assertTrue(tweening.finished());
    }

    @Test
    void aTweeningWithoutDurationIsAtItsEndAtOnce() {
        Vector2fTweening tweening = new Vector2fTweening().initTwoValue(0f, new Vector2f(1, 1), new Vector2f(7, 8));

        tweening.update();

        assertVector(7, 8, tweening.value());
        assertTrue(tweening.finished());
    }
}
