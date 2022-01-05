package MightyLibrary.mightylib.util.tweenings.types;

import MightyLibrary.mightylib.util.tweenings.ETweeningBehaviour;

public class Exponential {

    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d){
        switch(behaviour){
            case In:
                return Exponential.In(t, b, c, d);
            case Out:
                return Exponential.Out(t, b, c, d);
            case InOut:
                return Exponential.InOut(t, b, c, d);
            default:
                return 0.f;
        }
    }

    public static float In(float t, float b, float c, float d){
        return (t == 0) ? b : c * (float)Math.pow(2, 10 * (t / d - 1)) + b;
    }


    public static float Out(float t, float b, float c, float d){
        return (t == d) ? b + c : c * (float)(-Math.pow(2, -10 * t / d) + 1) + b;
    }


    public static float InOut(float t, float b, float c, float d){
        if (t == 0)
            return b;

        if (t == d)
            return b + c;

        if ((t /= d / 2) < 1)
            return c / 2 * (float)Math.pow(2, 10 * (t - 1)) + b;

        return c / 2 * (float)(-Math.pow(2, -10 * --t) + 2) + b;
    }
}
