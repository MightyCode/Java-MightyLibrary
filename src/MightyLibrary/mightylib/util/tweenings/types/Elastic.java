package MightyLibrary.mightylib.util.tweenings.types;

import MightyLibrary.mightylib.util.tweenings.ETweeningBehaviour;

public class Elastic {

    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d,
                                Float a, Float p){
        switch(behaviour){
            case In:
                return Elastic.InFull(t, b, c, d, a, p);
            case Out:
                return Elastic.OutFull(t, b, c, d, a, p);
            case InOut:
                return Elastic.InOutFull(t, b, c, d, a, p);
            default:
                return 0.f;
        }
    }


    public static float InFull(float t, float b, float c, float d,
                           Float a, Float p) {

        if (a == null || p == null)
            return Elastic.In(t, b, c, d);


        double s;
        if (t == 0)
            return b;

        if ((t /= d) == 1)
            return b + c;

        if (a < Math.abs(c)) {
            a = c;
            s = p / 4;
        } else {
            s = p / (2 * Math.PI) * Math.asin(c / a);
        }

        return -(float)
                (a
                 * Math.pow(2, 10 * (t -= 1))
                 * Math.sin((t * d - s)
                 * (2 * Math.PI) / p))
                 + b;
    }


    public static float In(float t, float b, float c, float d){
        if (t == 0)
            return b;

        if ((t /= d) == 1)
            return b + c;

        double p = d * 0.3;
        double a = c;
        double s = p / 4;

        return -(float)(
                a
                * Math.pow(2, 10 * (t -= 1))
                * Math.sin((t * d - s)
                * (2 * Math.PI) / p)) + b;
    }


    public static float OutFull(float t, float b, float c, float d,
                               Float a, Float p) {

        if (a == null || p == null)
            return Elastic.Out(t, b, c, d);


        double s;
        if (t == 0)
            return b;

        if ((t /= d) == 1)
            return b + c;

        if (a < Math.abs(c)) {
            a = c;
            s = p / 4;
        } else {
            s = p / (2 * Math.PI) * Math.asin(c / a);
        }

        return (float)(a
                * Math.pow(2, -10 * t)
                * Math.sin((t * d - s)
                * (2 * Math.PI) / p) + c + b);
    }


    public static float Out(float t, float b, float c, float d){
        if (t == 0)
            return b;

        if ((t /= d) == 1)
            return b + c;

        double p = d * 0.3;
        double a = c;
        double s = p / 4;

        return (float)(a
                * Math.pow(2, -10 * t)
                * Math.sin((t * d - s)
                * (2 * Math.PI) / p) + c + b);
    }


    public static float InOutFull(float t, float b, float c, float d,
                               Float a, Float p) {
        if (a == null || p == null)
            return Elastic.InOut(t, b, c, d);

        double s;
        if (t == 0)
            return b;

        if ((t /= d / 2) == 2)
            return b + c;

        if (a < Math.abs(c)) {
            a = c;
            s = p / 4;
        } else {
            s = p / (2 * Math.PI) * Math.asin(c / a);
        }

        if (t < 1)
            return (float) (-0.5 * (a
                    * Math.pow(2, 10 * (t -= 1))
                    * Math.sin((t * d - s)
                    * (2 * Math.PI) / p)) + b);

        return (float) (a
                * Math.pow(2, -10 * (t -= 1))
                * Math.sin((t * d - s)
                * (2 * Math.PI) / p)
                * 0.5 + c + b);

    }

    public static float InOut(float t, float b, float c, float d){
        if (t == 0)
            return b;

        if ((t /= d / 2) == 2)
            return b + c;

        double p = d * (0.3 * 1.5);
        double a = c;
        double s = p / 4;

        if (t < 1)
            return (float)(-0.5 * (a
                    * Math.pow(2, 10 * (t -= 1))
                    * Math.sin((t * d - s)
                    * (2 * Math.PI) / p)) + b);

        return (float)(a
                * Math.pow(2, -10 * (t -= 1))
                * Math.sin((t * d - s)
                * (2 * Math.PI) / p)
                * 0.5 + c + b);
    }
}
