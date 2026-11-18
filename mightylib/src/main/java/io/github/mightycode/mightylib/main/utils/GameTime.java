package io.github.mightycode.mightylib.main.utils;

import java.util.function.LongSupplier;

public final class GameTime {
    private static final LongSupplier SYSTEM_CLOCK = System::nanoTime;

    private static GameTime gametime;

    // Source of the time in nanoseconds: real time by default, replaced by a manual clock in tests
    private static LongSupplier clock = SYSTEM_CLOCK;

    private long currentTime;

    private float elapsedTime;

    private static final float NANO_TO_SEC = 1000000000.0f;

    // Duration of one tick (1 / TPS), to use for deterministic simulations like physics
    private static float fixedDeltaTime = 1f / 60f;

    public static float FixedDeltaTime(){
        return fixedDeltaTime;
    }

    public static void setFixedDeltaTime(float newFixedDeltaTime){
        fixedDeltaTime = newFixedDeltaTime;
    }

    public GameTime(){
        elapsedTime = 0;
        currentTime = clock.getAsLong();
    }

    private static GameTime instance(){
        if (gametime == null){
            gametime = new GameTime();
        }

        return gametime;
    }

    public static float DeltaTime(){
        return instance().elapsedTime;
    }


    public static float getDeltaTSinceLastUpdate(){
        return instance().deltaTimeSinceLastUpdate();
    }


    private float deltaTimeSinceLastUpdate(){
        return ((float)(clock.getAsLong() - currentTime)) / NANO_TO_SEC;
    }


    public static void update(){
        instance().updateGameTime();
    }

    public static float currentTime(){
        return instance().getCurrentTime();
    }

    public float getCurrentTime(){
        return currentTime / NANO_TO_SEC;
    }

    private void updateGameTime(){
        long now = clock.getAsLong();
        elapsedTime = ((float)(now - currentTime)) / NANO_TO_SEC;
        currentTime = now;
    }

    // Test access (package-private): the test kit reaches it through a bridge in this package

    // Replaces the source of the time (nanoseconds) and restarts the time from it; null = real time
    static void setClock(LongSupplier newClock){
        clock = newClock == null ? SYSTEM_CLOCK : newClock;
        gametime = null;
    }

    // Sets the duration of the last frame (DeltaTime) without waiting
    static void setDeltaTime(float seconds){
        instance().elapsedTime = seconds;
    }

    // Back to the real time, fixed delta time to its default, time restarted
    static void reset(){
        setClock(null);
        fixedDeltaTime = 1f / 60f;
    }
}
