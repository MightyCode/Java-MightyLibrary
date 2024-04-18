package MightyLibrary.mightylib.physics.tweenings.tweeningmath;

import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;

public class Linear {
    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d){
        switch(behaviour){
            case In:
                return Linear.In(t, b, c, d);
            case Out:
                return Linear.Out(t, b, c, d);
            case InOut:
                return Linear.InOut(t, b, c, d);
            default:
                return 0.f;
        }
    }

    public static float In(float t, float b, float c, float d){
        return c * t / d + b;
    }

    public static float Out(float t, float b, float c, float d){
        return Linear.In(t, b, c, d);
    }

    public static float InOut(float t, float b, float c, float d){
        return Linear.In(t, b, c, d);
    }
}
