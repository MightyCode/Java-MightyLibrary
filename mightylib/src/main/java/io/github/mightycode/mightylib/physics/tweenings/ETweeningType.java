package io.github.mightycode.mightylib.physics.tweenings;

import java.util.Locale;

public enum ETweeningType {
    Linear,
    Quadratic,
    Cubic,
    Quartic,
    Quintic,
    Sinusoidal,
    Exponential,
    Circular,
    Elastic,
    Back,
    Bounce;

    public ETweeningType fromString(String txt){
        String lowerCase = txt.trim().toLowerCase(Locale.ROOT);

        switch (lowerCase) {
            case "linear": return Linear;
            case "quadratic": return Quadratic;
            case "cubic": return Cubic;
            case "quartic": return Quartic;
            case "quintic": return Quintic;
            case "sinusoidal": return Sinusoidal;
            case "exponential": return Exponential;
            case "circular": return Circular;
            case "elastic": return Elastic;
            case "back": return Back;
            case "bounce": return Bounce;
            default: return null;
        }
    }
}
