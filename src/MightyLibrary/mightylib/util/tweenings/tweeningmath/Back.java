package MightyLibrary.mightylib.util.tweenings.tweeningmath;

import MightyLibrary.mightylib.util.tweenings.ETweeningBehaviour;

public class Back {

    public static final float SWING_BACK_DEFAULT = 1.70158f;
    public static final float SWING_MULTIPLICATOR = 1.525f;


    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d,
                                 Float s){
        switch(behaviour){
            case In:
                return Back.InFull(t, b, c, d, s);
            case Out:
                return Back.OutFull(t, b, c, d, s);
            case InOut:
                return Back.InOutFull(t, b, c, d, s);
            default:
                return 0.f;
        }
    }


    public static float InFull(float t, float b, float c, float d,
                               Float s) {

        if (s == null)
            return Back.In(t, b, c, d);

        return c * (t /= d) * t *((s + 1) * t - s) + b;
    }


    public static float In(float t, float b, float c, float d){
        float s = Back.SWING_BACK_DEFAULT;

        return c * (t /= d) * t *((s + 1) * t - s) + b;
    }


    public static float OutFull(float t, float b, float c, float d,
                                Float s) {
        return c * ((t = t / d - 1) * t * ((s + 1) * t + s) + 1) + b;
    }


    public static float Out(float t, float b, float c, float d){
        float s = Back.SWING_BACK_DEFAULT;

        return c * ((t = t / d - 1) * t * ((s + 1) * t + s) + 1) + b;
    }


    public static float InOutFull(float t, float b, float c, float d,
                                  Float s) {
        if (s == null)
            return Back.InOut(t, b, c, d);

        if ((t /= d / 2) < 1)
            return c / 2 * (t * t *
                    (((s *= (Back.SWING_MULTIPLICATOR)) + 1) * t - s)) + b;

        return c / 2 * ((t -= 2) * t *
                (((s *= (Back.SWING_MULTIPLICATOR)) + 1) * t + s) + 2) + b;

    }

    public static float InOut(float t, float b, float c, float d){
        float s = Back.SWING_BACK_DEFAULT;

        if ((t /= d / 2) < 1)
            return c / 2 * (t * t *
                    (((s *= (Back.SWING_MULTIPLICATOR)) + 1) * t - s)) + b;

        return c / 2 * ((t -= 2) * t *
                (((s *= (Back.SWING_MULTIPLICATOR)) + 1) * t + s) + 2) + b;
    }
}
