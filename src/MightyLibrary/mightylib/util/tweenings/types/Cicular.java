package MightyLibrary.mightylib.util.tweenings.types;

import MightyLibrary.mightylib.util.tweenings.ETweeningBehaviour;

public class Cicular {

    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d){
        switch(behaviour){
            case In:
                return Cicular.In(t, b, c, d);
            case Out:
                return Cicular.Out(t, b, c, d);
            case InOut:
                return Cicular.InOut(t, b, c, d);
            default:
                return 0.f;
        }
    }

    public static float In(float t, float b, float c, float d){
        return -c * (float)(Math.sqrt(1 - (t /= d) * t) - 1) + b;
    }


    public static float Out(float t, float b, float c, float d){
        return  c * (float)Math.sqrt(1 - (t = t / d -1) * t) + b;
    }


    public static float InOut(float t, float b, float c, float d){
        if ((t /= d / 2) < 1){
            return -c / 2 * (float)(Math.sqrt(1 - t * t) - 1) + b;
        }

        return c / 2 * (float)(Math.sqrt(1 - (t -= 2) * t) + 1) + b;
    }
}
