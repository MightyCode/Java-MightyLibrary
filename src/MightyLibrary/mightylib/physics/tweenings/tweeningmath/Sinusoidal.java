package MightyLibrary.mightylib.physics.tweenings.tweeningmath;


import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;

public class Sinusoidal {

    public static float Evaluate(ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d){
        switch(behaviour){
            case In:
                return Sinusoidal.In(t, b, c, d);
            case Out:
                return Sinusoidal.Out(t, b, c, d);
            case InOut:
                return Sinusoidal.InOut(t, b, c, d);
            default:
                return 0.f;
        }
    }

    public static float In(float t, float b, float c, float d){
        return -c * (float)Math.cos(t / d * (Math.PI / 2)) + c + b;
    }


    public static float Out(float t, float b, float c, float d){
        return c * (float)Math.sin(t / d * (Math.PI / 2)) + b;
    }


    public static float InOut(float t, float b, float c, float d){
        return -c / 2 * (float)(Math.cos(Math.PI * t / d) - 1) + b;
    }
}
