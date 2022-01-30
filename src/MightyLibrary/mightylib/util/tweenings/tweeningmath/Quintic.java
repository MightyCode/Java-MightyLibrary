package MightyLibrary.mightylib.util.tweenings.tweeningmath;

import MightyLibrary.mightylib.util.tweenings.ETweeningBehaviour;

public class Quintic {
    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d){
        switch(behaviour){
            case In:
                return Quintic.In(t, b, c, d);
            case Out:
                return Quintic.Out(t, b, c, d);
            case InOut:
                return Quintic.InOut(t, b, c, d);
            default:
                return 0.f;
        }
    }

    public static float In(float t, float b, float c, float d){
        return c * (t /= d) * t * t * t * t + b;
    }


    public static float Out(float t, float b, float c, float d){
        return c * ((t = t / d - 1) * t * t * t * t + 1) + b;
    }


    public static float InOut(float t, float b, float c, float d){
        if ((t /= d / 2) < 1){
            return c / 2 * t * t * t * t * t + b;
        }

        return c / 2 * ((t -= 2) * t * t * t * t + 2) + b;
    }
}
