package io.github.mightycode.mightylib.physics.tweenings.type;

import io.github.mightycode.mightylib.main.utils.GameTimeControl;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningBehaviour;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningOption;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningType;
import io.github.mightycode.mightylib.physics.tweenings.Tweening;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// A frame lasts half a second: a tweening of two seconds ends at the fourth update
class FloatTweeningTest {
    private static final float EPS = 1e-4f;

    @BeforeEach
    void halfASecondPerFrame() {
        GameTimeControl.setDeltaTime(0.5f);
    }

    @AfterEach
    void noTimeAnymore() {
        GameTimeControl.reset();
    }

    // The value after each of the next frames
    private static float[] run(FloatTweening tweening, int frames) {
        float[] values = new float[frames];
        for (int i = 0; i < frames; ++i) {
            tweening.update();
            values[i] = tweening.value();
        }
        return values;
    }

    private static FloatTweening twoSeconds(ETweeningOption option) {
        FloatTweening tweening = new FloatTweening();
        tweening.setTweeningOption(option);
        tweening.initTwoValue(2f, 0f, 10f);
        return tweening;
    }

    @Test
    void theValueIsTheBeginningOneBeforeTheFirstUpdate() {
        assertEquals(0f, new FloatTweening().value());
        assertEquals(4f, new FloatTweening().initTwoValue(2f, 4f, 9f).value());
    }

    @Test
    void directTweeningGoesFromTheBeginningToTheEndAndStops() {
        FloatTweening tweening = twoSeconds(ETweeningOption.Direct);

        assertArrayEquals(new float[]{2.5f, 5f, 7.5f, 10f, 10f, 10f}, run(tweening, 6), EPS);
        assertTrue(tweening.finished());
    }

    @Test
    void notFinishedBeforeTheEnd() {
        FloatTweening tweening = twoSeconds(ETweeningOption.Direct);
        run(tweening, 3);
        assertFalse(tweening.finished());
        run(tweening, 1);
        assertTrue(tweening.finished());
    }

    @Test
    void initWithARangeIsInitWithTwoValues() {
        FloatTweening tweening = new FloatTweening();
        tweening.initRangeValue(2f, 5f, -10f);

        assertArrayEquals(new float[]{2.5f, 0f, -2.5f, -5f}, run(tweening, 4), EPS);
    }

    @Test
    void initReturnsTheTweeningToChainCalls() {
        FloatTweening tweening = new FloatTweening();
        assertSame(tweening, tweening.initTwoValue(1f, 0f, 1f));
        assertSame(tweening, tweening.initRangeValue(1f, 0f, 1f));
        assertSame(tweening, tweening.setTweeningOption(ETweeningOption.Loop));
        assertSame(tweening, tweening.setTweeningValues(ETweeningType.Cubic, ETweeningBehaviour.Out));
        assertSame(tweening, tweening.setAdditionalArguments(1f, 2f));
    }

    @Test
    void theCurveIsTheOneThatWasChosen() {
        FloatTweening tweening = new FloatTweening();
        tweening.setTweeningValues(ETweeningType.Quadratic, ETweeningBehaviour.In);
        tweening.initTwoValue(2f, 0f, 10f);

        assertArrayEquals(new float[]{0.625f, 2.5f, 5.625f, 10f}, run(tweening, 4), EPS);
    }

    @Test
    void mirroredTweeningComesBackAfterTheEnd() {
        FloatTweening tweening = twoSeconds(ETweeningOption.DirectMirrored);

        assertArrayEquals(new float[]{2.5f, 5f, 7.5f, 10f}, run(tweening, 4), EPS);
        assertFalse(tweening.finished(), "it still has to come back");
        assertArrayEquals(new float[]{7.5f, 5f, 2.5f, 0f}, run(tweening, 4), EPS);
        assertTrue(tweening.finished());
        assertArrayEquals(new float[]{0f, 0f}, run(tweening, 2), EPS);
    }

    @Test
    void reversedTweeningPlaysTheSameCurveBackward() {
        FloatTweening tweening = new FloatTweening();
        tweening.setTweeningValues(ETweeningType.Quadratic, ETweeningBehaviour.In);
        tweening.setTweeningOption(ETweeningOption.DirectReversed);
        tweening.initTwoValue(2f, 0f, 10f);

        assertArrayEquals(new float[]{0.625f, 2.5f, 5.625f, 10f}, run(tweening, 4), EPS);
        assertFalse(tweening.finished());
        assertArrayEquals(new float[]{5.625f, 2.5f, 0.625f, 0f}, run(tweening, 4), EPS);
        assertTrue(tweening.finished());
    }

    @Test
    void loopStartsAgainAndNeverFinishes() {
        FloatTweening tweening = twoSeconds(ETweeningOption.Loop);

        assertArrayEquals(new float[]{2.5f, 5f, 7.5f, 0f, 2.5f, 5f, 7.5f, 0f}, run(tweening, 8), EPS);
        assertFalse(tweening.finished());
    }

    @Test
    void mirroredLoopGoesBackAndForth() {
        FloatTweening tweening = twoSeconds(ETweeningOption.LoopMirrored);

        assertArrayEquals(new float[]{2.5f, 5f, 7.5f, 10f, 7.5f, 5f, 2.5f, 0f, 2.5f}, run(tweening, 9), EPS);
        assertFalse(tweening.finished());
    }

    @Test
    void reversedLoopGoesBackAndForthOnTheSameCurve() {
        FloatTweening tweening = twoSeconds(ETweeningOption.LoopReversed);

        assertArrayEquals(new float[]{2.5f, 5f, 7.5f, 10f, 7.5f, 5f, 2.5f, 0f, 2.5f}, run(tweening, 9), EPS);
        assertFalse(tweening.finished());
    }

    @Test
    void aTweeningWithoutDurationIsAtItsEndAtOnce() {
        FloatTweening tweening = new FloatTweening();
        tweening.initTwoValue(0f, 3f, 8f);

        tweening.update();

        assertEquals(8f, tweening.value(), EPS);
        assertTrue(tweening.finished());
    }

    @Test
    void theLastFrameIsCappedWhenTheOptionWasNeverSet() {
        GameTimeControl.setDeltaTime(0.75f);
        FloatTweening tweening = new FloatTweening();
        tweening.initTwoValue(2f, 0f, 10f);

        assertArrayEquals(new float[]{3.75f, 7.5f, 10f}, run(tweening, 3), EPS);
    }

    @Test
    void theLastFrameOfADirectTweeningIsCapped() {
        GameTimeControl.setDeltaTime(0.75f);
        FloatTweening tweening = twoSeconds(ETweeningOption.Direct);

        assertArrayEquals(new float[]{3.75f, 7.5f, 10f}, run(tweening, 3), EPS);
    }

    @Test
    void additionalArgumentsAreKept() {
        FloatTweening tweening = new FloatTweening();
        tweening.setAdditionalArguments(1.5f, 2.5f);

        assertEquals(1.5f, tweening.addArg1());
        assertEquals(2.5f, tweening.addArg2());
    }

    @Test
    void additionalArgumentsAreGivenToTheCurve() {
        FloatTweening withSwing = new FloatTweening();
        withSwing.setTweeningValues(ETweeningType.Back, ETweeningBehaviour.In);
        withSwing.setAdditionalArguments(5f, null);
        withSwing.initTwoValue(2f, 0f, 10f);

        FloatTweening byDefault = new FloatTweening();
        byDefault.setTweeningValues(ETweeningType.Back, ETweeningBehaviour.In);
        byDefault.initTwoValue(2f, 0f, 10f);

        withSwing.update();
        byDefault.update();

        assertTrue(withSwing.value() < byDefault.value(), "a bigger swing goes further back");
    }

    @Test
    void randomStartStaysInsideTheValues() {
        for (int attempt = 0; attempt < 20; ++attempt) {
            FloatTweening tweening = twoSeconds(ETweeningOption.LoopMirrored);
            tweening.initRandomTweening();
            for (float value : run(tweening, 10)) {
                assertTrue(value >= -EPS && value <= 10f + EPS, "value " + value);
            }
        }
    }

    @Test
    void randomStartIsAvailableForEveryOption() {
        for (ETweeningOption option : ETweeningOption.values()) {
            FloatTweening tweening = twoSeconds(option);
            assertSame(tweening, tweening.initRandomTweening());
        }
    }

    @Test
    void isATweeningOfFloats() {
        Tweening<Float> tweening = new FloatTweening();
        assertEquals(0f, tweening.value());
    }

    @Test
    void goalValueIsTheEndValue() {
        assertEquals(10f, new FloatTweening().initTwoValue(2f, 0f, 10f).goalValue());
    }
}
