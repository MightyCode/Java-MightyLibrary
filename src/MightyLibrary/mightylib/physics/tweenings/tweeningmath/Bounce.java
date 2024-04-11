package MightyLibrary.mightylib.physics.tweenings.tweeningmath;

import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;

public class Bounce {
    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d){
        switch(behaviour){
            case In:
                return Bounce.In(t, b, c, d);
            case Out:
                return Bounce.Out(t, b, c, d);
            case InOut:
                return Bounce.InOut(t, b, c, d);
            default:
                return 0.f;
        }
    }

    public static float In(float t, float b, float c, float d){
        return c - Bounce.Out(d - t, 0, c, d) + b;
    }


    public static float Out(float t, float b, float c, float d){
        if ((t /= d) < (1 / 2.75f)) {
            return c * (7.5625f * t * t) + b;
        } else if (t < (2 / 2.75f)) {
            return c*(7.5625f * (t -= (1.5f / 2.75f)) * t + 0.75f) + b;
        } else if (t < (2.5 / 2.75)) {
            return c*(7.5625f * (t -= (2.25f / 2.75f)) * t + .9375f) + b;
        } else {
            return c*(7.5625f * (t -= (2.625f / 2.75f)) * t + .984375f) + b;
        }
    }


    public static float InOut(float t, float b, float c, float d){
        if (t < d / 2){
            return Bounce.In(t * 2, 0, c, d) / 2 + b;
        } else {
            return Bounce.Out(t * 2 -d, 0, c, d) / 2 + c / 2 + b;
        }
    }
}
