package io.github.mightycode.mightylib.main.utils;

import org.apiguardian.api.API;

import java.util.function.LongSupplier;

import static org.apiguardian.api.API.Status.INTERNAL;

/** Bridge of the test kit to the package-private clock of GameTime. Not for the games. */
@API(status = INTERNAL)
public final class KitTimeAccess {
    private KitTimeAccess() {}

    // The source of the time, in nanoseconds; null: the real time
    public static void setClock(LongSupplier clock) {
        GameTime.setClock(clock);
    }

    public static void reset() {
        GameTime.reset();
    }
}
