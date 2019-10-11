package MightyLibrary.main;

import org.joml.Vector3f;

public class World {
    public static Vector3f lightColor;
    public static float lightStrength;
    public static Vector3f lightColorS;

    public static void init(){
        lightColor = new Vector3f();
        lightColorS = new Vector3f();
        lightStrength = 1f;
    }

    public static void calcLightColor(){
        lightColorS = lightColor.mul(lightStrength, lightColorS);
    }
}
