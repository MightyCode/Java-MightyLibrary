package MightyLibrary.mightylib.physics.tweenings.tweeningmath;

import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;

public class Cubic {
    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d){
        switch(behaviour){
            case In:
                return Cubic.In(t, b, c, d);
            case Out:
                return Cubic.Out(t, b, c, d);
            case InOut:
                return Cubic.InOut(t, b, c, d);
            default:
                return 0.f;
        }
    }

    public static float In(float t, float b, float c, float d){
        return c * (t /= d) * t * t + b;
    }


    public static float Out(float t, float b, float c, float d){
        return c * ((t=t/d - 1) * t * t + 1) + b;
    }


    public static float InOut(float t, float b, float c, float d){
        if ((t /= d / 2) < 1){
            return c / 2 * t * t * t + b;
        }

        return c / 2 * ((t -= 2) * t * t + 2) + b;
    }
}
