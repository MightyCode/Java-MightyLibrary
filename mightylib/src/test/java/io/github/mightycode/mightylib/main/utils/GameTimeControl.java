package io.github.mightycode.mightylib.main.utils;

import java.lang.reflect.Field;

// Sets the duration of the last frame (GameTime.DeltaTime) so that timers can be tested without waiting
public final class GameTimeControl {
    private GameTimeControl() {
    }

    public static void setDeltaTime(float seconds) {
        GameTime.DeltaTime(); // creates the instance
        try {
            Field instanceField = GameTime.class.getDeclaredField("gametime");
            instanceField.setAccessible(true);
            Field elapsedField = GameTime.class.getDeclaredField("elapsedTime");
            elapsedField.setAccessible(true);
            elapsedField.setFloat(instanceField.get(null), seconds);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("GameTime changed: update GameTimeControl", e);
        }
    }

    public static void reset() {
        setDeltaTime(0f);
    }
}
