package MightyLibrary.mightylib.util.tweenings.types;

import MightyLibrary.mightylib.util.tweenings.ETweeningBehaviour;

public class Quadratic {

    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d){
        switch(behaviour){
            case In:
                return Quadratic.In(t, b, c, d);
            case Out:
                return Quadratic.Out(t, b, c, d);
            case InOut:
                return Quadratic.InOut(t, b, c, d);
            default:
                return 0.f;
        }
    }

    public static float In(float t, float b, float c, float d){
        return c * (t /= d) * t + b;
    }


    public static float Out(float t, float b, float c, float d){
        return -c * (t /= d) * (t - 2) + b;
    }


    public static float InOut(float t, float b, float c, float d){
        if ((t /= d / 2) < 1) {
            return c / 2 * t * t + b;
        }

        return -c / 2 * ((--t) * (t - 2) - 1) + b;
    }
}
