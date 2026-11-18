package io.github.mightycode.mightylib.main.utils;

// Sets the duration of the last frame (GameTime.DeltaTime) so that timers can be tested without waiting
public final class GameTimeControl {
    private GameTimeControl() {
    }

    public static void setDeltaTime(float seconds) {
        GameTime.setDeltaTime(seconds);
    }

    public static void reset() {
        GameTime.reset();
    }
}
