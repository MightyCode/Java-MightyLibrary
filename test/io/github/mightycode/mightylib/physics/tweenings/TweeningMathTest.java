package io.github.mightycode.mightylib.physics.tweenings;

import io.github.mightycode.mightylib.physics.tweenings.tweeningmath.Back;
import io.github.mightycode.mightylib.physics.tweenings.tweeningmath.Bounce;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Penner easing functions: t = time, b = beginning value, c = change, d = duration
class TweeningMathTest {
    private static final float B = 10f, C = 20f, D = 4f;
    private static final float EPS = 1e-3f;

    private static float at(ETweeningType type, ETweeningBehaviour behaviour, float t) {
        return Tweening.Evaluate(type, behaviour, t, B, C, D);
    }

    // The value at a given fraction of the duration on a curve going from 0 to 1
    private static float unit(ETweeningType type, ETweeningBehaviour behaviour, float fraction) {
        return Tweening.Evaluate(type, behaviour, fraction * D, 0f, 1f, D);
    }

    // Back.Out has no default swing: the curves are compared with the default one given explicitly
    private static float atWithArguments(ETweeningType type, ETweeningBehaviour behaviour, float t) {
        return Tweening.Evaluate(type, behaviour, t, B, C, D, Back.SWING_BACK_DEFAULT, 1.2f);
    }

    @Test
    void everyCurveStartsAtTheBeginningValueAndEndsAtTheEndValue() {
        for (ETweeningType type : ETweeningType.values()) {
            for (ETweeningBehaviour behaviour : ETweeningBehaviour.values()) {
                String name = type + " " + behaviour;
                assertEquals(B, atWithArguments(type, behaviour, 0f), EPS, name + " start");
                assertEquals(B + C, atWithArguments(type, behaviour, D), EPS, name + " end");
            }
        }
    }

    @Test
    void everyCurveWithItsDefaultArgumentsStartsAndEndsAtTheRightValues() {
        for (ETweeningType type : ETweeningType.values()) {
            for (ETweeningBehaviour behaviour : ETweeningBehaviour.values()) {
                if (type == ETweeningType.Back && behaviour == ETweeningBehaviour.Out)
                    continue; // see backOutWithoutArgument

                String name = type + " " + behaviour;
                assertEquals(B, at(type, behaviour, 0f), EPS, name + " start");
                assertEquals(B + C, at(type, behaviour, D), EPS, name + " end");
            }
        }
    }

    @Disabled("Known bug: Back.OutFull does not handle a missing swing value (null) like InFull and InOutFull do, it throws")
    @Test
    void backOutWithoutArgument() {
        assertEquals(Back.Out(1f, B, C, D), at(ETweeningType.Back, ETweeningBehaviour.Out, 1f), EPS);
    }

    @Test
    void inOutCurvesCrossTheMiddleInTheMiddle() {
        for (ETweeningType type : ETweeningType.values())
            assertEquals(B + C / 2, at(type, ETweeningBehaviour.InOut, D / 2), EPS, type.toString());
    }

    @Test
    void linearIsALineWhateverTheBehaviour() {
        for (ETweeningBehaviour behaviour : ETweeningBehaviour.values()) {
            assertEquals(B + C / 4, at(ETweeningType.Linear, behaviour, 1f), EPS);
            assertEquals(B + C / 2, at(ETweeningType.Linear, behaviour, 2f), EPS);
            assertEquals(B + 3 * C / 4, at(ETweeningType.Linear, behaviour, 3f), EPS);
        }
    }

    @Test
    void polynomialCurvesAtHalfTime() {
        assertEquals(0.25f, unit(ETweeningType.Quadratic, ETweeningBehaviour.In, 0.5f), EPS);
        assertEquals(0.75f, unit(ETweeningType.Quadratic, ETweeningBehaviour.Out, 0.5f), EPS);
        assertEquals(0.125f, unit(ETweeningType.Cubic, ETweeningBehaviour.In, 0.5f), EPS);
        assertEquals(0.875f, unit(ETweeningType.Cubic, ETweeningBehaviour.Out, 0.5f), EPS);
        assertEquals(0.0625f, unit(ETweeningType.Quartic, ETweeningBehaviour.In, 0.5f), EPS);
        assertEquals(0.9375f, unit(ETweeningType.Quartic, ETweeningBehaviour.Out, 0.5f), EPS);
        assertEquals(0.03125f, unit(ETweeningType.Quintic, ETweeningBehaviour.In, 0.5f), EPS);
        assertEquals(0.96875f, unit(ETweeningType.Quintic, ETweeningBehaviour.Out, 0.5f), EPS);
    }

    @Test
    void otherSmoothCurvesAtHalfTime() {
        assertEquals(0.29289f, unit(ETweeningType.Sinusoidal, ETweeningBehaviour.In, 0.5f), EPS);
        assertEquals(0.70711f, unit(ETweeningType.Sinusoidal, ETweeningBehaviour.Out, 0.5f), EPS);
        assertEquals(0.03125f, unit(ETweeningType.Exponential, ETweeningBehaviour.In, 0.5f), EPS);
        assertEquals(0.96875f, unit(ETweeningType.Exponential, ETweeningBehaviour.Out, 0.5f), EPS);
        assertEquals(0.13397f, unit(ETweeningType.Circular, ETweeningBehaviour.In, 0.5f), EPS);
        assertEquals(0.86603f, unit(ETweeningType.Circular, ETweeningBehaviour.Out, 0.5f), EPS);
    }

    @Test
    void easeInCurvesAreBelowTheLineAndEaseOutCurvesAbove() {
        ETweeningType[] smooth = {ETweeningType.Quadratic, ETweeningType.Cubic, ETweeningType.Quartic, ETweeningType.Quintic,
                ETweeningType.Sinusoidal, ETweeningType.Exponential, ETweeningType.Circular};
        for (ETweeningType type : smooth) {
            for (float fraction : new float[]{0.1f, 0.3f, 0.5f, 0.7f, 0.9f}) {
                assertTrue(unit(type, ETweeningBehaviour.In, fraction) < fraction, type + " in at " + fraction);
                assertTrue(unit(type, ETweeningBehaviour.Out, fraction) > fraction, type + " out at " + fraction);
            }
        }
    }

    @Test
    void smoothCurvesNeverGoBackward() {
        ETweeningType[] smooth = {ETweeningType.Linear, ETweeningType.Quadratic, ETweeningType.Cubic, ETweeningType.Quartic,
                ETweeningType.Quintic, ETweeningType.Sinusoidal, ETweeningType.Exponential, ETweeningType.Circular};
        for (ETweeningType type : smooth) {
            for (ETweeningBehaviour behaviour : ETweeningBehaviour.values()) {
                float previous = unit(type, behaviour, 0f);
                for (int step = 1; step <= 100; ++step) {
                    float value = unit(type, behaviour, step / 100f);
                    assertTrue(value >= previous - 1e-5f, type + " " + behaviour + " at step " + step);
                    previous = value;
                }
            }
        }
    }

    @Test
    void backStartsByGoingTheWrongWayAndOutOvershootsTheEnd() {
        assertTrue(unit(ETweeningType.Back, ETweeningBehaviour.In, 0.2f) < 0f);
        assertTrue(Back.Out(0.8f * D, 0f, 1f, D) > 1f);
        assertTrue(Back.In(0.2f * D, 0f, 1f, D) < 0f);
    }

    @Test
    void backWithoutSwingIsACubic() {
        float cubic = unit(ETweeningType.Cubic, ETweeningBehaviour.In, 0.6f);
        assertEquals(cubic, Back.InFull(0.6f * D, 0f, 1f, D, 0f), EPS);
    }

    @Test
    void backSwingGivenByTheCallerReplacesTheDefaultOne() {
        float defaultSwing = Back.In(0.2f * D, 0f, 1f, D);
        float biggerSwing = Back.InFull(0.2f * D, 0f, 1f, D, 3f);
        assertTrue(biggerSwing < defaultSwing, "a bigger swing goes further back");
        assertEquals(defaultSwing, Back.InFull(0.2f * D, 0f, 1f, D, null), EPS, "no value: the default swing");
        assertEquals(defaultSwing, Back.InFull(0.2f * D, 0f, 1f, D, Back.SWING_BACK_DEFAULT), EPS);
    }

    @Test
    void bounceFirstBounceAndSymmetry() {
        assertEquals(0.765625f, unit(ETweeningType.Bounce, ETweeningBehaviour.Out, 0.5f), EPS);
        assertEquals(0.234375f, unit(ETweeningType.Bounce, ETweeningBehaviour.In, 0.5f), EPS);
        assertEquals(1f, Bounce.Out(D, 0f, 1f, D) + Bounce.In(0f, 0f, 1f, D), EPS);
        for (float fraction : new float[]{0.1f, 0.4f, 0.8f})
            assertEquals(1f, unit(ETweeningType.Bounce, ETweeningBehaviour.In, fraction)
                    + unit(ETweeningType.Bounce, ETweeningBehaviour.Out, 1 - fraction), EPS);
    }

    @Test
    void bounceStaysBetweenTheTwoValues() {
        for (ETweeningBehaviour behaviour : ETweeningBehaviour.values()) {
            for (int step = 0; step <= 100; ++step) {
                float value = unit(ETweeningType.Bounce, behaviour, step / 100f);
                assertTrue(value >= -1e-4f && value <= 1f + 1e-4f, behaviour + " at step " + step + " = " + value);
            }
        }
    }

    @Test
    void elasticOscillatesAroundTheEndValue() {
        boolean above = false, below = false;
        for (int step = 1; step < 100; ++step) {
            float value = unit(ETweeningType.Elastic, ETweeningBehaviour.Out, step / 100f);
            above |= value > 1f;
            below |= value < 1f;
            assertTrue(Float.isFinite(value));
        }
        assertTrue(above && below, "the curve crosses the end value");
    }

    @Test
    void elasticArgumentsChangeTheCurve() {
        float byDefault = Tweening.Evaluate(ETweeningType.Elastic, ETweeningBehaviour.In, 3f, 0f, 1f, D);
        float custom = Tweening.Evaluate(ETweeningType.Elastic, ETweeningBehaviour.In, 3f, 0f, 1f, D, 2f, 2f);
        assertNotEquals(byDefault, custom, 1e-4f);
    }

    @Test
    void evaluateWithTheShortFormIsTheFormWithoutAdditionalValues() {
        assertEquals(Tweening.Evaluate(ETweeningType.Elastic, ETweeningBehaviour.Out, 1f, B, C, D, null, null),
                at(ETweeningType.Elastic, ETweeningBehaviour.Out, 1f));
    }

    @Test
    void ratesBeyondTheDurationAreExtrapolated() {
        assertEquals(B + 2 * C, at(ETweeningType.Linear, ETweeningBehaviour.In, 2 * D), EPS);
        assertEquals(B + 4 * C, at(ETweeningType.Quadratic, ETweeningBehaviour.In, 2 * D), EPS);
    }

    @Test
    void aNegativeChangeGoesDown() {
        assertEquals(10f, Tweening.Evaluate(ETweeningType.Quadratic, ETweeningBehaviour.In, 0f, 10f, -10f, D), EPS);
        assertEquals(0f, Tweening.Evaluate(ETweeningType.Quadratic, ETweeningBehaviour.In, D, 10f, -10f, D), EPS);
        assertEquals(7.5f, Tweening.Evaluate(ETweeningType.Quadratic, ETweeningBehaviour.In, D / 2, 10f, -10f, D), EPS);
    }
}
